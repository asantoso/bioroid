package com.neusou.bioroid.web;

import android.os.Parcel;
import android.os.Parcelable;

import com.neusou.Logger;

/**
 * <b>PagingInfo</b> is a simple paging controller for viewing large datasets. <br/>
 * The class manages the start retrieval index, and the expected number of
 * records received (window size)
 * 
 * <br/><br/>
 * The compute(int direction) is called before a remote invocation. <br/>
 * the update() method must be called after a remote response is received
 * locally.
 * 
 * <br/><br/>
 * 
 * <b><i>count</i></b> is the size of the local dataset. <br/>
 * <b><i>start</i></b> is the remote index of the first item in the local dataset. <br/>
 * <b><i>totalCount</i></b> is the size of the remote dataset. <br/>
 * <b><i>windowSize</i></b> is the number of items for the next remote request. <br/>
 * 
 * @author asantoso
 * 
 */
public class PagingInfo implements Parcelable{
	/**
	 * the default size of the data window
	 */
	public static final int DEFAULTWINDOWSIZE = 25;

	public static final String LOG_TAG = "PagingInfo";
	
	public static final String XTRA_OBJECT = PagingInfo.class.getName();

	public static final int NEXT = 1;
	public static final int FIRSTRECORD = -2;
	public static final int PREV = -1;

	/**
	 * The size of the data available on the remote side.
	 */
	public int totalCount;

	/**
	 * the number of data rows successfully retrieved
	 */
	public int count;

	/**
	 * the size of the data window for the next retrieval
	 */
	public int windowSize = DEFAULTWINDOWSIZE;

	/**
	 * the starting index, using zero-based index
	 */
	public int start;

	/**
	 *
	 */
	public int nextStart;

	/**
	 * The first row id in the local dataset
	 */
	int lastStartRowId;

	/**
	 * The last row id in the local dataset
	 */
	int lastEndRowId;

	/**
	 * The offset of the index used in the paging info with respect to the
	 * zero-based index<br/>
	 * 0 - zero based index<br/>
	 * 1 - one-based index
	 */
	public int indexStartingOffset;

	public boolean isRemoteSiteHasNext = false;
	public boolean isRemoteSiteHasPrev = false;

	public static final int CURRENT = 0;

	/**
	 * Check if the operation successful by looking at the new rowid and endid
	 * 
	 * @param startId
	 * @param endId
	 * @return
	 */
	public boolean isLastOperationSuccessful(int startId, int endId) {
		Logger
				.l(Logger.DEBUG, LOG_TAG, "time0:" + startId + "+,time1:"
						+ endId + ",lasttime0:" + lastStartRowId
						+ ",lasttime1:" + lastEndRowId);
		if (startId != lastStartRowId || endId != lastEndRowId) {
			lastStartRowId = startId;
			lastEndRowId = endId;
			return true;
		}
		return false;
	}

	public PagingInfo(int indexStartingOffset) {
		this.indexStartingOffset = indexStartingOffset;
	}

	public String toString() {
		return  "start: " + start + 
				", count:" + count +
				", totalcount: " + totalCount +
				", windowSize: " + windowSize +
				
				", hasNext: " + hasNext() + 
				", hasPrev: " + hasPrev();
	}

	public boolean hasNext() {
		return (start + count - indexStartingOffset) < totalCount;
	}

	public boolean hasPrev() {
		return (start > 0 && count != 0);
	}

	public void clear() {
		totalCount = 0;
		count = 0;
		start = 0;
		windowSize = DEFAULTWINDOWSIZE;
	}

	public void lastOperationSuccessful(int count, int totalCount) {
		this.count = count;
		this.totalCount = totalCount;
		start = nextStart;
		computePaging();
	}

	public int getNextStart() {
		return nextStart;
	}

	public static final int RECORD_ID_UNKNOWN = -1;

