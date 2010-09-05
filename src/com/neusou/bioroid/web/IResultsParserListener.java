package com.neusou.bioroid.web;

public interface IResultsParserListener<D>{
		public PagingInfo getPagingInfo();
		public void onResultSet(int code, int firstResultPosition, int resultCount, int totalCount);
		public void onItemParsed(D item);
		public void onStartParsing();
		public void onFinishParsing();
}