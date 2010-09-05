package com.neusou.adapter;

import java.util.HashMap;
import android.app.Activity;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.neusou.Logger;

import com.neusou.web.PagingInfo;

public abstract class GenericPageableAdapter<K,V extends PageableDataStore<K>> extends BaseAdapter implements IPageableAdapter{
	
	private static final String LOG_TAG = "GenericPageableAdapter";

	public static final byte VIEWTYPE_LOADER = 0;
	public static final byte VIEWTYPE_DATA = 1;
	public static final byte TOTAL_VIEWTYPES = 2;
	
	public static final int mInternalTag = 1;
	public static final int mProgressBarId = 10;
	public static final int mLoaderLayoutResId = 10;
	public static final int mLoaderLabelId = 10;
	//HashMap<String,Boolean> mDeletedComments = new HashMap<String, Boolean>(5);			
	
	
	V datastores;
		
		Activity ctx;
		Resources mResources;
		LayoutInflater mLayoutInflater; 	
		Object mDeletedCommentsLock = new Object();		
		
		int[] mJumpCount;
		PagingInfo mPagingInfo;	
		IPageableListener mPageableListener ;
		View nextRowView;
		View prevRowView;
		
		
		View.OnClickListener mGetNextOnClick = new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				mPageableListener.onGetNext();				
			}
		};
		
		View.OnClickListener mGetPrevOnClick = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPageableListener.onGetPrev();				
			}
		};
		
		View.OnKeyListener mGetPrevOnKey = new View.OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				mPageableListener.onGetPrev();	
				return true;
			}
		};
		
		View.OnKeyListener mGetNextOnKey = new View.OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				mPageableListener.onGetNext();
				return true;
			}
		};
		
	
		
		public void setListener(IPageableListener listener){
			mPageableListener = listener;
		}
		
		public void onStartLoadingNext(){
			View v = nextRowView.findViewById(mProgressBarId );
			v.setVisibility(View.VISIBLE);
		}
		public void onFinishedLoadingNext(){
			View v = nextRowView.findViewById(mProgressBarId );
			v.setVisibility(View.GONE);
		}
				
		public void onStartLoadingPrev(){
			View v = prevRowView.findViewById(mProgressBarId );
			v.setVisibility(View.VISIBLE);
		}
		
		public void onFinishedLoadingPrev(){
			View v = prevRowView.findViewById(mProgressBarId );
			v.setVisibility(View.GONE);
		}
		
		public void onFinishedLoading(){
			onFinishedLoadingNext();
			onFinishedLoadingPrev();
		}
		
		public void setPagingInfo(PagingInfo pagingInfo){
			mPagingInfo = pagingInfo;
		}
	
		public void setData(V commentsJsonData){
			this.datastores = commentsJsonData;			
			clearDeletedList();
		}
	
		public String[] getDirtyRows(){		
			return null;
		}
		
		private void clearDeletedList(){
			
		}
		
		public void markDataAsDeleted(String comment_id, int position){
				
		}

		public void clearData(){
			datastores = null;			
		}
		
		public GenericPageableAdapter(Activity ctx) {
			this.ctx = ctx;
			mResources = ctx.getResources();
			mLayoutInflater = ctx.getLayoutInflater();			
			nextRowView = mLayoutInflater.inflate(mLoaderLayoutResId, null, false);
			prevRowView = mLayoutInflater.inflate(mLoaderLayoutResId, null, false);
		}
					
		@Override
		public int getCount() {
			if(datastores == null){
				return 0;
			}
			int len = datastores.size();// - mDeletedComments.size();
			int add = 0;
			if(mPagingInfo.isRemoteSiteHasNext){
				add = 1;
			}
			if(mPagingInfo.isRemoteSiteHasPrev){
				add++;
			}
			return len + add; 
		}
			
		

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		@Override
		public int getViewTypeCount() {
			return TOTAL_VIEWTYPES;
		}
		
		@Override
		public int getItemViewType(int position) {
			if(mPagingInfo.isRemoteSiteHasPrev){
				if(position == 0){
					return VIEWTYPE_LOADER;	
				}
			}
			if(mPagingInfo.isRemoteSiteHasNext){
				if(position == getCount() - 1){
					return VIEWTYPE_LOADER;
				}	
			}						
			return VIEWTYPE_DATA;
		}
		
		public abstract View createDataView(int requestedPosition, View convertView, ViewGroup parent);
			
		@Override
		public final View getView(int requestedPosition, View convertView, ViewGroup parent) {			
			InternalTag internalTag = null;
						 
			int totalRows = getCount();
			
			Logger.l(Logger.DEBUG,LOG_TAG,"[getView()] " +
					" hasPrev: "+mPagingInfo.isRemoteSiteHasNext +
					", hasNext: "+mPagingInfo.isRemoteSiteHasPrev +
					", start: "+mPagingInfo.getNextStart() +
					", reqPos:"+requestedPosition+", count:"+totalRows);
					
			boolean showViewPrev = (requestedPosition == 0 && mPagingInfo.isRemoteSiteHasPrev);
			boolean showViewNext = (requestedPosition == totalRows - 1 && mPagingInfo.isRemoteSiteHasNext);

			
			if(showViewPrev || showViewNext){
				
				if(showViewNext){
					convertView = nextRowView;
				}
				else if(showViewPrev){
					convertView = prevRowView;
				}
				internalTag = (InternalTag)convertView.getTag(mInternalTag);
				if(internalTag == null){
					internalTag = new InternalTag();
					convertView.setTag(mInternalTag,internalTag);
				}
				internalTag.viewType = VIEWTYPE_LOADER;
				
				TextView label = (TextView) convertView.findViewById(mLoaderLabelId);
				
				if(showViewNext){
					label.setText("view next");
					internalTag.loadDirection = PagingInfo.NEXT;
				}else{
					label.setText("view previous");
					internalTag.loadDirection = PagingInfo.PREV;
				}				
				
				if(mPageableListener != null){
					if(showViewNext){						
						mPageableListener.onHasNext();
					}
					if(showViewPrev){
						mPageableListener.onHasPrev();
					}
				}
				
				return convertView;
			}
			
			//show data row			
			int calculatedRequestedDataRowIndex = requestedPosition - (mPagingInfo.isRemoteSiteHasPrev?1:0);
			if(convertView != null){
				internalTag = (InternalTag) convertView.getTag(mInternalTag);
			}
			if(convertView == null || internalTag != null && internalTag.viewType != VIEWTYPE_DATA){
				//convertView = mLayoutInflater.inflate(R.layout.t_comment, parent, false);
				convertView = createDataView(calculatedRequestedDataRowIndex, convertView,  parent);
				internalTag = new InternalTag();	
				internalTag.viewType = VIEWTYPE_DATA;
			}
			
			return convertView;
		}
		
		
		public class InternalTag{
			public byte viewType;
			public byte loadDirection; // 1 or -1
		}
		
		
		
	};