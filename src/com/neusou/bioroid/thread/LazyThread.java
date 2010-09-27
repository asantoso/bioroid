/*
Copyright 2010 Agus Santoso

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.neusou.bioroid.thread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.Looper;
import android.os.Process;

/**
 * <b>LazyThread</b> that waits for a countdown latch either internal or external before excuting its runnable code.
 * @author asantoso
 */
public abstract class LazyThread extends Thread{
	private CountDownLatch mWaitLatch;
	public static final long TIMEOUT = 10000l;
	public static final TimeUnit TIMEOUT_UNIT  = TimeUnit.MILLISECONDS; 
	long mTimeOut = TIMEOUT;
	TimeUnit mTimeOutUnit = TIMEOUT_UNIT;
	int mThreadPriority = Process.THREAD_PRIORITY_DEFAULT;
	
	public CountDownLatch getLatch(){
		return mWaitLatch;
	}
	
	/**
	 * Construct a lazy thread with an internal latch 
	 */	
	public LazyThread(){
		this.mWaitLatch = new CountDownLatch(1);		
	}	
	
	/**
	 * Construct a lazy thread with an internal latch with a specific latch count	
	 * @param i latch count
	 */
	public LazyThread(int count){
		this.mWaitLatch = new CountDownLatch(count);		
	}	
		
	/**
	 * Construct a lazy thread with an external latch 
	 */	
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