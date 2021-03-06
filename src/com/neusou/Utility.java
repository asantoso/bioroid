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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

public class Utility {
	
	final String tag =  "Utility";

	public synchronized final InputStream getXmlInputStream(String uri) 
	throws MalformedURLException, IOException, SocketTimeoutException{
		return getXmlInputStream(uri,10000,20000);
	}
	
	public synchronized final InputStream getXmlInputStream(String uri, int connectTimeout, int readTimeout) 
	throws MalformedURLException, IOException, SocketTimeoutException {
		
			Logger.l(Logger.INFO,tag,"uri: "+uri);			
			
			if (uri != null && uri.length() > 0) {
				URL url;
				try {
					url = new URL(uri);
					URLConnection mURLConnection = url.openConnection();
					mURLConnection.setConnectTimeout(connectTimeout);
					mURLConnection.setReadTimeout(readTimeout);
					mURLConnection.connect();					
					return mURLConnection.getInputStream();
				} catch (MalformedURLException e) {
					Logger.l(Logger.ERROR,tag,"getXmlInputStream()->malformed error: "+e.toString());
					throw e;					
				} catch (IOException e) {
					Logger.l(Logger.ERROR,tag,"getXmlInputStream()->io error: "+e.toString());
					throw e;					
				}
			} 
			return null;
	}
	
	public static Drawable getScaledDrawable(Resources res, int resId, int w, int h){
		Drawable d = res.getDrawable(resId);
		Rect rect = d.copyBounds();
		rect.right=w;
		rect.bottom=h;
		d.setBounds(rect);
		return d;
	}
	
	public static Drawable getScaledDrawable(Drawable d, int w, int h){
		Rect rect = d.copyBounds();
		rect.right=w;
		rect.bottom=h;
		d.setBounds(rect);
		return d;
	}
	
	public static void close(Closeable c){
		try{
			c.close();
		}catch(IOException e){			
		}		
	}
	
	
}
	
