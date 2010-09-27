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
 * <b>ProactiveThread</b> is a thread that is not lazy. 
 * @author asantoso
 */
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
	
	/**
	 * Construct a proactive thread with an internal wait latch
	 */
	public ProactiveThread(){
		this.mWaitLatch = new CountDownLatch(1);
	}
	
	/**
	 * Construct a proactive thread with an external wait latch
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