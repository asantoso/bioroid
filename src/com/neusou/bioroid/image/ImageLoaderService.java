package com.neusou.bioroid.image;

import java.util.concurrent.TimeUnit;

import android.R;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.neusou.Logger;
import com.neusou.bioroid.async.UserTaskExecutionScope;
import com.neusou.bioroid.image.ImageLoader.AsyncListener;
import com.neusou.bioroid.image.ImageLoader.AsyncLoaderInput;

public class ImageLoaderService extends Service {

	private static final String LOG_TAG = Logger.registerLog(ImageLoaderService.class);

	protected static ImageLoader mImageLoader; 
	protected static UserTaskExecutionScope mImageLoadingScope;
	
	protected Context mContext;
	protected String name;
	protected int mPoolSize;
	protected int mMaxPoolSize;
	protected int mPendingSize;
	protected long mTimeOutMilli;
	protected boolean mAutoPurgeCache;
	protected int mPurgeExpiryDays;
	protected int mPurgeTotalSize;
	protected String mCacheDbName;
	
	public static final String METADATA_POOLSIZE = "poolSize";
	public static final String METADATA_MAXPOOLSIZE = "maxPoolSize";
	public static final String METADATA_TIMEOUT = "timeout";
	public static final String METADATA_NAME = "name";
	public static final String METADATA_PENDINGSIZE = "pendingSize";
	public static final String METADATA_AUTOPURGECACHE = "autoPurgeCache";
	public static final String METADATA_PURGE_EXPIRYDAYS = "purgeExpiryDays";
	public static final String METADATA_PURGE_TOTALSIZE = "purgeTotalSize";
	public static final String METADATA_CACHE_DB_NAME = "cacheDbName";
	
	public static final long MINIMUM_TIMEOUT_MILLI = 1000l;
	
	@Override
	public void onDestroy() {	
		super.onDestroy();
		Logger.l(Logger.DEBUG, LOG_TAG, "onDestroy");
	}
		
	@Override
	public void onCreate() {		
		super.onCreate();
		Logger.l(Logger.DEBUG, LOG_TAG, "onCreate");		
		Bundle metadata;		
		try {
			metadata = getPackageManager().getServiceInfo(
					new ComponentName(mContext, mContext.getClass()),					
					PackageManager.GET_META_DATA).metaData;
			name = metadata.getString(METADATA_NAME);
			name = name == null?LOG_TAG:name;
			mPoolSize = metadata.getInt(METADATA_POOLSIZE);
			mPoolSize = mPoolSize == 0?1:mPoolSize;
			mMaxPoolSize = metadata.getInt(METADATA_MAXPOOLSIZE);
			mMaxPoolSize = mMaxPoolSize == 0?1:mMaxPoolSize;
			mTimeOutMilli = (long) metadata.getInt(METADATA_TIMEOUT);
			mTimeOutMilli = mTimeOutMilli<MINIMUM_TIMEOUT_MILLI?MINIMUM_TIMEOUT_MILLI:mTimeOutMilli;
			mPendingSize = metadata.getInt(METADATA_PENDINGSIZE);
			mAutoPurgeCache  = metadata.getBoolean(METADATA_AUTOPURGECACHE);
			mPurgeExpiryDays  = metadata.getInt(METADATA_PURGE_EXPIRYDAYS);
			mPurgeTotalSize  = metadata.getInt(METADATA_PURGE_TOTALSIZE);
			mCacheDbName = name+"_"+metadata.getString(METADATA_CACHE_DB_NAME);
			
		}catch(Exception e){			
		}
		mImageLoader = new ImageLoader(this);
		mImageLoadingScope = new UserTaskExecutionScope(name,mMaxPoolSize,mPoolSize,mTimeOutMilli,TimeUnit.MILLISECONDS,mPendingSize);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if(intent == null){
			return;
		}
		String action = intent.getAction();
		Logger.l(Logger.DEBUG, LOG_TAG, "onStart. action:"+action);	
	}
	
	public void loadImage(AsyncLoaderInput input, AsyncListener listener){
		mImageLoader.loadImageAsync(mImageLoadingScope, input, listener);
	}
	
	public Bitmap loadImage(String imageUri, boolean immediately){
		return mImageLoader.loadImage(imageUri, immediately);
	}
	
	class ImageLoaderServiceBinder extends Binder{
		public ImageLoaderService getService(){
			return ImageLoaderService.this;
		}
	}
	
	private final IBinder mBinder = new ImageLoaderServiceBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		String action = intent.getAction();
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onBind()] action: "+action);        
		return mBinder;
	}

}
