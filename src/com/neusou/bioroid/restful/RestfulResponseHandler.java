package com.neusou.bioroid.restful;
import org.apache.http.client.ResponseHandler;

public abstract class RestfulResponseHandler<T> implements ResponseHandler<T>{
	public abstract T createResponse(String data);
	
}