package com.neusou.bioroid.restful;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.neusou.Logger;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;
import com.neusou.bioroid.restful.RestfulClient.RestfulResponse;

public abstract class RestfulService extends Service {

	public static final String LOG_TAG = Logger.registerLog(RestfulService.class);
	RestfulResponseProcessor<RestfulResponse> mProcessor; 
	
	public RestfulService() {
		super();		
		mProcessor = new RestfulResponseProcessor<RestfulResponse>(this, getClass()) {		
			@Override
			protected void handleResponse(RestfulResponse response, RestfulMethod method, Bundle requestdata, String error) {				
			}
		};
	}
	
	@Override
	public void onDestroy() {	
		super.onDestroy();
		//Logger.l(Logger.DEBUG, LOG_TAG, "onDestroy");		
	}
	
	@Override
	public void onCreate() {	
		super.onCreate();
		//Logger.l(Logger.DEBUG, LOG_TAG, "onCreate");			
	}
	
	public abstract void doExecute(Intent intent, int startId);
	public abstract void doProcess(Intent intent, int startId);
	public abstract RestfulClient<?> getClient();
	
	@Override
	public final void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		String action = intent.getAction();
		Logger.l(Logger.DEBUG, LOG_TAG, "onStart. action:"+action);
		RestfulClient<?> client = getClient();
		if(action == null){			
		}
		else if(action.equals(client.INTENT_EXECUTE_REQUEST)){
			doExecute(intent, startId);	    	
		}else if(action.equals(client.INTENT_PROCESS_RESPONSE)){
			doProcess(intent, startId);
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
