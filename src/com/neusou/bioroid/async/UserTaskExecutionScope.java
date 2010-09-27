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

package com.neusou.bioroid.async;

import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class UserTaskExecutionScope {
    
	public BlockingQueue<Runnable> sWorkQueue;
	//public BlockingQueue<Runnable> sPendingQueue;
	public Stack<Runnable> sPendingQueue;
	
    public ThreadFactory sThreadFactory;
    public ThreadPoolExecutor sExecutor;
    
    static int MAXIMUM_POOL_SIZE = 1;
    static String LOG_TAG = "UserTaskExecutionScope";
    
    int mMaxPoolSize;
    int mCorePoolSize;
    TimeUnit mKeepAliveUnit;
    long mKeepAlive;
    int mPendingQueueSize;
    
    String mName;
    
    int mPendingQueueCount;
    
    RejectedExecutionHandler mRejectedHandler =  new RejectedExecutionHandler() {			
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			boolean isTerminating = executor.isTerminating();
			
			if(!isTerminating){				
				//TODO remove
				String threadName = Thread.currentThread().getName();
				//try{				
				
                	
					//boolean isAddedToQueue = sPendingQueue.offer(r, 1, TimeUnit.NANOSECONDS);
					boolean isAddedToQueue = true;
					int size = sPendingQueue.size();
					if(size < mPendingQueueSize){							
					}else{
						sPendingQueue.remove(size-1);
					}					
					sPendingQueue.push(r);
					
					if(isAddedToQueue){
						//TODO Log comment out
	//					Logger.l(Logger.WARN, mName, "[Thread:"+threadName+"] [rejectedExecution()] task is successfully added to pending task queue. ");	
					}else{
						//TODO Log comment out
		//				Logger.l(Logger.ERROR,  mName, "[Thread:"+threadName+"] [rejectedExecution()] task can not be added to pending task queue due to timeout. ");	
					}
				//}catch(InterruptedException e){
					//TODO Log comment out
				//	Logger.l(Logger.ERROR, LOG_TAG, "[Thread:"+threadName+"] [rejectedExecution()] task can not be added to pending task queue. was interrupted while waiting. ");
			//	}
			}
		}
    };
    
    public void cleanUp(){
    	sExecutor.shutdown();    	
    	sPendingQueue.clear();
    }
    
    public void clearQueues(){
    	sWorkQueue.clear();
    	sPendingQueue.clear();
    }
        
    public UserTaskExecutionScope(String name, int maxPoolSize, int corePoolSize, long keepAlive, TimeUnit keepAliveUnit, int pendingQueueSize) {
    	mMaxPoolSize = maxPoolSize;
    	mCorePoolSize = corePoolSize;
    	mKeepAlive = keepAlive;
    	mKeepAliveUnit = keepAliveUnit;
    	mPendingQueueSize = pendingQueueSize>0?pendingQueueSize:1;
    	mName = name;
    	init();
	}
    
    void init(){
    	if(sWorkQueue == null){
    		sWorkQueue = new LinkedBlockingQueue<Runnable>(MAXIMUM_POOL_SIZE);    		
    	}
    	
    	if(sPendingQueue == null){
    		//sPendingQueue = new LinkedBlockingQueue<Runnable>(mPendingQueueSize);
    		sPendingQueue = new Stack<Runnable>();
    	}
    	

    	if(sThreadFactory == null){
    		sThreadFactory = new ThreadFactory() {
    	        private final AtomicInteger mCount = new AtomicInteger(1);
    	        StringBuffer sb = new StringBuffer();    	        
    	        public Thread newThread(Runnable r) {
    	        	        	
    	        	sb.delete(0, sb.length());
    	        	sb
    	        	.append("[")
    	        	.append(mName)
    	        	.append(" ")
    	        	.append("#")
    	        	.append(mCount.getAndIncrement())
    	        	.append("]");
    	        	
    	            return new Thread(r,sb.toString());
    	        }
    	    };
    	}
    	    
    	if(sExecutor == null){
    		sExecutor = new ThreadPoolExecutor(mCorePoolSize, mMaxPoolSize, mKeepAlive,mKeepAliveUnit, sWorkQueue, sThreadFactory);
    		sExecutor.setRejectedExecutionHandler(mRejectedHandler);
    	}
    }
    
	
}