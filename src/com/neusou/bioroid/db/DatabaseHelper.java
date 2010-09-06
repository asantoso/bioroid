package com.neusou.bioroid.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.neusou.Logger;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 
 * DBHelper is an abstract database helper
 *  
 */
public abstract class DatabaseHelper extends SQLiteOpenHelper{
 
    protected final Context myContext;        
    protected int[] mRawParts;
    protected int bufferSize = 4096;

    private AssetManager mAssetManager;
    private String mDBName;     
    private static final String LOG_TAG = "DBHelper";
    
   
    public DatabaseHelper(Context context, String dbName, int version) { 
    	super(context, dbName, null, version);
    	this.mDBName = dbName;
        this.myContext = context;
        this.mAssetManager = context.getAssets();
    }	
   
    public void recreateDatabase(String[] sqlFiles) throws IOException {
    	Logger.l(Logger.DEBUG, LOG_TAG, "[recreateDatabase]");
    	boolean dbExist = checkDataBase(); 
    	if(dbExist){
    		File dbFile = myContext.getDatabasePath(mDBName);
    		boolean successdelete = dbFile.delete();
    		Logger.l(Logger.DEBUG, LOG_TAG, "[recreateDatabase] delete db file "+(successdelete?"sucessful":"unsuccessful"));
    		
    	}
    	createDataBase(sqlFiles);
    }
        
    /**
     * Performs a DB reconstruction by reading text files containing non-query sql statements. 
     * @param sqlFiles name of the sql files as path in the assets.
     */
    public void createDataBase(String... sqlFiles) throws IOException{    	
    	boolean dbExist = checkDataBase(); 
    	if(dbExist){
    	}
    	else{
        	try {
        		SQLiteDatabase db = this.getReadableDatabase();	
        		int len = sqlFiles.length;
            	for(int i=0;i<len;i++){
            		try{    			
            			Log.d("info","opening file: "+sqlFiles[i]);
            			InputStream is = mAssetManager.open(sqlFiles[i]);    		 
            			BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            			
            			while(true){
            				String line = bf.readLine();
            				if(line==null){
            					bf.close();
            					break;
            				}else if(line.trim().length() > 0){    					
            					Log.d("db",line);
            					try{
            						db.execSQL(line);	
            					}catch(SQLException e){
            					Log.d("db","error executing SQL: "+line);	
            					}
            				}
            			}
            			bf.close(); 
            				
                	} 
            		catch (IOException e) {
        			}        	
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
    public void createDataBase(int... rawparts) throws IOException{
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
        	}    		
    	} 
    }
        
    private boolean checkDataBase(){ 
    	SQLiteDatabase checkDB = null; 
    	String myDbPath = null;
    	try{    		
    		myDbPath = myContext.getDatabasePath(mDBName).getAbsolutePath();
    		checkDB = SQLiteDatabase.openDatabase(myDbPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS| SQLiteDatabase.OPEN_READWRITE); 
    	}catch(SQLiteException e){    		    		
    		File f = new File(myDbPath);
    		if(f.canRead()){
    			boolean isDBFileDeleted = f.delete();
    			if(isDBFileDeleted){
    				Log.d("db","sucessfully deleted db file "+myDbPath);
    			}
    		}
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
    	
    	Log.d(LOG_TAG, "copyDataBaseByParts");
    	
    	OutputStream databaseOutputStream = new	FileOutputStream(myContext.getDatabasePath(mDBName));
    	InputStream databaseInputStream;
  
    	int numparts = mRawParts.length;
    	
    	for(int i=0;i<numparts;i++){
    	  
    		byte[] buffer = new byte[bufferSize];
    		try{
    			databaseInputStream = myContext.getResources().openRawResource(mRawParts[i]);
    			while ( (databaseInputStream.read(buffer)) > 0 ) {
    				databaseOutputStream.write(buffer);
    			}
    			databaseInputStream.close();
    		}catch(Resources.NotFoundException e){    	
    		
    		}
    		
    	}
    	databaseOutputStream.flush();
    	databaseOutputStream.close();
		
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