package com.neusou.database;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


import com.neusou.Logger;
import com.neusou.database.InitializeDatabaseMethod;
import com.neusou.database.InitializeDatabaseTask;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * <p>
 * DBHelper is an abstract database helper
 * </p>
 * 
 * Functionalities: <br/>
 * <ol> 
 * <li>Create sqlite3 local db from fragment(s)</li>
 * <li>Create sqlite3 local db from sql files containing sql inserts/updates/deletes statements</li>
 * <li>Dumps a cursor onto android log (during development)</li> 
 * </ol>
 * 
 * <b>1. Create sqlite3 db from fragment(s) </b>
 * <br/>by invoking {@link #createDataBase(int[])}
 * <p>split .db file exceeding 1MB into multiple files and provide ids of the raw resource files.</p>
 * 
 * <b>2. Create sqlite3 db from sql files </b>
 * <br/>by invoking {@link #createDataBase(String[])} 
 * <p>provide sql files (assets) containing only non-query statements.</p>
 * 
 * <b>3. Dumps a cursor on the log</b>
 * <br/>by invoking {@link #dumpCursor(Cursor)} 
 *  
 * <br/>
 * <br/>
 * @author asantoso@3rdwhale.com
 * @see SQLiteOpenHelper
 * @see InitializeDatabaseMethod
 * @see InitializeDatabaseTask
 * @see DBHelperListener
 * @since July 2009
 */
public abstract class DBHelper extends SQLiteOpenHelper{
 
    protected Context myContext;        
    protected int[] mRawParts;
    private AssetManager mAssetManager;
    private DBHelperListener mDBHelperListener;
    
    public abstract String getDataBasePath();
    
    /**
     * A listener for DBHelper.
     * 
     * @author asantoso
     */
    public static interface DBHelperListener{
    	/**
    	 * called when a db fragment has been read and copied.
    	 **/
    	public void onCompleteDatabaseFragment(int currentFragment, int totalFragments);
    	/**
    	 * called when one sql file has been executed. 
    	 **/
    	public void onCompleteDatabaseSQL(int currentSQLFile, int totalSQLFiles);
    	/**
    	 * called when all sql files have been executed.
    	 **/
    	public void onFinishDatabaseReconstructionUsingSQLFiles();
    	/**
    	 * called when all db fragments have been copied.
    	 **/
    	public void onFinishDatabaseReconstructionUsingParts();
    }
    
    public void setListener(DBHelperListener listener){
    	this.mDBHelperListener = listener;
    }
    
    public DBHelper(Context context, String dbName, int version) { 
    	super(context, dbName, null, version);
        this.myContext = context;
        this.mAssetManager = context.getAssets();
    }	
   
    /**
     * Do a DB reconstruction by reading text files containing non-query sql statements. 
     * @param sqlFiles name of the sql files as path in the assets.
     */
    public void createDataBase(String[] sqlFiles){    	
    	boolean dbExist = checkDataBase(); 
    	if(dbExist){
    	}
    	else{        	 
        	try {
        		SQLiteDatabase db = this.getReadableDatabase();	
        		int len = sqlFiles.length;
            	for(int i=0;i<len;i++){
            		try{    			
            			Logger.l(Logger.INFO,"DBHelper","createDatabase()->opening file: "+sqlFiles[i]);
            			InputStream is = mAssetManager.open(sqlFiles[i]);    		 
            			BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            			
            			while(true){
            				String line = bf.readLine();
            				if(line==null){
            					bf.close();
            					break;
            				}else if(line.trim().length() > 0){    					
            				//	Log.i("db",line);
            					try{
            						db.execSQL(line);	
            					}catch(SQLException e){
            					//Log.i("db","error executing SQL: "+line);	
            					}
            				}
            			}
            			bf.close();            			
            			if(mDBHelperListener != null){
            				mDBHelperListener.onCompleteDatabaseSQL(i, len);            				
            			}
            				
                	} 
            		catch (IOException e) {
        			}        	
            	}
            	if(mDBHelperListener != null){
    				mDBHelperListener.onFinishDatabaseReconstructionUsingSQLFiles();            				
    			}
            	db.close();
    			close();
    		} catch (SQLException e) { 
        		throw new Error("Error creating database, "+e.getMessage());
        	}    		
    	} 
    }
    
    
    /**
     * Do a DB reconstruction by parts. <br/> 
     * You have to split the db files manually and provide the array if id's of the raw parts if you include them as raw resources.
     * @param rawparts ids of the raw resources. 
     */
    public void createDataBase(int[] rawparts) throws IOException{
    	boolean dbExist = checkDataBase(); 
    	if(dbExist){
    	}else{ 
        	this.getReadableDatabase(); 
        	try {
        		this.mRawParts = rawparts.clone();
    			copyDataBaseByParts();
    			close();
    		} catch (IOException e) { 
        		throw new Error("Error copying database "+e.getMessage());
        		//TODO delete database file on error?
        	}    		
    	} 
    }
    
    /**
     * A helper method to dump the contents of a cursor onto the android log.
     * @param c is a cursor
     */
    public static void dumpCursor(Cursor c){
		
		int num = c.getCount();
		Logger.l(Logger.INFO,"info","num: "+num);
		while(c.moveToNext()){
			StringBuilder sb = new StringBuilder (10000); 
			android.database.DatabaseUtils.dumpCurrentRow(c,sb);
			Logger.l(Logger.INFO,"info",sb.toString());
		}
		c.close();
		
	}
        
    private boolean checkDataBase(){ 
    	SQLiteDatabase checkDB = null; 
    	try{
    		String myPath = getDataBasePath();
    		Logger.l(Logger.INFO,"db","openDB");
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS| SQLiteDatabase.OPEN_READONLY);
 
    	}catch(SQLiteException e){
    	}
 
    	if(checkDB != null){ 
    		checkDB.close(); 
    	} 
    	return checkDB != null ? true : false;
    }
     
   /**
    * Reconstruct a database using parts and save it to the local data directory. 
    * @throws IOException
    */
    private void copyDataBaseByParts() throws IOException{
    	OutputStream databaseOutputStream = new	FileOutputStream(getDataBasePath());
    	InputStream databaseInputStream;    
  
    	int numparts = mRawParts.length;
    	for(int i=0;i<numparts;i++){
    	  
    		byte[] buffer = new byte[1024];
    		try{
    			databaseInputStream = myContext.getResources().openRawResource(mRawParts[i]);
    			while ( (databaseInputStream.read(buffer)) > 0 ) {
    				databaseOutputStream.write(buffer);
    			}
    			databaseInputStream.close();
    		}catch(Resources.NotFoundException e){    	
    		
    		}
    		if(mDBHelperListener != null){
				mDBHelperListener.onCompleteDatabaseFragment(i,numparts);            				
			}
    	}
    	databaseOutputStream.flush();
    	databaseOutputStream.close();
    	if(mDBHelperListener != null){
			mDBHelperListener.onFinishDatabaseReconstructionUsingParts();            				
		}
    }
     
    @Override
	public synchronized void close() {
    	super.close();
	}
    
 	@Override
	public void onCreate(SQLiteDatabase db) {
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
 }