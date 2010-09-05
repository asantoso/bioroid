package com.neusou.web;


public class IntelligentPagingInfo extends PagingInfo{

	public IntelligentPagingInfo(int indexStartingOffset) {
		super(indexStartingOffset);
	}
	
	@Override
	public void lastOperationSuccessful(int count, int totalCount) {
	
		super.lastOperationSuccessful(count, totalCount);
	}
	
	@Override
	public boolean isLastOperationSuccessful(int startId, int endId) {
		return super.isLastOperationSuccessful(startId, endId);
	}

	@Override
	public void compute(int direction) {
	
		super.compute(direction);
	}
}