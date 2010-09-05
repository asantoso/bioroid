package com.neusou.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Environment;
import android.widget.ImageView;

import com.neusou.DataState;
import com.neusou.Logger;
import com.neusou.SoftHashMap;
import com.neusou.async.UserTask;
import com.neusou.async.UserTaskExecutionScope;

public class ImageUrlLoader2 {
	
	static final String LOG_TAG = Logger.registerLog(ImageUrlLoader2.class);
	
	static final String BITMAPCACHE_DEFAULT_CACHE_DIRECTORY = "cache";	
	static final float BITMAPCACHE_EXPAND_FACTOR = 0.75f;
	static final int BITMAPCACHE_INITIAL_CAPACITY = 10;
	static ConcurrentHashMap<String,Byte> mFileAccessCount;
	
	static final byte WRITE_TO_DISK_WAIT = 1;
	static final byte WRITE_TO_DISK_ERROR = 2;
	static final byte WRITE_TO_DISK_SUCCESS = 3;
	static final byte WRITE_TO_DISK_RECALL = 4;
	
	SoftHashMap<String, Bitmap> drawablesMap;
	String sdcachepath;
	Bitmap DEFAULT_BITMAP;
	Context mContext;
	
	public ImageUrlLoader2(Context ctx){		
		initObjects(ctx);
		getDefaultCachePath();
		createCacheDirectory();
	}
	
	public ImageUrlLoader2(Context ctx, String imageFolder){
		initObjects(ctx);
		sdcachepath = imageFolder; 
		File cacheDirectory = new File(sdcachepath);
		boolean isDirectory = cacheDirectory.isDirectory();
		if(!isDirectory){
			try{
				getDefaultCachePath();
			}catch(Exception e){
			}
		}else{
			if(!cacheDirectory.canRead()){
				createCacheDirectory();
			}			
		}
	}
	
	private void getDefaultCachePath(){
		String ess = Environment.getExternalStorageDirectory().toString();
		sdcachepath = ess + "/" + mContext.getPackageName()+"/"+BITMAPCACHE_DEFAULT_CACHE_DIRECTORY;		
	}
			
	private void initObjects(Context ctx){
		mContext = ctx;		
		drawablesMap = new SoftHashMap<String, Bitmap>(BITMAPCACHE_INITIAL_CAPACITY,BITMAPCACHE_EXPAND_FACTOR);
		mFileAccessCount = new ConcurrentHashMap<String,Byte>(1);
		DEFAULT_BITMAP = null;
	}
	
	private boolean createCacheDirectory(){
		File path = new File(sdcachepath);
		return path.mkdirs();
	}
	
	public static interface AsyncListener{
		public void onPreExecute();
		public void onPostExecute(AsyncLoaderResult result);
		public void onCancelled();		
		public void onPublishProgress(AsyncLoaderProgress progress);
	}
	
	public static class AsyncLoaderInput{
		public String imageUri = null;
		public ImageView imageView;
		public long code = 0;
		public long groupCode = 0;
		
		//TODO object pool AsyncLoaderInput
		public static AsyncLoaderInput getInstance(){
			return new AsyncLoaderInput();
		}
	}
	
	public static class AsyncLoaderProgress{		
		public String message;
		public String imageUri;
		public boolean success;
		public Bitmap bitmap;
		public ImageView imageView;
		public long code;
		public long groupCode;
		public byte dataState = DataState.VALID;
		
		public void recycle(){
			imageUri = null;
			imageView = null;
			code = -1;
			groupCode = -1;
			dataState = DataState.INVALID;
		}
	}
	
	public static class AsyncLoaderResult{
//		public Bitmap drawable;
		public byte status = 0;
		public static final byte WAITING = 0;
		public static final byte SUCCESS = 1;
		public static final byte FAILURE = 2;
		public void cleanUp(){
	//		drawable = null;
		}
	}
	
	class AsyncImageLoader extends UserTask<AsyncLoaderInput, AsyncLoaderProgress, AsyncLoaderResult>{
		
		WeakReference<AsyncListener> mListenerWeakRef;
		//AsyncListener mListener;
		AsyncLoaderResult mResult = new AsyncLoaderResult();
		public static final String LOG_TAG = "AsyncImageLoader2";
				
		public AsyncImageLoader() {		
		}
		
