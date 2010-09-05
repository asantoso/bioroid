package com.neusou.bioroid.web;

import java.io.InputStream;

public interface IResultsParser<D> {
		public boolean parse(InputStream parse);
		public void setListener(IResultsParserListener<D> listener);
}
