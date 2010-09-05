package com.neusou;

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