		public AsyncImageLoader(UserTaskExecutionScope scope, AsyncListener listener){
			super(scope);
			mListenerWeakRef = new WeakReference<AsyncListener>(listener);
			//mListener = listener;
		}
		
		public void setListener(AsyncListener listener){
			mListenerWeakRef = new WeakReference<AsyncListener>(listener);
			//mListener = listener;
		}
		
		
		private void releaseAllLocks(){
			String tname = Thread.currentThread().getName();
			Logger.l(Logger.DEBUG, LOG_TAG, "["+tname+"] [releaseAllLocks()]");
						
			for(int i=0;i<numImages;i++){
				AsyncLoaderInput loadInput = mInputs[i];
				String uri = loadInput.imageUri;
				String bigInt = computeDigest(uri);
				releaseFileProcessing(bigInt);
			}
			
		}
		
		
		private void cleanUp(){
						
			mListenerWeakRef.clear();
			mResult.cleanUp();
			mResult = null;
			mInputs = null;
		}
		
		int numImages;
		AsyncLoaderInput mInputs[];
		
		@Override
		protected AsyncLoaderResult doInBackground(AsyncLoaderInput... inputs) {
			if(inputs == null || inputs.length == 0){
				return null;
			}
			
			numImages = inputs.length;
			this.mInputs = inputs;
			
			String threadName = Thread.currentThread().getName();
			//TODO Logger remove
			//Logger.l(Logger.DEBUG, LOG_TAG, "[Thread:"+threadName+"] [doInBackground()] loadImageAsync numImages:"+numImages);
			
			//try{
			//TODO Attachment #images to load
				for(int i=0;i<numImages;i++){
					AsyncLoaderInput loadInput = inputs[i];
					assert loadInput != null;
					
					String uri = loadInput.imageUri;
					
					assert uri != null;
					
					//TODO Logger remove
					Logger.l(Logger.DEBUG, LOG_TAG,  "[Thread:"+threadName+"] [doInBackground()] loadImageAsync ["+(i+1)+"/"+numImages+"] uri:"+ uri);
					Bitmap bitmap = loadImage(uri, false);								
					AsyncLoaderProgress progress = new AsyncLoaderProgress();
					progress.imageView = loadInput.imageView;
					progress.code = loadInput.code;
					progress.groupCode = loadInput.groupCode;
				
					//TODO Logger remove
					Logger.l(Logger.DEBUG, LOG_TAG, "[Thread:"+threadName+"] [doInBackground()] loadImageAsync ["+(i+1)+"/"+numImages+"] publishProgress success:"+(bitmap!=null));
					if(bitmap != null){
						progress.success = true;
						progress.bitmap = bitmap;				
						progress.imageUri = uri;
						publishProgress(progress);
					}
					else{
						progress.success = false;
						progress.bitmap = null;
						progress.imageUri = uri;
						publishProgress(progress);
					}
				}
				mResult.status = AsyncLoaderResult.SUCCESS;
			//}
			//catch(NullPointerException e){
			//	mResult.status = AsyncLoaderResult.FAILURE;		
			//	Logger.l(Logger.ERROR, LOG_TAG,	"[doInBackground()] ]error: "+e.getMessage()+". cause:"+e.getCause());
			//}
						
			return mResult;			
		}
		
		@Override
		protected void onPreExecute() {		
			super.onPreExecute();
			AsyncListener lst = mListenerWeakRef.get();
			if(lst!=null){
				lst.onPreExecute();
			}
		}
		
		@Override
		protected void onProgressUpdate(AsyncLoaderProgress... values) {		
			super.onProgressUpdate(values);
			String threadName = Thread.currentThread().getName();
			AsyncListener lst = mListenerWeakRef.get();
			if(lst != null){				
				lst.onPublishProgress(values[0]);
			}		
		}
		
		@Override
		protected void onPostExecute(AsyncLoaderResult result) {		
			super.onPostExecute(result);
			//releaseAllLocks();
			AsyncListener lst = mListenerWeakRef.get();
			if(lst!=null){
				lst.onPostExecute(result);
			}			
			cleanUp();
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			//releaseAllLocks();
			AsyncListener lst = mListenerWeakRef.get();
			if(lst!=null){
				lst.onCancelled();
			}			
			cleanUp();
		}
	}
	
