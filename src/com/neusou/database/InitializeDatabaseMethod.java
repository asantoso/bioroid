package com.neusou.database;

import java.io.IOException;


/**
 * 
 * @author asantoso@3rdwhale.com
 * @since July 20, 2009
 */
public class InitializeDatabaseMethod {
	
	private static InitializeDatabaseMethod INSTANCE;	
	
	private InitializeDatabaseMethod(){}
	
	public static InitializeDatabaseMethod getInstance(){
		if(INSTANCE == null){
			INSTANCE = new InitializeDatabaseMethod();
		}
		return INSTANCE;
	}	
	
	public void call(Param param){		
		try {
			if(param.sqlParts != null){
				param.dbHelper.createDataBase(param.sqlParts);	
			}
			if(param.sqlFiles != null){
				param.dbHelper.createDataBase(param.sqlFiles);
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static class Result{
		public boolean isSQLCopySuccess;
		public boolean isFragmentCopySuccess;
		
		public Result(){
			
		}
	}
	
	public static class Param{
		public int[]  sqlParts;
		public String[] sqlFiles;	
		public DBHelper dbHelper;
	}
	
}