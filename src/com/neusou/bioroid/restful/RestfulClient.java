package com.neusou.bioroid.restful;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.neusou.Logger;

public class RestfulClient<S extends RestfulClient.IRestfulResponse<?>> {
	
	public static final String LOG_TAG = Logger.registerLog(RestfulClient.class);	
	private DefaultHttpClient httpClient;
	
	public String BASE_PACKAGE = "";
	public String INTENT_EXECUTE_REQUEST;
	public String INTENT_PROCESS_RESPONSE;
	public String CALLBACK_ACTION;
	public String CALLBACK_INTENT_ERROR;
	public String CALLBACK_INTENT_SUCCESS;
	
	/**
	 * The xtra key for restful method invocation, its value implements Parcelable and a subclass of RestfulMethod
	 */
	public String XTRA_METHOD;
	
	/**
	 * The xtra key for responses of the restful method invocation, its value implements Parcelable
	 */
	public String XTRA_RESPONSE;	

	/**
	 * The xtra key for the internal error that occurred when sending the request, the value is a string.
	 */
	public String XTRA_ERROR;
	
	/**
	 * The xtra key for the request data that was sent to be the RESTful client, the value is a bundle
	 */
	public String XTRA_REQUEST;
	
	private String mName = "default";
	private Context mContext;
	private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(1,20,1,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());
	
	private LinkedHashMap<Integer, Class<?>> mMethods = new LinkedHashMap<Integer, Class<?>>(1);
	private ResponseHandler<S> mResponseHandler;
	
	public static final String KEY_CALL_METHOD = "CALL_METHOD";
	public static final String KEY_PROCESS_RESPONSE = "PROCESS_RESPONSE";
	public static final String KEY_EXECUTE_REQUEST = "EXECUTE_REQUEST";
	public static final String KEY_RESPONSE = "RESPONSE";
	public static final String KEY_ERROR = "ERROR";
	public static final String KEY_REQUEST = "ORIGINAL";
	public static final String KEY_CALLBACK_INTENT = "CALLBACK_INTENT";
	public static final String KEY_CALLBACK_INTENT_ERROR = "CALLBACK_INTENT_ERROR";
	public static final String KEY_CALLBACK_INTENT_SUCCESS = "CALLBACK_INTENT_SUCCESS";
	
	private void init(String packageName){
		BASE_PACKAGE = packageName+"."+mName+".restful";
		XTRA_METHOD = BASE_PACKAGE+".CALL_METHOD";
		INTENT_PROCESS_RESPONSE = BASE_PACKAGE+".PROCESS_RESPONSE";
		INTENT_EXECUTE_REQUEST = BASE_PACKAGE+".EXECUTE_REQUEST";
		XTRA_RESPONSE = BASE_PACKAGE+".RESPONSE";	
		XTRA_ERROR = BASE_PACKAGE+".ERROR";
		XTRA_REQUEST = BASE_PACKAGE+".ORIGINAL";
		CALLBACK_ACTION = BASE_PACKAGE+".CALLBACK_INTENT";
		CALLBACK_INTENT_ERROR = BASE_PACKAGE+".CALLBACK_INTENT_ERROR";
		CALLBACK_INTENT_SUCCESS = BASE_PACKAGE+".CALLBACK_INTENT_SUCCESS";		
	}
	
	public void setContext(Context ctx) throws IllegalArgumentException{
		if(ctx == null){
			throw new IllegalArgumentException();
		}
		mContext = ctx;
		init(ctx.getPackageName());
	}
	
	public static final String generateBaseDomain(String clientName){
		StringBuffer sb = new StringBuffer(RestfulClient.class.getPackage().getName());
		return sb.append(".").append(clientName).append(".restful.").toString();
	}
	
	public static final String generateKey(String packageName, String clientName, String action){
		StringBuffer sb = new StringBuffer(packageName);
		return sb.append(".").append(clientName).append(".restful.").append(action).toString();
	}
	
	public void cleanUp(){
		httpClient.getConnectionManager().shutdown();		
	}
	