	/**
	 * Updates the internal paging state after a remote invocation has been
	 * performed.
	 * 
	 * @param time0
	 * @param time1
	 * @param count
	 * @param totalCount
	 * @return
	 */
	public boolean update(int time0, int time1, int count, int totalCount) {
		assert count >= 0;

		Logger.l(Logger.DEBUG, LOG_TAG, "start:"+start+", nextstart:"+nextStart+", count:"+count+", total:"+totalCount);
		boolean validBatch = false;
		boolean success = false;
		
		this.count = count;
		
		// do a quick comparison of the last recorded ids to determine whether
		// the new batch is valid
		if ((time0 != RECORD_ID_UNKNOWN && lastStartRowId != time0)
				|| (time1 != RECORD_ID_UNKNOWN && lastEndRowId != time1)) {
			validBatch = true;
		}

		// always assume the batch is valid if pre-invocation total count differs than post-invocation total count
		if (this.totalCount != totalCount) {
			validBatch = true;
		}

		if (validBatch) {
			
			lastStartRowId = time0;
			lastEndRowId = time1;
			
			this.totalCount = totalCount;

			// when previous requested starting index is greater than the totalCount - 1 that means
			// the reported totalCount post-invocation happens to be smaller
			// compared to the totalCount pre-invocation.
			// hence, we update and bound the current start and the next start indices.
			if (nextStart > totalCount - 1) {
				nextStart = totalCount - 1;
			}
			start = nextStart;
			
			// make sure the start index is less than the totalCount and greater than 0
			assert start < totalCount;
			assert 0 <= start;
			success = true;
			
		}

		computePaging();
		return success;
	}

	/**
	 * Computes the required indices used in the actual invocation of remote
	 * data retrieval. <br/>
	 * 
	 * 
	 * @param direction
	 */
	public void compute(int direction) {
		// compute the next starting index to retrieve

		if (count == 0) {
			nextStart = 0;
		} else {
			nextStart = start + windowSize * direction;
		}

		if (nextStart < 0) {
			nextStart = 0;
		}

		else if (start < totalCount && nextStart > totalCount) {
			windowSize = nextStart - totalCount + 2;
		} else {
			windowSize = DEFAULTWINDOWSIZE;
		}

		computePaging();

	}

	public void computePaging() {
		isRemoteSiteHasPrev = hasPrev();
		isRemoteSiteHasNext = hasNext();
		
	}

	public static Creator CREATOR = new Creator();
	
	static class Creator implements Parcelable.Creator<PagingInfo>{

		@Override
		public PagingInfo createFromParcel(Parcel source) {
			
			int start, nextStart, windowSize,lastStartRowId,lastEndRowId, indexStartingOffset;
			boolean isRemoteSiteHasNext, isRemoteSiteHasPrev;
			int count, totalCount;
			
			start = source.readInt();
			nextStart = source.readInt();
			windowSize = source.readInt();
			lastStartRowId = source.readInt();
			lastEndRowId = source.readInt();
			indexStartingOffset = source.readInt();
			isRemoteSiteHasNext = source.readByte()==1?true:false;
			isRemoteSiteHasPrev = source.readByte()==1?true:false;
			count = source.readInt();
			totalCount = source.readInt();
			
			PagingInfo pi = new PagingInfo(indexStartingOffset);
			pi.start = start;
			pi.nextStart= nextStart;
			pi.windowSize = windowSize;
			pi.lastStartRowId = lastStartRowId;
			pi.lastEndRowId = lastEndRowId;
			pi.indexStartingOffset = indexStartingOffset;
			pi.isRemoteSiteHasNext = isRemoteSiteHasNext;
			pi.isRemoteSiteHasPrev = isRemoteSiteHasPrev;
			pi.count = count;
			pi.totalCount = totalCount;
			
			return pi;
		}

		@Override
		public PagingInfo[] newArray(int size) {
			return new PagingInfo[size];			
		}
		
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(start);
		dest.writeInt(nextStart);
		dest.writeInt(windowSize);
		dest.writeInt(lastStartRowId);
		dest.writeInt(lastEndRowId);
		dest.writeInt(indexStartingOffset);		
		dest.writeByte(isRemoteSiteHasNext?(byte)1:0);
		dest.writeByte(isRemoteSiteHasPrev?(byte)1:0);
		dest.writeInt(count);
		dest.writeInt(totalCount);		
	}

}

