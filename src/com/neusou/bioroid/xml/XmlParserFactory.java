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

package com.neusou.bioroid.xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Factory for creating an xml pull parser
 * @author asantoso
 *
 */
public final class XmlParserFactory{	 
	static XmlPullParserFactory ppf;
	static XmlParserFactory instance;
	
	private XmlParserFactory(){
		try {			
			ppf = XmlPullParserFactory.newInstance();
			ppf.setNamespaceAware(false);			
			ppf.setValidating(false);
		} catch (XmlPullParserException e) {
		
		}			
	}
	
	public static XmlParserFactory getInstance(){
		if(instance == null){
			instance = new XmlParserFactory();
		}
		return instance;
	}
	
	public XmlPullParser createParser(){
		if(ppf == null)return null;
		try {		
			XmlPullParser xpp = ppf.newPullParser();
			return xpp;
		} catch (XmlPullParserException e) {			
			e.printStackTrace();
		}
		return null;
	}	
	
}