	public RestfulClient(Context context, LinkedHashMap<Integer, Class<?>> methods, ResponseHandler<S> responseHandler, String name) {
		if(name == null){
			throw new IllegalArgumentException("name can not be null");
		}
		if(responseHandler == null){
			throw new IllegalArgumentException("response handler can not be null");
		}
		if(methods == null){
			throw new IllegalArgumentException("method maps can not be null");
		}
		if(context == null){
			Logger.l(Logger.WARN, LOG_TAG, "Required Context argument is null.");
		}
						
		mMethods = methods;
		mContext = context;
		mResponseHandler = responseHandler;	
		mName = name;
		
		HttpParams httpParams = new BasicHttpParams();
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
		httpParams.setBooleanParameter("http.protocol.expect-continue", false);
		
		SchemeRegistry scheme = new SchemeRegistry();		
		scheme.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		sslSocketFactory.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		scheme.register(new Scheme("https", sslSocketFactory, 443));
		
		ThreadSafeClientConnManager tscm = new ThreadSafeClientConnManager(httpParams, scheme);
		
		httpClient = new DefaultHttpClient(tscm,httpParams);
		httpClient.setReuseStrategy(new ConnectionReuseStrategy() {			
			@Override
			public boolean keepAlive(HttpResponse response, HttpContext context) {				
				return false;
			}
		});
		
		
		mExecutor.setRejectedExecutionHandler(new RejectedExecutionHandler() {			
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			//	Logger.l(Logger.DEBUG, LOG_TAG, "rejectedExecution. #activethread:"+executor.getActiveCount()+", queue.size:"+executor.getQueue().size());				
			}
		});
	}
	
	public static Parcelable getParcelable(Bundle data, String name) {
		boolean invocation = data.containsKey(name);
		if (!invocation) {
			return null;
		}
		return data.getParcelable(name);
	}

	public static <T> T getParcelable(Bundle data, Class<T> type) {
		String name = type.getClass().getCanonicalName();
		boolean invocation = data.containsKey(name);
		if (!invocation) {
			return null;
		}
		return (T) data.getParcelable(name);
	}

	public <T extends HttpRequestBase> S sendRequest(String request, T method) throws Exception {
		//Logger.l(Logger.DEBUG, LOG_TAG, "request line: " + request);
		DefaultHttpClient httpClient = new DefaultHttpClient();
		return httpClient.execute(method, mResponseHandler);
	}
	
	public <T extends HttpRequestBase> S sendRequest(String request, Class<T> method) throws Exception {
	//	Logger.l(Logger.DEBUG, LOG_TAG, "request line: " + request);
		URI reqURI = URI.create(request);
		T httpMethod;
		try {
			httpMethod = (T) method.newInstance();
			httpMethod.setURI(reqURI);
			
			S response = httpClient.execute(httpMethod, mResponseHandler);
			//Log.d(LOG_TAG, " response: " + response.toString());		
			return response;
		} catch (IllegalAccessException e) {
			e.printStackTrace();			
			throw e;
		} catch (InstantiationException e) {
			e.printStackTrace();			
			throw e;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw e;
		} catch (SecurityException e) {
			e.printStackTrace();
			throw e;
		} catch (UnknownHostException e){
			e.printStackTrace();
			throw e;
		} finally{			
		}
				
	}

	public void execute(final Bundle b) throws IllegalArgumentException{
		
		Runnable r = new Runnable() {			
			@Override
			public void run() {
				if(b == null){
		//			Logger.l(Logger.WARN, LOG_TAG, "no execution data");
					return;
				}
				if(! b.containsKey(XTRA_METHOD)){
					throw new IllegalArgumentException("Does not have method information to execute");
				}
				
				RestfulMethod method = (RestfulMethod) getParcelable(b, XTRA_METHOD);
			//	Logger.l(Logger.WARN, LOG_TAG, "executing restful method "+method.describeContents()+" "+method.getClass().getCanonicalName());
				
				method.go(b);
			}
		};
		
		//Logger.l(Logger.DEBUG, LOG_TAG, "executing runnable.");
		mExecutor.execute(r);
	}
	
	public <T extends HttpRequestBase> void execute(final T httpMethod,final Bundle data) {
		//Logger.l(Logger.DEBUG, LOG_TAG, httpMethod.getRequestLine().toString());
		DefaultHttpClient httpClient = new DefaultHttpClient();
		S response = null;
		String exceptionMessage = null;
		
		try {
			 response = httpClient.execute(httpMethod, mResponseHandler);
		} catch (ClientProtocolException e) {		
			e.printStackTrace();
			exceptionMessage = e.getMessage();
			httpMethod.abort();
		} catch (IOException e) {
			e.printStackTrace();
			exceptionMessage = e.getMessage();
			httpMethod.abort();
		}
		
		// process response
						
		//Logger.l(Logger.DEBUG, LOG_TAG, "starting service with action: "+INTENT_PROCESS_RESPONSE);
		Intent processIntent = new Intent();
		processIntent.setAction(INTENT_PROCESS_RESPONSE);					
		processIntent.putExtra(XTRA_RESPONSE, response);
		processIntent.putExtra(XTRA_ERROR, exceptionMessage);
		processIntent.putExtra(XTRA_REQUEST, data);
		
		mContext.startService(processIntent);		
	}
	
	/*
	//generic restful execution
	public <T extends HttpRequestBase> void execute(final String request, final Bundle data, final Class<T> httpMethod) {
				S response = null;
				String exceptionMessage = null;
				try {
					response = sendRequest(request, httpMethod);
				} catch (Exception e) {
					e.printStackTrace();
					onExecuteError(e);					
					exceptionMessage = e.getClass().getCanonicalName()+": "+e.getMessage();
				}
				
				// process response
								
				Logger.l(Logger.DEBUG, LOG_TAG, "starting service with action: "+INTENT_PROCESS_RESPONSE);
				Intent processIntent = new Intent();
				processIntent.setAction(INTENT_PROCESS_RESPONSE);					
				processIntent.putExtra(XTRA_RESPONSE, response);
				processIntent.putExtra(XTRA_ERROR, exceptionMessage);
				processIntent.putExtra(XTRA_REQUEST, data);
				
				mContext.startService(processIntent);
			
	}
	*/
	
	/**
	 * Sends an Intent callback to the original caller
	 * @param ctx
	 * @param data
	 */
	public static void broadcastCallback(Context ctx, Bundle data, String action) {
		if (ctx != null ){			
			//Log.d(LOG_TAG,"broadcastCallback action:"+action);
			Intent i = new Intent(action);
			i.putExtras(data);
			ctx.sendBroadcast(i);
		}
	}

	public static interface RestfulMethod extends Parcelable{		
		public void go(Bundle b);
		public long getCallId();
		public void setCallId(long callId);
	}
	
	public static abstract class BaseRestfulMethod implements RestfulMethod{
		long mCallId;		
		@Override
		public long getCallId() {		
			return mCallId;
		}
		@Override
		public void setCallId(long callId) {		
			mCallId = callId;
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeLong(mCallId);			
		}
		
		public static void createFromParcel(BaseRestfulMethod obj, Parcel source){
			obj.setCallId(source.readLong());
		}
	}
	
	/**
	 * Default implementation of IRestfulResponse
	 * @author asantoso
	 *
	 */
	public static class RestfulResponse implements IRestfulResponse<String>{
		
		private String data;
		
		public RestfulResponse(String response) {
			data = response;
		}
		
		@Override
		public int describeContents() {			
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(data);
		}
		
		public static final Parcelable.Creator<RestfulResponse> CREATOR = new Creator<RestfulResponse>() {
			
			@Override
			public RestfulResponse[] newArray(int size) {
				return null;
			}
			
			@Override
			public RestfulResponse createFromParcel(Parcel source) {
				return new RestfulResponse(source.readString());
			}
		};
	
		@Override
		public void setData(String data) {
			this.data = data;
		}
		
		@Override
		public String getData() {
			return this.data;
		}
		
		
	}
		
	public static interface IRestfulResponse<D> extends Parcelable{
		public void setData(D data);
		public D getData();
	};


	
}


