package com.neusou.bioroid.restful;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.neusou.Logger;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;

public abstract class RestfulCallback<S extends Parcelable> extends BroadcastReceiver {

	private static final String LOG_TAG = Logger.registerLog(RestfulCallback.class);

	private String callbackAction;
	private String xtraMethod;
	private String xtraResponse;
	private String xtraError;
	private String xtraRequest;
	
	/**
	 * Creates a RestfulCallback with the intent action to listen
	 * @param action
	 */
	public RestfulCallback(RestfulClient<?> client) {		
		callbackAction = client.CALLBACK_ACTION;
		xtraMethod = client.XTRA_METHOD;
		xtraResponse = client.XTRA_RESPONSE;
		xtraError = client.XTRA_ERROR;
		xtraRequest = client.XTRA_REQUEST;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	final public void onReceive(Context context, Intent intent) {
		//Log.d(LOG_TAG,"onReceive "+intent.getAction());
		/*
		Log.d(LOG_TAG,"onReceive: xtra_method:"+xtraMethod);
		Log.d(LOG_TAG,"onReceive: xtra_response:"+xtraResponse);
		Log.d(LOG_TAG,"onReceive: xtra_error:"+xtraError);
		Log.d(LOG_TAG,"onReceive: xtra_request:"+xtraRequest);
		*/
		Bundle b = intent.getExtras();
		Bundle request = intent.getBundleExtra(xtraRequest);
    	RestfulMethod restMethod = (RestfulMethod) request.getParcelable(xtraMethod);
    	S response = (S) b.getParcelable(xtraResponse);
    	String error = b.getString(xtraError);
    	onCallback(restMethod, response, error);
	}
	
	public abstract <T extends RestfulMethod> void onCallback(T restMethod, S response, String error);

	public void register(Context ctx){
		IntentFilter filter = new IntentFilter();
		filter.addAction(callbackAction);
		ctx.registerReceiver(this, filter);
	}
	
	public void unregister(Context ctx){
		ctx.unregisterReceiver(this);
	}

}
