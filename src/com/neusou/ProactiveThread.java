package com.neusou;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.Looper;
import android.os.Process;

public abstract class ProactiveThread extends Thread{
	public static final long TIMEOUT = 10000;
	public static final TimeUnit TIMEOUTUNIT = TimeUnit.MILLISECONDS;	
	protected volatile CountDownLatch mWaitLatch;	
	public long mTimeOut = TIMEOUT;
	public TimeUnit mTimeOutUnit = TIMEOUTUNIT;
	int mThreadPriority = Process.THREAD_PRIORITY_DEFAULT;
	
	public CountDownLatch getLatch(){
		return mWaitLatch;
	}
	
	public ProactiveThread(){
		this.mWaitLatch = new CountDownLatch(1);
	}
	
	/**
	 * External latch
	 * @param waitLatch
	 */
	public ProactiveThread(CountDownLatch waitLatch){
		this.mWaitLatch = waitLatch;
	}	

	public ProactiveThread(long initWaitTime, TimeUnit unit) {
		super();
		this.mWaitLatch = new CountDownLatch(1);
		mTimeOut = initWaitTime;
		mTimeOutUnit = unit;
	}	
	
	public void setThreadPriority(){
		android.os.Process.setThreadPriority((int)Process.myTid(), mThreadPriority);
	}
	
	public void setThreadPriority(int priority){
		mThreadPriority = priority;
		setThreadPriority();
	}
	
	public void updateCountdown(){
		mWaitLatch.countDown();
	}
	
	public abstract void doRun();
	
	@Override
	public final void run() {		
		Looper.prepare();
		try{
			setThreadPriority();
		}catch(Exception e){				
		}
		doRun();		
		try{
			setThreadPriority();
		}catch(Exception e){				
		}		
		updateCountdown();
		Looper.loop();		
	}
	
	/**
	 * Waits the the initialization of this thread to complete. Invoking this method blocks the calling thread.
	 * Note: gets executed in the calling thread. 
	 * @return
	 */
	public boolean waitInit(){
		try{
			this.mWaitLatch.await(mTimeOut, mTimeOutUnit);
			return true;
		}catch(InterruptedException e){	
		}
		return false;
	}
	
	/**
	 * Start the thread, and blocks the calling thread until the new thread succesfully initializes.
	 */
	@Override
	public synchronized void start() {
		super.start();
		waitInit();
	}
	
}