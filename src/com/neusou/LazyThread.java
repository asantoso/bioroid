package com.neusou;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.Looper;
import android.os.Process;

/**
 * Thread that waits for a countdown latch either internal or external before excuting its runnable code.
 * @author asantoso
 */
public abstract class LazyThread extends Thread{
	public CountDownLatch mWaitLatch;
	public static final long TIMEOUT = 10000l;
	public static final TimeUnit TIMEOUT_UNIT  = TimeUnit.MILLISECONDS; 
	long mTimeOut = TIMEOUT;
	TimeUnit mTimeOutUnit = TIMEOUT_UNIT;
	int mThreadPriority = Process.THREAD_PRIORITY_DEFAULT;
	
	public LazyThread(){
		this.mWaitLatch = new CountDownLatch(1);		
	}	
	
	public LazyThread(CountDownLatch cdl){
		this.mWaitLatch = cdl;
	}	
	
	/**
	 * Creates a waiting thread 
	 * @param waitLatch the wait latch
	 * @param timeoutMillis the wait timeout in milliseconds
	 */
	public LazyThread(CountDownLatch waitLatch, long timeoutMillis){
		this.mWaitLatch = waitLatch;
		mTimeOut = timeoutMillis;
	}

	public void updateCountdown(){
		mWaitLatch.countDown();
	}
	
	public void setThreadPriority(){
		android.os.Process.setThreadPriority((int)Process.myTid(), mThreadPriority);
	}
	
	public void setThreadPriority(int priority){
		mThreadPriority = priority;
		setThreadPriority();
	}
	
	public abstract void doRun();
	
	@Override
	public final void run() {
		Looper.prepare();
		try{
			setThreadPriority();
		}catch(Exception e){				
		}
		try{
			mWaitLatch.await(mTimeOut, mTimeOutUnit);
			doRun();
		}catch(InterruptedException e){			
		}
		try{
			setThreadPriority();
		}catch(Exception e){				
		}		
		Looper.loop();
	}
	
}