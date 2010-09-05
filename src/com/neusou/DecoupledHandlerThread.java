package com.neusou;

import java.util.concurrent.TimeUnit;

import android.os.Handler;

/**
 * Another version of LeadingLooperThread that:  <br/>
 * 1. does not need an external initialization of the latch. <br/>
 * 2. has it's own handler running on internal thread. <br/>
 * 
 * <br/>
 * <h1>Usage:</h1><br/>
 * <code>
 * DecoupledHandlerThread dht = new DecoupledHandlerThread();<br/>
 * dht.start();<br/>
 * Runnable r = new Runnable(){..};<br/>
 * dht.post(r);<br/>
 * </code>
 * 
 * @author asantoso
 *
 */
public class DecoupledHandlerThread extends ProactiveThread{
	
	public Handler h;
	
	public DecoupledHandlerThread() {
		super();		
	}
	
	public DecoupledHandlerThread(long initWaitTime, TimeUnit unit) {
		super();
		mTimeOut = initWaitTime;
		mTimeOutUnit = unit;
	}
	
	@Override
	public void doRun() {
		this.h = new Handler();		
	}
	
}