	private String computeDigest(String uri){
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
	
	//static List<String> mFileAccessLocks = Collections.synchronizedList(new ArrayList<String>(1));
	
	
	private boolean tryAcquiringLock(String tag){
		String tname =  Thread.currentThread().getName();
		
		if(mFileAccessCount != null){
		synchronized(mFileAccessCount){
			Logger.l(Logger.DEBUG, LOG_TAG, "["+tname+"] trying to access lock:"+tag+"");
			assert(tag != null);
			assert(mFileAccessCount != null);
			boolean hasKey = mFileAccessCount.containsKey(tag);
			Logger.l(Logger.DEBUG, LOG_TAG, "["+tname+"] has key? "+hasKey);
			byte accessCount = 0;
			if(hasKey){
				accessCount = mFileAccessCount.get(tag);
			}else{				
			}
			
			Logger.l(Logger.DEBUG, LOG_TAG, "["+tname+"] "+tag+" has been accessed "+accessCount);	
			if(accessCount == 0 || hasKey){
				acquireFileProcessing(tag);
			}else{
				return false;
			}
			
			/*
			ListIterator<String> it = mFileAccessLocks.listIterator();
			for(int i=0;it.hasNext();i++){				
				Logger.l(Logger.DEBUG, LOG_TAG, "["+tname+"] locks iterater: #"+i+": "+it.next());
			}
			int foundIndex = Collections.binarySearch(mFileAccessLocks,tag);
			if(0 <= foundIndex){
				Logger.l(Logger.DEBUG, LOG_TAG, "["+tname+"] searching for:"+tag+". found index: #" + foundIndex);
				return false;
			}else{
				acquireFileProcessing(tag);
			}
			*/
		}
		}
		return true;
	}
	
	private void acquireFileProcessing(String tag){		
		String name = Thread.currentThread().getName();
		Logger.l(Logger.DEBUG,"debug", "[loadImage()] current thread name: "+name+" has acquired the lock. tag:"+tag);
		//mFileAccessLocks.add(tag);
		byte count = 0;
		if(mFileAccessCount.contains(tag)){
			count = mFileAccessCount.get(tag);
		}
		count ++;
		mFileAccessCount.put(tag, count);
		
	}
	
	private void releaseFileProcessing(String tag){
		
		String name = Thread.currentThread().getName();
		Logger.l(Logger.DEBUG,"debug", name+" [loadImage()] trying to release file: "+tag+" null?"+(mFileAccessCount==null));
		synchronized(mFileAccessCount){
			if(mFileAccessCount != null){
				byte count = 0;			
				if(mFileAccessCount.contains(tag)){//needs to check first otherwise will throw an NullPointerException
				 count = mFileAccessCount.get(tag);
				}
				Logger.l(Logger.DEBUG,"debug", name+" [loadImage()] trying to release file: "+tag+", count: "+count);
				count --;
				count = count < 0 ? 0 : count;
				mFileAccessCount.put(tag, count);			
				mFileAccessCount.notify();
			}
		}
	}
	

	
	public byte writeToDisk(File imageFile, String savedFilename, String url){
		
		InputStream is = null;
		try{
			
			URL _url = new URL(url);	
			try{
				imageFile.createNewFile();
			}catch(IOException e){
				Logger.l(Logger.VERBOSE,LOG_TAG,"could not create new file for url: "+url+"  message:"+e.getMessage());
			}
			
			try{
				is = (InputStream) _url.getContent();
			}catch(IOException e){
				Logger.l(Logger.VERBOSE,LOG_TAG,"could not get content for url: "+url+"  message:"+e.getMessage());
			}
			
			String canonPath = "";
			try{
				canonPath = imageFile.getCanonicalPath();
			}catch(FileNotFoundException e){
				Logger.l(Logger.VERBOSE,LOG_TAG,"could not find file specified by "+imageFile.getAbsolutePath()+"  message:"+e.getMessage());
			}
			OutputStream os = new FileOutputStream(canonPath);
			int bufferSize = 1024*8;
			BufferedOutputStream bos = new BufferedOutputStream(os, bufferSize);
			BufferedInputStream bis = new BufferedInputStream(is, bufferSize);
			byte[] buffer = new byte[bufferSize];

			// Start reading image data and writing to sdcard.
			//TODO Logger remove
			Logger.l(Logger.VERBOSE,LOG_TAG,"Start writing image data to disk. file path: "+imageFile.toString());
			int totalBytesRead = 0;
			while (true) {
				int numBytesRead = bis.read(buffer);				
				if (numBytesRead == -1) {
					break;
				}
				totalBytesRead += numBytesRead;
				bos.write(buffer, 0, numBytesRead);
			}
			//TODO Logger remove
			Logger.l(Logger.VERBOSE,LOG_TAG,"Finished writing image data to disk. file size: "+totalBytesRead);
			// Finished reading image data and writing to sdcard.

			bos.close();
			bis.close();
			os.close();
			is.close();
		}
		catch(MalformedURLException e){
			Logger.l(Logger.ERROR,LOG_TAG, "[writeToDisk()] malformedURLException: "+e.getMessage());
		}
		catch(IOException e){
			Logger.l(Logger.ERROR,LOG_TAG, "[writeToDisk()] error: "+e.getCause());			
			Logger.l(Logger.ERROR,LOG_TAG, "[writeToDisk()] error: "+e.getMessage());
			if(imageFile.isFile() && imageFile.exists()){								
				//TODO Logger remove
				//Logger.l(Logger.ERROR,LOG_TAG, "[writeToDisk()] deleting file..");
				boolean successDelete = imageFile.delete();
				//TODO Logger remove
				//Logger.l(Logger.ERROR,LOG_TAG, "[writeToDisk()] deletion successful? "+successDelete);
			}
			return WRITE_TO_DISK_ERROR;
		}catch(RuntimeException e){
			return WRITE_TO_DISK_ERROR;
		}
		
		return WRITE_TO_DISK_SUCCESS;		
	}
	
	public Bitmap decode(InputStream bis){
		BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inPurgeable=true;		             
        options.inInputShareable=true;
        Bitmap d = BitmapFactory.decodeStream(bis, null, options);
        return d;
	}
	public void fetch(UserTaskExecutionScope scope, long code, long groupCode, String imageUri, ImageView imageView, Bitmap defaultBitmap,
			AsyncListener imageAsyncLoaderListener){
		Bitmap iconBmp = loadImage(imageUri, true);
		//Logger.l(Logger.DEBUG, "debug", "listener null?"+(imageAsyncLoaderListener==null));
		if (iconBmp != null) {
			imageView.setImageBitmap(iconBmp);
			iconBmp=null;
		}else{
			// we don't have the bitmap in the cache, so fetch it asynchronously.
			AsyncLoaderInput input = new AsyncLoaderInput();
			input.imageView = imageView;
			input.imageUri = imageUri;
			input.code = code;
			input.groupCode = groupCode;
			imageView.setImageBitmap(defaultBitmap);
			//Logger.l(Logger.DEBUG, "debug", "listener null?"+(imageAsyncLoaderListener==null));
			loadImageAsync(scope, input, imageAsyncLoaderListener);
		}		
	}
	
	public void loadImageAsync(UserTaskExecutionScope scope, final AsyncLoaderInput input, AsyncListener listener ){
		//load image asynchronously..
		
		try{
			new AsyncImageLoader(scope, listener).execute(input);
		}catch(RejectedExecutionException e){
			//TODO Logger remove
			//Logger.l(Logger.WARN, LOG_TAG, "[loadImageAsync()] rejected executions. "+e.getMessage()); 
		}
	}

	public void loadImageAsync(UserTaskExecutionScope scope, final AsyncLoaderInput[] inputs, AsyncListener listener) {
		//load image asynchronously..
		try{
			new AsyncImageLoader(scope, listener).execute(inputs);
		}catch(RejectedExecutionException e){
			//TODO Logger remove
			//Logger.l(Logger.WARN, LOG_TAG, "[loadImageAsync()] rejected executions. "+e.getMessage()); 
		}	
	}
	
	public Bitmap loadImage(final String imageUri, boolean immediately) {
		if(imageUri == null || imageUri.equals("null")){
			return null;
		}
		//NOTE : DO NOT URLDECODE IMAGEURI, otherwise will not fetch facebook attachment images url
		//TODO Logger remove
		Logger.l(Logger.DEBUG,LOG_TAG,"loadImage("+imageUri+","+immediately+")");
			
		final String url;		
		url = imageUri;
			
		if (url == null || url.compareTo("null") == 0) {
			//TODO Logger remove
			Logger.l(Logger.DEBUG,LOG_TAG, "[loadImage()] requested url is null");
			return null;
		}

			
		Bitmap dcached;
		dcached = drawablesMap.get(url);
				
		if (dcached != null || immediately) {
			if(dcached == null){
				drawablesMap.remove(url);
			}else{
				//TODO Logger remove
				Logger.l(Logger.DEBUG,LOG_TAG, "[loadImage()] cached drawable is not null");	
			}
			return dcached;
		}

		File imageFile = null;
		File imageDir = new File(sdcachepath);
		String bigInt = computeDigest(url);
		String savedFilename = bigInt;			
		String ess = Environment.getExternalStorageState();
		
		boolean canReadImageCacheDir = false;
				
		try {
			//IF SDCARD can be accessed but the cache has not been created, then create cache directory
			//otherwise retrieve image from the network and save TO MEMORY CACHE AND RETURN THE IMAGE
			if(ess.equals(Environment.MEDIA_MOUNTED)){
				if(!imageDir.canRead()){
					// Check for SDCARD availability			
					//TODO Logger remove
					Logger.l(Logger.DEBUG,LOG_TAG,"Can not read SDCARD image directory: "+imageDir.toString());					
					boolean createSuccess = false;
					synchronized (imageDir) {
						createSuccess = imageDir.mkdirs();
					}	
					
					//TODO Logger remove
					Logger.l(Logger.DEBUG,LOG_TAG,"Success create SDCARD image directory?: "+createSuccess);
					canReadImageCacheDir = createSuccess && imageDir.canRead();
				}
				canReadImageCacheDir = imageDir.canRead();
			}
			
			else{
				// try to load image from the web
				try {					
					URL imageUrl = new URL(url);
					   HttpGet httpRequest = null;
		               try {
		            	   httpRequest = new HttpGet(imageUrl.toURI());
		               } catch (URISyntaxException e) {
		                   e.printStackTrace();
		               }

		               HttpClient httpclient = new DefaultHttpClient();
		               HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);

		               HttpEntity entity = response.getEntity();
		               BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
		               InputStream imageStream = bufHttpEntity.getContent();
		               
		               //InputStream imageStream = (InputStream) imageUrl.getContent();	
		               BufferedInputStream bis = new BufferedInputStream(imageStream,1024*10);
		               Options opt = new Options();
		               opt.inSampleSize=2;
		               opt.inInputShareable=true;
		               opt.inPurgeable=true;
		              
		               Bitmap d = decode(bis);
		               		               
		               imageStream.close();
		               
		               if (d != null) {
		            	   drawablesMap.put(url, d);
		            	   dcached = d;
	
		               } else {
		            	   dcached = DEFAULT_BITMAP;

		               }
				} catch (Exception e) {
					Logger.l(Logger.ERROR,LOG_TAG, "[loadImage()] error trying to retrieve image from the web: "+e.getMessage());
					dcached = DEFAULT_BITMAP;
				}
				
				
				
				return dcached;
			}

			//IF EXTERNAL SDCARD STORAGE IS MOUNTED
			
			//IF SDCARD CAN BE ACCESSED THEN SAVE IMAGE DATA ON SDCARD
			
			//TODO Logger remove
			///Logger.l(Logger.DEBUG,LOG_TAG, "[Thread:"+threadName+"] [loadImage()] Image URL:"+url+", MD5:" + bigInt);	
			imageFile = new File(sdcachepath, savedFilename);
			
			byte writeToDiskStatus = 0;
			
			
			if(canReadImageCacheDir){			
							
				while(true){
						
					String name = Thread.currentThread().getName();
					//TODO Logger remove
					//Logger.l(Logger.DEBUG,"debug", "[loadImage()] current thread name: "+name+" , url:"+url);
					
					synchronized (mFileAccessCount) {
							
						if(tryAcquiringLock(savedFilename)){
							break;
						}
						else{
							//TODO Logger remove
							//Logger.l(Logger.DEBUG,"debug", "[loadImage()] current thread name: "+name+" has been put to waiting!.");
						
							
							try{
								final long waitMillis = 50l;
								Logger.l(Logger.DEBUG,"debug", "[loadImage()] current thread name: "+name+" has been put to waiting!. tag:"+savedFilename+", file:"+url);
								mFileAccessCount.wait(waitMillis);
							}catch(InterruptedException e){				
								//Logger.l(Logger.VERBOSE,"debug", "[loadImage()] current thread name: "+name+" was interrupted");
							}	
							
							return null;
							
						}
					}
					
				}
				
				boolean imageFileIsFile = imageFile.isFile();
				boolean canReadImageFile = imageFile.canRead();
				
				long fileSize = imageFile.length();
				Logger.l(Logger.DEBUG, LOG_TAG,"[loadImage()] uri:"+url+", file length:"+fileSize+", isFile:"+imageFileIsFile);
				//if image not on SDCard or in SDCard but the size is 0 then
				//try retrieving the image from the web and write to disk
				//Note: if filesize is 0 then the file will be written over.
				String digest = computeDigest(url);
				if(!canReadImageFile || (imageFileIsFile && imageFile.length() == 0)){
					//synchronized(url){
						if(imageFile.exists() && imageFile.isFile()){
							imageFile.delete();
						}
						writeToDiskStatus = writeToDisk(imageFile, digest, url);						
						if(writeToDiskStatus == WRITE_TO_DISK_WAIT){						
						}else if(writeToDiskStatus == WRITE_TO_DISK_SUCCESS){						
						}else if(writeToDiskStatus == WRITE_TO_DISK_ERROR) {							
						}					
				}
				
				if (imageFile.canRead()) {
					//TODO Logger remove
					//Logger.l(Logger.VERBOSE, LOG_TAG, "[Thread:"+threadName+"] [loadImage()] able to read written image file..");
					if(imageFile.length() == 0){
						try{
						//TODO Logger remove
						//Logger.l(Logger.WARN, LOG_TAG, "[Thread:"+threadName+"] [loadImage()] saved file length is 0 bytes. deleting image file: "+bigInt);
							if(imageFile.exists() && imageFile.isFile()){
								imageFile.delete();
							}
						}catch(SecurityException e){
							e.printStackTrace();
						}
					}				
				//synchronized (readImageLock) {
					FileInputStream fis = new FileInputStream(imageFile);
					BufferedInputStream bis = new BufferedInputStream(fis, 1024*10);
					int available = fis.available();
					if (available > 0) {
						try{			
							Bitmap d = decode(bis);
				            
							fis.close();							
							bis.close();
							if (d == null) {
								//TODO Logger remove
								//Logger.l(Logger.ERROR, LOG_TAG,"[Thread:"+threadName+"] [loadImage()] could not interpret saved image file. ");
								drawablesMap.remove(url);
								if(imageFile.exists() && imageFile.isFile()){
									imageFile.delete();
								}
								dcached = null;								
								
							} else {								
								drawablesMap.put(url, d);
								dcached = d;								
							}							
						}catch(java.lang.OutOfMemoryError e){
							e.printStackTrace();
							dcached = null;
						}
					}					
					bis.close();
					fis.close();
				//}
			}
			}
			else{
				//TODO Logger remove
				//Logger.l(Logger.WARN, LOG_TAG, "[Thread:"+threadName+"] [loadImage()] unable to read saved image file. now trying to deleting file..");
				boolean deleteSuccess = imageFile.delete();
				//TODO Logger remove
				//Logger.l(Logger.VERBOSE, LOG_TAG,"[Thread:"+threadName+"] [loadImage()] "+(deleteSuccess?"file was sucessfully deleted.":"file couldn't be deleted."));
				dcached = DEFAULT_BITMAP;
			}
			
		} catch (MalformedURLException e) {
			//TODO Logger remove
			Logger.l(Logger.INFO,LOG_TAG, "MalformedURLException " + e.getMessage());
			dcached = null;
		} catch (IOException e) {
			//TODO Logger remove
			Logger.l(Logger.INFO,LOG_TAG, " IOException " + e.getMessage());

			try {
				if (imageFile != null && imageFile.exists() && imageFile.isFile()) {
					imageFile.delete();
				}
			} catch (Exception e2) {
			}
			dcached = null;
		} finally{
					
		}
		
		releaseFileProcessing(savedFilename);
		
		return dcached;
	}
	

}