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