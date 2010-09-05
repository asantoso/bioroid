package com.neusou.database;

import com.neusou.Logger;

import android.os.AsyncTask;


/**
 * Background task that initializes database
 * @author asantoso
 * @since July 20, 2009
 */

public class InitializeDatabaseTask extends AsyncTask<InitializeDatabaseMethod.Param, Float, InitializeDatabaseMethod.Result> {		
		
		private DBHelper.DBHelperListener mListener;		
		private float total = 0;
		private float progress = 0;
		
		public InitializeDatabaseTask(){		
			
			mListener = new DBHelper.DBHelperListener(){
				@Override
				public void onCompleteDatabaseFragment(int currentFragment,
						int totalFragments) {
					progress++;
					float ratio = progress/total;
					publishProgress(ratio);					
				}

				@Override
				public void onCompleteDatabaseSQL(int currentSQLFile,
						int totalSQLFiles) {
					progress++;
					float ratio = progress/total;
					publishProgress(ratio);	
				}

				@Override
				public void onFinishDatabaseReconstructionUsingParts() {
									
				}

				@Override
				public void onFinishDatabaseReconstructionUsingSQLFiles() {
					
				}			
			};			
		}		
		
		@Override
		public InitializeDatabaseMethod.Result doInBackground(
				InitializeDatabaseMethod.Param... params) {		
			Logger.l(Logger.INFO,"info","initdb : do in background");
			
				InitializeDatabaseMethod m = InitializeDatabaseMethod.getInstance();
				
				//get total
				for(int i=0;i<params.length;i++){
					if(params[i].sqlFiles != null){
						total += params[i].sqlFiles.length;
					}
					if(params[i].sqlParts != null){
						total += params[i].sqlParts.length;
					}
				}
				
				for(int i=0;i<params.length;i++){
					params[i].dbHelper.setListener(mListener);
					if(params[i] != null){
						m.call(params[i]);	
					}					
				}
			
			return new InitializeDatabaseMethod.Result();
			
		}
		
}

