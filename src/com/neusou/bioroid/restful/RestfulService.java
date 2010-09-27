/*
Copyright 2010 Agus Santoso

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.neusou.bioroid.restful;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.neusou.Logger;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;

public abstract class RestfulService extends Service {

	public static final String LOG_TAG = Logger.registerLog(RestfulService.class);
	RestfulResponseProcessor<StringRestfulResponse> mProcessor; 
	
	public RestfulService() {
		super();		
		mProcessor = new RestfulResponseProcessor<StringRestfulResponse>(this, getClass()) {		
			@Override
			protected void handleResponse(StringRestfulResponse response, RestfulMethod method, Bundle requestdata, String error) {				
			}
		};
		mProcessor.setClient(getClient());
	}
	
	@Override
	public void onDestroy() {	
		super.onDestroy();
	}
	
	@Override
	public void onCreate() {	
		super.onCreate();		
		
	}
	
	public abstract void onExecuteRequest(Intent intent, int startId);
	public abstract void onProcessResponse(Intent intent, int startId);
	public abstract RestfulClient getClient();
	
	class Metadata{
		public static final String CACHE_RESPONSE = "cacheResponse";
		public static final String IDENTIFIER = "identifier";
	}
	
	/**
	 * Reads metadata from manifest and applies it
	 * 
	 * @param ctx the context, this is so that subclasses can work correctly
	 */
	protected void applyMetadata(Context ctx){
		Bundle metadata;		
		try {			
			metadata = ctx.getPackageManager().getServiceInfo(
					new ComponentName(ctx, ctx.getClass()),
					PackageManager.GET_META_DATA).metaData;
			
			boolean useCache = metadata.getBoolean(Metadata.CACHE_RESPONSE);
			getClient().setUseCacheByDefault(useCache);
			
			//String contextPackageName = this.getApplicationContext().getPackageName();
			//Logger.l(Logger.DEBUG, LOG_TAG, "*******************\n****************** "+contextPackageName+" CODE: "+code);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent == null){
			return START_STICKY;
		}
		
		String action = intent.getAction();
		Logger.l(Logger.DEBUG, LOG_TAG, "onStartCommand. action:"+action);
		RestfulClient client = getClient();
		if(action == null){	
		}
		else if(action.equals(client.INTENT_EXECUTE_REQUEST)){
			onExecuteRequest(intent, startId);	 
			Bundle b = intent.getExtras();
        	getClient().execute(b);
		}else if(action.equals(client.INTENT_PROCESS_RESPONSE)){
			onProcessResponse(intent, startId);
			mProcessor.onHandleIntent(intent);	
		}
		// We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	@Override
	public final void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		String action = intent.getAction();
		Logger.l(Logger.DEBUG, LOG_TAG, "onStart. action:"+action);
		RestfulClient client = getClient();
		if(action == null){	
		}
		else if(action.equals(client.INTENT_EXECUTE_REQUEST)){
			onExecuteRequest(intent, startId);	    	
		}else if(action.equals(client.INTENT_PROCESS_RESPONSE)){
			onProcessResponse(intent, startId);
			mProcessor.onHandleIntent(intent);
		}
	}
	
	public void execute(Bundle data){
		Logger.l(Logger.DEBUG, LOG_TAG, "execute");			
		getClient().execute(data);
	}
	
	class RestfulBinder extends Binder{
		public RestfulService getService(){
			return RestfulService.this;
		}
	}
	
	private final IBinder mBinder = new RestfulBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		String action = intent.getAction();
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onBind()] action: "+action);        
		return mBinder;
	}

}
