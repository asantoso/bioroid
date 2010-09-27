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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Parcelable;

import com.neusou.Logger;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;
import com.neusou.bioroid.restful.RestfulService.Metadata;

public abstract class RestfulResponseProcessor<S extends Parcelable> {

	private static final String LOG_TAG = Logger.registerLog(RestfulResponseProcessor.class);
			
	Context mContext;
	Class<?> mServiceClass;
	RestfulClient mClient;
	
	/**
	 * Creates a restful response processor
	 * 
	 * @param ctx
	 * @param serviceClass 
	 */
	public RestfulResponseProcessor(Context ctx, Class<?> serviceClass) {
		this.mContext = ctx;
		this.mServiceClass= serviceClass;
	}

	private void broadcastCallback(Bundle data, String action) {
		mClient.broadcastCallback(mContext, data, action);
	}

	public void setClient(RestfulClient client){
		mClient = client;
	}

	final public void onHandleIntent(Intent intent) {

		String action = intent.getAction();
		Bundle data = intent.getExtras();
		//Logger.l(Logger.DEBUG, LOG_TAG, "onHandleIntent() action: " + action);

		String xtra_method = null;
		String xtra_response = null;
		String xtra_error = null;
		String xtra_request = null;
		String callback_intent = null;

		Bundle metadata;
		
		try {
			metadata = mContext.getPackageManager().getServiceInfo(
					new ComponentName(mContext, mContext.getClass()),
					PackageManager.GET_META_DATA).metaData;

			String identifier = metadata.getString(Metadata.IDENTIFIER);

			//Logger.l(Logger.DEBUG, LOG_TAG, "identifier: "+identifier);
			
			String contextPackageName = mContext.getApplicationContext().getPackageName();
			
			xtra_method = RestfulClient.generateKey(contextPackageName,identifier,
					RestfulClient.KEY_CALL_METHOD);
			xtra_response = RestfulClient.generateKey(contextPackageName, identifier,
					RestfulClient.KEY_RESPONSE);
			xtra_error = RestfulClient.generateKey(contextPackageName, identifier,
					RestfulClient.KEY_ERROR);
			xtra_request = RestfulClient.generateKey(contextPackageName, identifier,
					RestfulClient.KEY_REQUEST);
			callback_intent = RestfulClient.generateKey(contextPackageName, identifier,
					RestfulClient.KEY_CALLBACK_INTENT);

		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
/*
		Logger.l(Logger.DEBUG, LOG_TAG, 
				"\n"+
				"xtra_request: "+xtra_request+"\n"+
				"xtra_method: "+xtra_method+"\n"+
				"xtra_error: "+xtra_error+"\n"+
				"callback_intent: "+callback_intent);
	*/	
		String error = data.getString(xtra_error);
		Bundle request = data.getBundle(xtra_request);

		if(request != null){
			if (request != null && request.containsKey(xtra_method)) {
				Parcelable restMethod = request.getParcelable(xtra_method);
				S response = data.getParcelable(xtra_response);
				handleResponse(response, (RestfulMethod) restMethod, data.getBundle(xtra_request), error);
			} else {
			//	Logger.l(Logger.WARN, LOG_TAG, "no method data. value of xtra "+xtra_method+" is null");
			}
			broadcastCallback(data, callback_intent);
		}else{
		//	Logger.l(Logger.WARN, LOG_TAG, "no execution request data. value of xtra "+xtra_request+" is null");
		}
		
	}

	protected abstract void handleResponse(S response, RestfulMethod method, Bundle requestdata, String error);

}
