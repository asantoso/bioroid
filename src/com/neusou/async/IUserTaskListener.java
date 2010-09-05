package com.neusou.async;

public abstract interface IUserTaskListener<Params,Result>{
	public void onCancelled();
	public void onTimeout();
	public void onPreExecute();
	public void onProgressUpdate(Params... progress);
	public void onPostExecute(Result result);    	
}