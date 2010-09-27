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

import java.util.concurrent.TimeUnit;

import android.os.Handler;

/**
 * DecoupledHandlerThread allows the creation of a handler that runs in a thread <br/>
 * This thread is useful in the following cases:
 * 
 * <ul>
 * <li>
 * you want to have a broadcast receiver that runs in a non-UI thread.
 * </li>
 * </ul>
 * <ul>
 * <li>
 * you want to perform a long operation outside of the main thread.
 * </li>
 * </ul>
 * 
 * <h1>Usage:</h1><br/>
 * <code>
 * DecoupledHandlerThread dht = new DecoupledHandlerThread();<br/>
 * dht.start();<br/>
 * Runnable r = new Runnable(){..};<br/>
 * dht.h.post(r);<br/>
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