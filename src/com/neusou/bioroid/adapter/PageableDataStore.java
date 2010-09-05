package com.neusou.bioroid.adapter;

public interface PageableDataStore<K>{
		public int size();
		public K get(int index);
		public void clear();
		public Object getAt(int index);
	}
	