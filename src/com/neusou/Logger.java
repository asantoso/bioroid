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

package com.neusou;

import android.util.Log;

public class Logger {
	
	public static boolean show = true;
	public static final byte DEBUG = 0;
	public static final byte ERROR = 1;
	public static final byte INFO = 2;
	public static final byte VERBOSE = 3;
	public static final byte WARN = 4;
		
	public static synchronized void l(byte type, String tag, String msg){
		if(!show ){
			return;	 
		}
		
		
		
		StringBuffer sb = new StringBuffer(Thread.currentThread().getName());
		sb.append(" --- ");
		sb.append(msg);
		
		switch(type){
			case DEBUG:{
				Log.d(tag,sb.toString());
				break;
			}
			case ERROR:{
				Log.d(tag,sb.toString());
				break;
			}
			case INFO:{
				Log.d(tag,sb.toString());		
				break;
			}
			case WARN:{
				Log.d(tag,sb.toString());				
				break;
			}
			case VERBOSE:{
				Log.d(tag,sb.toString());				
				break;
			}
		}
	}
	
	/**
	 * Register the class to use logger functionality
	 * @return log tag
	 */
	public static String registerLog(Class c){
		return c.getCanonicalName();
	}
	
}