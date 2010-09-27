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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;

import com.neusou.Logger; 
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;
import com.neusou.bioroid.thread.DecoupledHandlerThread;

/**
 * RestfulCallback handles callback from asynchronous calls made by RestfulClient.
 * 
 * @author asantoso
 * 
 */
public abstract class RestfulCallback extends BroadcastReceiver {

	private static final String LOG_TAG = Logger.registerLog(RestfulCallback.class);

	private String callbackAction;
	private String xtraMethod;
	private String xtraResponse;
	private String xtraError;
	private String xtraRequest;

	/**
	 * A handler that runs on a separate thread than the Main thread.
	 */
	private DecoupledHandlerThread mThread = new DecoupledHandlerThread();
	
	private RestfulClient mClient;
	
	/**
	 * Creates a RestfulCallback with a restful client
	 * @param action
	 */
	public RestfulCallback(RestfulClient client) {	
		mClient = client;
		callbackAction = client.CALLBACK_ACTION;
		xtraMethod = client.XTRA_METHOD;
		xtraResponse = client.XTRA_RESPONSE;
		xtraError = client.XTRA_ERROR;
		xtraRequest = client.XTRA_REQUEST;
		mThread.start();		
	}
	
	/**
	 * Receives callback intents from restful client.  
	 */
	@SuppressWarnings("unchecked")
	@Override
	final public void onReceive(Context context, Intent intent) {
		Logger.l(Logger.DEBUG, LOG_TAG,"onReceive "+intent.getAction());
		/*
		Log.d(LOG_TAG,"onReceive: xtra_method:"+xtraMethod);
		Log.d(LOG_TAG,"onReceive: xtra_response:"+xtraResponse);
		Log.d(LOG_TAG,"onReceive: xtra_error:"+xtraError);
		Log.d(LOG_TAG,"onReceive: xtra_request:"+xtraRequest);
		*/
		
		Bundle b = intent.getExtras();
		Bundle request = intent.getBundleExtra(xtraRequest);
		
    	RestfulMethod restMethod = (RestfulMethod) request.getParcelable(xtraMethod);
    	//S response = (S) b.getParcelable(xtraResponse);
    	Parcelable rsp =  b.getParcelable(xtraResponse);
    	String error = b.getString(xtraError);

    	//context.removeStickyBroadcast(intent);    	
    	int resultCode = onCallback(restMethod, rsp, error);

    	setResultCode(resultCode);
	}
	
	public static final int CONSUMED = 0;
	public static final int NOTCONSUMED = 1;
	
	/**
	 * This onCallback method must be implemented to properly receive a restful callback.<br/>
	 * The code is executed in a separate thread from the Main UI thread thereby long operations are safe to be performed.
	 * @param <T>
	 * @param restMethod the restful method that was originally called.
	 * @param response is null if error is not null
	 * @param error the error string
	 */
	public abstract <T extends RestfulMethod> int onCallback(T restMethod, Parcelable response, String error);
	
	/**
	 * Registers this callback 
	 * Returns a sticky intent if there is one. 
	 * The sticky intent returned is useful when activity had been destroyed while the restful callback intent occurred.
	 * For example: The new activity instance can retrieve the sticky intent and closes any progress dialogs.
	 * @param ctx
	 * @return
	 */
	public Intent register(Context ctx){
		IntentFilter filter = new IntentFilter();
		filter.addAction(callbackAction);		
		//here we set a handler of a non UI thread so that the onReceive occurs in a separate thread than the thread where the context is in (which is usually the main UI thread).
		return ctx.registerReceiver(this, filter, null, mThread.h);
	}
	
	/**
	 * Unregisters this callback
	 * @param ctx
	 */
	public void unregister(Context ctx){
		ctx.unregisterReceiver(this);
	}

}
