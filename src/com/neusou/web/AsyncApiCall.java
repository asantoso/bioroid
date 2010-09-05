package com.neusou.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import com.neusou.async.UserTask;

public abstract class AsyncApiCall<K extends IResultsParser,V extends IResultsParserListener> extends UserTask<AsyncApiCall.CallInput, Void, AsyncApiCall.CallOutcome> {

	K mParser;
	V mParserListener;
	
	public abstract InputStream invokeRemoteCall(int method, HashMap<String,Object> param)
	throws MalformedURLException, SocketTimeoutException, IOException;
	
	public AsyncApiCall(K parser,V parserListener) {
		mParser = parser;
		mParserListener = parserListener;
	}
		
	public static class CallInput{
		public int apiMethod;
		public HashMap<String,Object> parameters;
		public CallInput (int method, HashMap<String,Object> parameters){
			this.apiMethod = method;
			this.parameters = parameters;
		}
	}
	
	public static class CallOutcome{		
		//external
		public int responseCode;
		public PagingInfo pagingInfo;
		
		//internal
		public boolean isNullResponse = false;
		public boolean hasException = false;
		public Exception e = null;
	}
	
	protected CallOutcome doInBackground(CallInput... params) {
		if (params == null || params.length == 0) {
			cancel(true);
			return null;
		}
		
		CallInput input = params[0];
		CallOutcome outcome = new CallOutcome();
		InputStream response = null;
		try{
			response = invokeRemoteCall(input.apiMethod, input.parameters);			
		}catch(MalformedURLException  e){
			e.printStackTrace();
			outcome.e = e;
			outcome.hasException = true;
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			outcome.e = e;
			outcome.hasException = true;
		} catch (IOException e) {
			e.printStackTrace();
			outcome.e = e;
			outcome.hasException = true;
		}
				
		if(response == null){
			outcome.isNullResponse = true;			
		}
		if(outcome.hasException){			
			return outcome;
		}
				
		if(mParser != null){
			mParser.setListener(mParserListener);
			mParser.parse(response);
			outcome.pagingInfo = mParserListener.getPagingInfo();
		}
		
		return outcome;
	}

};
