package com.neusou.bioroid.image;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.R;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import android.widget.ImageView;

import com.neusou.DataState;
import com.neusou.Logger;
import com.neusou.SoftHashMap;
import com.neusou.bioroid.async.UserTask;
import com.neusou.bioroid.async.UserTaskExecutionScope;
import com.neusou.bioroid.db.DatabaseHelper;

public class ImageLoader {

	static final String LOG_TAG = Logger.registerLog(ImageLoader.class);

	static final String BITMAPCACHE_DEFAULT_CACHE_DIRECTORY = "cache";
	static final float BITMAPCACHE_EXPAND_FACTOR = 0.75f;
	static final int BITMAPCACHE_INITIAL_CAPACITY = 10;
	static ConcurrentHashMap<String, Byte> mFileAccessCount;

	static final byte WRITE_TO_DISK_WAIT = 1;
	static final byte WRITE_TO_DISK_ERROR = 2;
	static final byte WRITE_TO_DISK_SUCCESS = 3;
	static final byte WRITE_TO_DISK_RECALL = 4;

	public static final int DEFAULT_IMAGECACHEQUALITY = 100;
	
	Context mContext;
	SoftHashMap<String, Bitmap> mBitmapMap;
	String mCacheDirectoryPath;
	Bitmap DEFAULT_BITMAP;	
	int mImageCacheQuality = DEFAULT_IMAGECACHEQUALITY;
	CacheRecordDbHelper mCacheRecordDbHelper;
	private boolean mUseCacheDatabase = false;
	private static String mDBName;
	private static final int mDBVersion = 1;
	private SQLiteDatabase mDb;
	
	private void addCacheRecordInDatabase(URL imageUrl, long time, long sizeBytes ){
		mCacheRecordDbHelper.insertCacheRecord(mDb, imageUrl, time, sizeBytes);
	}
	
	private void deleteCacheRecords(long age){
		
	}
	
	public ImageLoader(Context ctx) {
		initObjects(ctx);
		mCacheDirectoryPath = createCacheDirectoryPath();
		createCacheDirectory(mCacheDirectoryPath);
	}

	public ImageLoader(Context ctx, String imageFolder) {
		initObjects(ctx);
		mCacheDirectoryPath = imageFolder;
		File cacheDirectory = new File(mCacheDirectoryPath);
		boolean isDirectory = cacheDirectory.isDirectory();
		if (!isDirectory) {
			try {
				createCacheDirectoryPath();
			} catch (Exception e) {
			}
		} else {
			if (!cacheDirectory.canRead()) {
				createCacheDirectory(mCacheDirectoryPath);
			}
		}
	}
	
	public void setImageCacheQuality(int quality){
		mImageCacheQuality = quality;
	}

	private String createCacheDirectoryPath() {
		String ess = Environment.getExternalStorageDirectory().toString();
		String out = ess + "/" + mContext.getPackageName() + "/"
				+ BITMAPCACHE_DEFAULT_CACHE_DIRECTORY;
		return out;
	}
		
	private void initObjects(Context ctx) {
		mContext = ctx;
		mBitmapMap = new SoftHashMap<String, Bitmap>(
				BITMAPCACHE_INITIAL_CAPACITY, BITMAPCACHE_EXPAND_FACTOR);
		mFileAccessCount = new ConcurrentHashMap<String, Byte>(1);
		DEFAULT_BITMAP = null;		
		
	}
	
	public void initCacheDatabase(Context ctx, String dbName, int sqlite){
		mCacheRecordDbHelper = new CacheRecordDbHelper(ctx, dbName, mDBVersion);
		try {
			mCacheRecordDbHelper.createDataBase(sqlite);
			mDb = mCacheRecordDbHelper.getWritableDatabase();
			mUseCacheDatabase = true;
		} catch (IOException e) {		
			e.printStackTrace();
			mUseCacheDatabase = false;
		}
	}

	public File getCacheDirectory() {
		return new File(mCacheDirectoryPath);
	}

	private boolean createCacheDirectory(String cacheDir) {
		File path = new File(cacheDir);
		return path.mkdirs();
	}

	public static interface AsyncListener {
		public void onPreExecute();

		public void onPostExecute(AsyncLoaderResult result);

		public void onCancelled();

		public void onPublishProgress(AsyncLoaderProgress progress);
	}

	public static class AsyncLoaderInput {
		public String imageUri = null;
		public ImageView imageView;
		public long code = 0;
		public long groupCode = 0;

