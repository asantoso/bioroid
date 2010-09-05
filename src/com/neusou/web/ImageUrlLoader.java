package com.neusou.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;

import com.neusou.Logger;
import com.neusou.SoftHashMap;

import android.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;

public class ImageUrlLoader {
	
	SoftHashMap<String, Drawable> drawablesMap;
	String sdcachepath;
	Drawable DEFAULT_PROFILE_DRAWABLE;
	Object readImageLock = new Object();
	Context mContext;
	static final String LOGTAG = "ImageUrlLoader";
	static final String DEFAULT_IMAGEDIRECTORY = "cache";
	
	static final float cacheExpandFactor = 0.75f;
	static final int cacheInitialCapacity = 1;
	
	static final String LOG_TAG = "ImageUrlLoader";
	
	public ImageUrlLoader(Context ctx){		
		initObjects(ctx);
		getDefaultCachePath();
		createCacheDirectory();
	}
	
	public ImageUrlLoader(Context ctx, String imageFolder){
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
		sdcachepath = ess + "/" + mContext.getPackageName()+DEFAULT_IMAGEDIRECTORY;		
	}
	
	private void initObjects(Context ctx){
		mContext = ctx;
		drawablesMap = new SoftHashMap<String, Drawable>(cacheInitialCapacity,cacheExpandFactor);
		DEFAULT_PROFILE_DRAWABLE = null;
	}
	
	private boolean createCacheDirectory(){
		File path = new File(sdcachepath);
		return path.mkdirs();
	}
	
	public interface AsyncListener{
		public void onPreExecute();
		public void onPostExecute(AsyncLoaderResult result);
		public void onCancelled();		
	}
	
	public class AsyncLoaderResult{
		public Drawable drawable;
		public byte status = 0;
		public static final byte WAITING = 0;
		public static final byte SUCCESS = 1;
		public static final byte FAILURE = 2;
	}
	
	class AsyncImageLoader extends AsyncTask<String,Void,AsyncLoaderResult>{
		AsyncListener mListener;
		AsyncLoaderResult mResult = new AsyncLoaderResult();
		public static final String LOG_TAG = "AsyncImageLoader";
		public AsyncImageLoader(AsyncListener listener){
			mListener = listener;
		}
		
		public void setListener(AsyncListener listener){
			mListener = listener;
		}
		
		@Override
		protected AsyncLoaderResult doInBackground(String... uris) {
			if(uris == null || uris.length == 0){
				return null;
			}
			
			String uri = uris[0];
			
			Logger.l(Logger.DEBUG, LOG_TAG, "[doInBackground()] uri:"+ uri);
			//load image 
			Drawable d = loadImage(uri, false);
			mResult.drawable = d;
			
			if(d != null){
				Logger.l(Logger.DEBUG, "debug1", "AsyncLoad success");
				mResult.status = AsyncLoaderResult.SUCCESS;
				return mResult;
			}
			Logger.l(Logger.WARN, "debug1", "AsyncLoad failure");
			mResult.status = AsyncLoaderResult.FAILURE;			
			
			return mResult;			
		}
		
		@Override
		protected void onPreExecute() {		
			super.onPreExecute();
			if(mListener!=null){
				mListener.onPreExecute();
			}
		}
		
