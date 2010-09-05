package com.neusou.adapter;

public interface IPageableAdapter{
	
	public void onStartLoadingNext();
	public void onFinishedLoadingNext();
	public void onStartLoadingPrev();			
	public void onFinishedLoadingPrev();
	public void onFinishedLoading();
	public void setListener(IPageableListener listener);
	
}