		// TODO object pool AsyncLoaderInput
		public static AsyncLoaderInput getInstance() {
			return new AsyncLoaderInput();
		}
	}

	public static class AsyncLoaderProgress {
		public String message;
		public String imageUri;
		public boolean success;
		public Bitmap bitmap;
		public ImageView imageView;
		public long code;
		public long groupCode;
		public byte dataState = DataState.VALID;

		public void recycle() {
			imageUri = null;
			imageView = null;
			code = -1;
			groupCode = -1;
			dataState = DataState.INVALID;
		}
	}

	public static class AsyncLoaderResult {
		public byte status = 0;
		public static final byte WAITING = 0;
		public static final byte SUCCESS = 1;
		public static final byte FAILURE = 2;

		public void cleanUp() {
		}
	}

	class AsyncImageLoader extends
			UserTask<AsyncLoaderInput, AsyncLoaderProgress, AsyncLoaderResult> {

		WeakReference<AsyncListener> mListenerWeakRef;
		// AsyncListener mListener;
		AsyncLoaderResult mResult = new AsyncLoaderResult();
		private final String LOG_TAG = Logger
				.registerLog(AsyncImageLoader.class);

		public AsyncImageLoader() {
		}

		public AsyncImageLoader(UserTaskExecutionScope scope,
				AsyncListener listener) {
			super(scope);
			mListenerWeakRef = new WeakReference<AsyncListener>(listener);
		}

		public void setListener(AsyncListener listener) {
			mListenerWeakRef = new WeakReference<AsyncListener>(listener);
		}

		private void releaseAllLocks() {
			String tname = Thread.currentThread().getName();
			Logger.l(Logger.DEBUG, LOG_TAG, "[" + tname
					+ "] [releaseAllLocks()]");

			for (int i = 0; i < numImages; i++) {
				AsyncLoaderInput loadInput = mInputs[i];
				String uri = loadInput.imageUri;
				String bigInt = computeDigest(uri);
				releaseFileProcessing(bigInt);
			}

		}

		private void cleanUp() {

			mListenerWeakRef.clear();
			mResult.cleanUp();
			mResult = null;
			mInputs = null;
		}

		int numImages;
		AsyncLoaderInput mInputs[];

		@Override
		protected AsyncLoaderResult doInBackground(AsyncLoaderInput... inputs) {
			if (inputs == null || inputs.length == 0) {
				return null;
			}

			numImages = inputs.length;
			this.mInputs = inputs;

			

			// try{
			// TODO Attachment #images to load
			for (int i = 0; i < numImages; i++) {
				AsyncLoaderInput loadInput = inputs[i];
				assert loadInput != null;

				String uri = loadInput.imageUri;

				assert uri != null;

				// TODO Logger remove
				Logger.l(Logger.DEBUG, LOG_TAG, "[doInBackground()] loadImageAsync [" + (i + 1)
						+ "/" + numImages + "] uri:" + uri);
				Bitmap bitmap = loadImage(uri, false);
				AsyncLoaderProgress progress = new AsyncLoaderProgress();
				progress.imageView = loadInput.imageView;
				progress.code = loadInput.code;
				progress.groupCode = loadInput.groupCode;

				// TODO Logger remove
				// Logger.l(Logger.DEBUG, LOG_TAG,
				// "[Thread:"+threadName+"] [doInBackground()] loadImageAsync ["+(i+1)+"/"+numImages+"] publishProgress success:"+(bitmap!=null));
				if (bitmap != null) {
					progress.success = true;
					progress.bitmap = bitmap;
					progress.imageUri = uri;
					publishProgress(progress);
				} else {
					progress.success = false;
					progress.bitmap = null;
					progress.imageUri = uri;
					publishProgress(progress);
				}
			}

			return mResult;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			AsyncListener lst = mListenerWeakRef.get();
			if (lst != null) {
				lst.onPreExecute();
			}
		}

		@Override
		protected void onProgressUpdate(AsyncLoaderProgress... values) {
			super.onProgressUpdate(values);
			String threadName = Thread.currentThread().getName();
			AsyncListener lst = mListenerWeakRef.get();
			if (lst != null) {
				lst.onPublishProgress(values[0]);
			}
		}

		@Override
		protected void onPostExecute(AsyncLoaderResult result) {
			super.onPostExecute(result);
			// releaseAllLocks();
			AsyncListener lst = mListenerWeakRef.get();
			if (lst != null) {
				lst.onPostExecute(result);
			}
			cleanUp();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			// releaseAllLocks();
			AsyncListener lst = mListenerWeakRef.get();
			if (lst != null) {
				lst.onCancelled();
			}
			cleanUp();
		}
	}