		@Override
		protected void onPostExecute(AsyncLoaderResult result) {		
			super.onPostExecute(result);
			if(mListener!=null){
				mListener.onPostExecute(result);
			}			
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			if(mListener!=null){
				mListener.onCancelled();
			}
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
	
	ArrayList<String> mFileWriteLocks = new ArrayList<String>(1);
	Object recordLock = new Object();
	
	private boolean isFileUnderProcess(String tag){
		if(!mFileWriteLocks.contains(tag)){
			return false;
		}
		return true;
	}
	private void acquireFileProcessing(String tag){
		mFileWriteLocks.add(tag);
	}
	private void releaseFileProcessing(String tag){
		mFileWriteLocks.remove(tag);
	}
	
	public boolean writeToDisk(File imageFile, String url){
		String digest;
		synchronized (recordLock){
			digest = computeDigest(url);
			String name = Thread.currentThread().getName();
			Logger.l(Logger.DEBUG,"debug", "[writeToDisk()] current thread name: "+name+" , url:"+url);
			boolean underProcessing = isFileUnderProcess(digest);
			if(underProcessing){
				Logger.l(Logger.DEBUG,"debug", "[writeToDisk()] current thread name: "+name+" has been kicked out!.");
				return false;
			}
			acquireFileProcessing(digest);
		}
						
		InputStream is;
		try{
			URL _url = new URL(url);			
			imageFile.createNewFile();
			is = (InputStream) _url.getContent();
			String canonPath = imageFile.getCanonicalPath();
			OutputStream os = new FileOutputStream(canonPath);
			BufferedOutputStream bos = new BufferedOutputStream(os, 256);
			BufferedInputStream bis = new BufferedInputStream(is, 256);
			byte[] buffer = new byte[256];

			// Start reading image data and writing to sdcard.
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
			Logger.l(Logger.VERBOSE,LOG_TAG,"Finished writing image data to disk. file size: "+totalBytesRead);
			// Finished reading image data and writing to sdcard.

			bos.close();
			bis.close();
			os.close();
			is.close();
		}catch(Exception e){
			Logger.l(Logger.ERROR,LOG_TAG, "[writeToDisk()] "+e.getMessage());
			imageFile.delete();
			releaseFileProcessing(digest);
			return false;
		}
		releaseFileProcessing(digest);
		return true;
	}
	
	public void loadImageAsync(final String uri, AsyncListener listener){
		//load image asynchronously..
		try{
			new AsyncImageLoader(listener).execute(uri);
		}catch(RejectedExecutionException e){
			Logger.l(Logger.WARN, LOG_TAG, "rejected executions. "+e.getMessage()); 
		}
	}

	public Drawable loadImage(final String url, boolean immediately) {
		Logger.l(Logger.DEBUG,LOGTAG, "loadImage("+ url+","+immediately+")");

		if (url == null || url.compareTo("null") == 0) {			
			Logger.l(Logger.DEBUG,LOGTAG,"[loadImage()] requested url is null");
			return null;
		}
		
		Drawable dcached;
		dcached = drawablesMap.get(url);
				
		if (dcached != null || immediately) {
						
			if(dcached == null){
				drawablesMap.remove(url);
			}else{
				Logger.l(Logger.DEBUG,LOGTAG,"[loadImage()] cached drawable is not null");	
			}
			return dcached;
		}

		File imageFile = null;
		File imageDir = new File(sdcachepath);
		
		String ess = Environment.getExternalStorageState();
		boolean canReadImageCacheDir = false;
		boolean isExternalMediaMounted = false;
		
		try {
			
			//IF SDCARD can be accessed but the cache has not been created, then create cache directory
			//otherwise retrieve image from the network and save TO MEMORY CACHE AND RETURN THE IMAGE
			if(ess.compareTo(Environment.MEDIA_MOUNTED) == 0){
				if(!imageDir.canRead()){
					// Check for SDCARD availability				
					Logger.l(Logger.DEBUG,LOGTAG,"Can not read SDCARD image directory: "+imageDir.toString());					
					boolean createSuccess = false;
					synchronized (imageDir) {
						createSuccess = imageDir.mkdirs();
					}			
					Logger.l(Logger.DEBUG,LOGTAG,"Success create SDCARD image directory?: "+createSuccess);
					canReadImageCacheDir = createSuccess && imageDir.canRead();
				}
				canReadImageCacheDir = imageDir.canRead();
				isExternalMediaMounted = true;
			}
			
			else{
				// try to load image from the web
				try {
					URL imageUrl = new URL(url);
					InputStream imageStream = (InputStream) imageUrl.getContent();					
					Drawable d = Drawable.createFromStream(imageStream, url);
					if (d != null) {
						drawablesMap.put(url, d);
						dcached = d;
						//return d;
					} else {
						dcached = DEFAULT_PROFILE_DRAWABLE;
						//return DEFAULT_PROFILE_DRAWABLE;
					}
				} catch (Exception e) {
					Logger.l(Logger.ERROR,LOG_TAG,"[loadImage()] error trying to retrieve image from the web: "+e.getMessage());
					dcached = DEFAULT_PROFILE_DRAWABLE;
				}
				
				return dcached;
			}

			//IF EXTERNAL SDCARD STORAGE IS MOUNTED
			
			//IF SDCARD CAN BE ACCESSED THEN SAVE IMAGE DATA ON SDCARD
			String bigInt = computeDigest(url);
			Logger.l(Logger.DEBUG,LOGTAG,"[loadImage()] Image URL:"+url+", MD5:" + bigInt);	
			imageFile = new File(sdcachepath, bigInt);
			if(!canReadImageCacheDir){
				dcached = DEFAULT_PROFILE_DRAWABLE;
			}else{
				boolean canReadImageFile = imageFile.canRead();
				
				//if image not on SDCard or in SDCard but the size is 0 then
				//try retrieving the image from the web and write to disk
				//Note: if filesize is 0 then the file will be written over.
				
				if(!canReadImageFile || imageFile.length() == 0){
					writeToDisk(imageFile, url);
				}
			}
	
			if (imageFile.canRead()) {
				synchronized (readImageLock) {
					FileInputStream fis = new FileInputStream(imageFile);
					int available = fis.available();				
					if (available > 0) {
						try{
							Drawable d = Drawable.createFromStream(fis, url);
							fis.close();											
							if (d == null) {
								drawablesMap.remove(url);
								return null;
							} else {						
								drawablesMap.put(url, d);
							return d;
							}
						}catch(java.lang.OutOfMemoryError e){
							e.printStackTrace();
						}
					}
				}

			}

			return DEFAULT_PROFILE_DRAWABLE;

		} catch (MalformedURLException e) {
			Logger.l(Logger.INFO,LOGTAG,"MalformedURLException " + e.getMessage());
			return null;
		} catch (IOException e) {
			Logger.l(Logger.INFO,LOGTAG,"IOException " + e.getMessage());

			try {
				if (imageFile != null) {
					imageFile.delete();
				}
			} catch (Exception e2) {
			}

			return null;
		} 
	}

}