	private String computeDigest(String uri) {
		String s = uri;
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			m.update(s.getBytes(), 0, s.length());
			BigInteger bi = new BigInteger(1, m.digest());
			String bigInt = bi.toString(16);
			return bigInt;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	// static List<String> mFileAccessLocks = Collections.synchronizedList(new
	// ArrayList<String>(1));

	private boolean tryAcquiringLock(String tag) {

		if (mFileAccessCount != null) {
			synchronized (mFileAccessCount) {
				Logger.l(Logger.DEBUG, LOG_TAG, "trying to access lock:" + tag
						+ "");
				assert (tag != null);
				assert (mFileAccessCount != null);
				boolean hasKey = mFileAccessCount.containsKey(tag);
				Logger.l(Logger.DEBUG, LOG_TAG, "has key? " + hasKey);
				byte accessCount = 0;
				if (hasKey) {
					accessCount = mFileAccessCount.get(tag);
				} else {
				}

				Logger.l(Logger.DEBUG, LOG_TAG, tag + " has been accessed "
						+ accessCount);
				if (!hasKey || accessCount == 0) {
					acquireFileProcessing(tag);
				} else {
					return false;
				}

			}
		}
		return true;
	}

	private void acquireFileProcessing(String tag) {
		String name = Thread.currentThread().getName();
		Logger.l(Logger.DEBUG, "debug", "[loadImage()] current thread name: "
				+ name + " has acquired the lock. tag:" + tag);

		Byte count = 1;
		Byte existing = mFileAccessCount.putIfAbsent(tag, count);
		if (existing != null) { // we have existing value in the map.
			mFileAccessCount.replace(tag, existing++);
		}
		/*
		 * if(mFileAccessCount.contains(tag)){ count =
		 * mFileAccessCount.get(tag); } count ++; mFileAccessCount.put(tag,
		 * count);
		 */
	}

	private void releaseFileProcessing(String tag) {
		// Logger.l(Logger.DEBUG,"debug",
		// " [loadImage()] trying to release file: "+tag+" null?"+(mFileAccessCount==null));

		synchronized (mFileAccessCount) {
			if (mFileAccessCount != null) {
				Byte count = 0;
				count = mFileAccessCount.putIfAbsent(tag, count);
				Logger.l(Logger.DEBUG, "debug",
						" [loadImage()] trying to release file: " + tag
								+ ", count: " + count);
				if (count != null) {
					count--;
				}
				count = count < 0 ? 0 : count;
				mFileAccessCount.replace(tag, count);
				mFileAccessCount.notify();
			}
		}

	}

	private boolean encode(Bitmap bmp, URL path, CompressFormat format,
			int quality) {
		try {
			FileOutputStream imageFos = new FileOutputStream(new File(path.toURI()));
			boolean success = bmp.compress(format, quality, imageFos);
			imageFos.close();
			return success;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return false;
	}

	private Bitmap decode(InputStream bis) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 1;
		options.inPurgeable = true;
		options.inInputShareable = true;
		Bitmap d = BitmapFactory.decodeStream(bis, null, options);
		return d;
	}

	public void loadImageAsync(UserTaskExecutionScope scope,
			final AsyncLoaderInput input, AsyncListener listener) {
		try {
			new AsyncImageLoader(scope, listener).execute(input);
		} catch (RejectedExecutionException e) {
		}
	}

	public void loadImageAsync(UserTaskExecutionScope scope,
			final AsyncLoaderInput[] inputs, AsyncListener listener) {
		try {
			new AsyncImageLoader(scope, listener).execute(inputs);
		} catch (RejectedExecutionException e) {
		}
	}
	
	public void prefetchImage(UserTaskExecutionScope scope,
			final AsyncLoaderInput[] inputs){
		
	}

	private Bitmap getCachedBitmap(URL url, File cachedImageFile)
			throws FileNotFoundException {
		FileInputStream cachedImageFis = new FileInputStream(cachedImageFile);
		Bitmap decodedBitmap = decode(cachedImageFis);
		if (decodedBitmap != null) {
			mBitmapMap.put(url.toString(), decodedBitmap);
		}
		return decodedBitmap;
	}

	/**
	 * Fetches image from the web if the caches does not contain the image and save it to disk cache (level 1 cache)or memory cache (level 2 cache).
	 */
	public void prefetchImage(){
		
	}
	
	public void purgeCachedImages(int maxTotalSize, int ageLimit){
		mCacheRecordDbHelper.getAllRecords(mDb);
	}
	
	public Bitmap loadImage(final String imageUri, boolean immediately) {
		if (imageUri == null || imageUri.equals("null")) {
			return null;
		}

		final String url;
		url = imageUri;

		if (url == null || url.compareTo("null") == 0) {
			return null;
		}

		Bitmap dcached;
		dcached = mBitmapMap.get(url);

		if (dcached != null || immediately) {
			if (dcached == null) {
				mBitmapMap.remove(url);
			}
			return dcached;
		}

		URL imageUrl = null;

		try {
			imageUrl = new URL(imageUri);
		} catch (MalformedURLException e) {
			return null;
		}
		
		File imageDir = new File(mCacheDirectoryPath);
		String bigInt = computeDigest(url);		
		File cachedImageFile = new File(imageDir, bigInt);
		String ess = Environment.getExternalStorageState();
		boolean canReadImageCacheDir = false;

		if (ess.equals(Environment.MEDIA_MOUNTED)) {
			if (!imageDir.canRead()) {
				boolean createSuccess = false;
				synchronized (imageDir) {
					createSuccess = imageDir.mkdirs();
				}
				canReadImageCacheDir = createSuccess && imageDir.canRead();
			}
			canReadImageCacheDir = imageDir.canRead();
			if (cachedImageFile.canRead()) {
				try {
					Bitmap cachedBitmap = getCachedBitmap(imageUrl,
							cachedImageFile);
					if (cachedBitmap != null) {
						mBitmapMap.put(imageUri, cachedBitmap);
						return cachedBitmap;
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		HttpGet httpRequest = null;
		try {
			httpRequest = new HttpGet(imageUrl.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}

		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = null;
		try {
			response = (HttpResponse) httpclient.execute(httpRequest);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		HttpEntity entity = response.getEntity();
		BufferedHttpEntity bufHttpEntity = null;
		try {
			bufHttpEntity = new BufferedHttpEntity(entity);
			final int bufferSize = 1024 * 50;
			BufferedInputStream imageBis = new BufferedInputStream(
					bufHttpEntity.getContent(), bufferSize);
			long contentLength = bufHttpEntity.getContentLength();
			Bitmap decodedBitmap = decode(imageBis);
			try {
				imageBis.close();
			} catch (IOException e1) {
			}
			encode(decodedBitmap, cachedImageFile.toURL(), CompressFormat.PNG,	mImageCacheQuality);
			Calendar cal = Calendar.getInstance();
			long currentTime = cal.getTime().getTime();
			if(mUseCacheDatabase){
				mCacheRecordDbHelper.insertCacheRecord(mDb, imageUrl, currentTime, contentLength);
			}
			if (decodedBitmap != null) {
				mBitmapMap.put(url, decodedBitmap);
			}
			return decodedBitmap;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	class CacheTable {
		public static final String TABLE = "bioroid_imageloader_cache";
		public static final int COLINDEX_ID = 0; 
		public static final int COLINDEX_LAST_ACCESSED_DATE = 1;
		public static final int COLINDEX_FILESIZE = 2;
		public static final int COLINDEX_IMAGEPATH = 3;
		
		public static final String COL_ID = "_id"; 
		public static final String COL_LAST_ACCESSED_DATE = "last_accessed_date";
		public static final String COL_FILESIZE = "filsize";
		public static final String COL_IMAGEPATH = "imagepath";
	}
	
	class CacheRecordDbHelper extends DatabaseHelper{		
		public CacheRecordDbHelper(Context context, String dbName, int version) {
			super(context, dbName, version);
		}
		public void insertCacheRecord(SQLiteDatabase db, URL imageUrl, long time, long sizeBytes){
			ContentValues values = new ContentValues();
			values.put(CacheTable.COL_LAST_ACCESSED_DATE, time );
			values.put(CacheTable.COL_IMAGEPATH, imageUrl.toString());
			values.put(CacheTable.COL_FILESIZE, sizeBytes);
			db.insert(CacheTable.TABLE, null, values);
		}		
		public Cursor getAllRecords(SQLiteDatabase db){
			return db.query(CacheTable.TABLE, null, null, null, null, null, null);			
		}
		
	};
	

}