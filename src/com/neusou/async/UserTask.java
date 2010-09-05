package com.neusou.async;

import android.os.*;
import android.os.Process;

import java.util.ArrayList;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import com.neusou.Logger;
import com.neusou.Utility;

/**
 * <p>UserTask enables proper and easy use of the UI thread. This class allows to
 * perform background operations and publish results on the UI thread without
 * having to manipulate threads and/or handlers.</p>
 *
 * <p>A user task is defined by a computation that runs on a background thread and
 * whose result is published on the UI thread. A user task is defined by 3 generic
 * types, called <code>Params</code>, <code>Progress</code> and <code>Result</code>,
 * and 4 steps, called <code>begin</code>, <code>doInBackground</code>,
 * <code>processProgress<code> and <code>end</code>.</p>
 *
 * <h2>Usage</h2>
 * <p>UserTask must be subclassed to be used. The subclass will override at least
 * one method ({@link #doInBackground(Object[])}), and most often will override a
 * second one ({@link #onPostExecute(Object)}.)</p>
 *
 * <p>Here is an example of subclassing:</p>
 * <pre>
 * private class DownloadFilesTask extends UserTask&lt;URL, Integer, Long&gt; {
 *     public File doInBackground(URL... urls) {
 *         int count = urls.length;
 *         long totalSize = 0;
 *         for (int i = 0; i < count; i++) {
 *             totalSize += Downloader.downloadFile(urls[i]);
 *             publishProgress((int) ((i / (float) count) * 100));
 *         }
 *     }
 *
 *     public void onProgressUpdate(Integer... progress) {
 *         setProgressPercent(progress[0]);
 *     }
 *
 *     public void onPostExecute(Long result) {
 *         showDialog("Downloaded " + result + " bytes");
 *     }
 * }
 * </pre>
 *
 * <p>Once created, a task is executed very simply:</p>
 * <pre>
 * new DownloadFilesTask().execute(new URL[] { ... });
 * </pre>
 *
 * <h2>User task's generic types</h2>
 * <p>The three types used by a user task are the following:</p>
 * <ol>
 *     <li><code>Params</code>, the type of the parameters sent to the task upon
 *     execution.</li>
 *     <li><code>Progress</code>, the type of the progress units published during
 *     the background computation.</li>
 *     <li><code>Result</code>, the type of the result of the background
 *     computation.</li>
 * </ol>
 * <p>Not all types are always used by a user task. To mark a type as unused,
 * simply use the type {@link Void}:</p>
 * <pre>
 * private class MyTask extends UserTask<Void, Void, Void) { ... }
 * </pre>
 *
 * <h2>The 4 steps</h2>
 * <p>When a user task is executed, the task goes through 4 steps:</p>
 * <ol>
 *     <li>{@link #onPreExecute()}, invoked on the UI thread immediately after the task
 *     is executed. This step is normally used to setup the task, for instance by
 *     showing a progress bar in the user interface.</li>
 *     <li>{@link #doInBackground(Object[])}, invoked on the background thread
 *     immediately after {@link # onPreExecute ()} finishes executing. This step is used
 *     to perform background computation that can take a long time. The parameters
 *     of the user task are passed to this step. The result of the computation must
 *     be returned by this step and will be passed back to the last step. This step
 *     can also use {@link #publishProgress(Object[])} to publish one or more units
 *     of progress. These values are published on the UI thread, in the
 *     {@link #onProgressUpdate(Object[])} step.</li>
 *     <li>{@link # onProgressUpdate (Object[])}, invoked on the UI thread after a
 *     call to {@link #publishProgress(Object[])}. The timing of the execution is
 *     undefined. This method is used to display any form of progress in the user
 *     interface while the background computation is still executing. For instance,
 *     it can be used to animate a progress bar or show logs in a text field.</li>
 *     <li>{@link # onPostExecute (Object)}, invoked on the UI thread after the background
 *     computation finishes. The result of the background computation is passed to
 *     this step as a parameter.</li>
 * </ol>
 *
 * <h2>Threading rules</h2>
 * <p>There are a few threading rules that must be followed for this class to
 * work properly:</p>
 * <ul>
 *     <li>The task instance must be created on the UI thread.</li>
 *     <li>{@link #execute(Object[])} must be invoked on the UI thread.</li>
 *     <li>Do not call {@link # onPreExecute ()}, {@link # onPostExecute (Object)},
 *     {@link #doInBackground(Object[])}, {@link # onProgressUpdate (Object[])}
 *     manually.</li>
 *     <li>The task can be executed only once (an exception will be thrown if
 *     a second execution is attempted.)</li>
 * </ul>
 */

public abstract class UserTask<Params, Progress, Result> {
    protected static final String LOG_TAG = "UserTask";
    
    protected static int CORE_POOL_SIZE = 1;
    protected static int MAXIMUM_POOL_SIZE = 10;
    protected static int KEEP_ALIVE = 5;//careful when setting this value. since it is coupled with SystemClock.wait.
    protected static TimeUnit KEEP_ALIVE_UNIT = TimeUnit.SECONDS;

   
    public enum Mode{
    	ONLINE,
    	OFFLINE
    }
    
    protected Mode mMode;
    
    public void setMode(Mode mode){
    	mMode = mode;
    }
    
    public Mode getMode(){
    	return mMode;
    }    
    
    public void setListener(IUserTaskListener<Progress, Result> listener){
    	mUserTaskListener = listener;
    }
   
    // = new ThreadPoolExecutor(CORE_POOL_SIZE,
    //        MAXIMUM_POOL_SIZE, KEEP_ALIVE, KEEP_ALIVE_UNIT, sWorkQueue, sThreadFactory);

    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;
    private static final int MESSAGE_POST_CANCEL = 0x3;
    private static final int MESSAGE_POST_TIMEOUT = 0x4;

    private static final InternalHandler sHandler = new InternalHandler();

    private WorkerRunnable<Params, Result> mWorker;
    private FutureTask<Result> mFuture;

    protected volatile Status mStatus = Status.PENDING;
        
    protected IUserTaskListener<Progress, Result> mUserTaskListener;
    
    private long mTimeoutMilliseconds = 1000;
    
    /**
     * Set timeout in seconds
     * @param seconds
     */
    public void setTimeout(long milliseconds){
    	mTimeoutMilliseconds = milliseconds;
    }
    
    /**
     * Indicates the current status of the task. Each status will be set only once
     * during the lifetime of a task.
     */
    public enum Status {
        /**
         * Indicates that the task has not been executed yet.
         */
        PENDING,
        /**
         * Indicates that the task is running.
         */
        RUNNING,
        /**
         * Indicates that {@link UserTask#onPostExecute(Object)} has finished.
         */
        FINISHED,
        /**
         * Indicates that the task is canceled.
         */        
        CANCELLED,
        
        /**
         * Indicates that the task is timed out
         */
        TIMEDOUT
    }

   
    UserTaskExecutionScope mExecutionScope;
    
    static UserTaskExecutionScope mDefaultExecutionScope = 
    	new UserTaskExecutionScope(LOG_TAG,MAXIMUM_POOL_SIZE,CORE_POOL_SIZE,KEEP_ALIVE,KEEP_ALIVE_UNIT, 10);
    
    /**
     * Only call this once before any instance of this class is created.
     * @param scope
     */
    public static void setDefaultExecutionScope(UserTaskExecutionScope scope){
    	mDefaultExecutionScope = scope;
    }
    
    public UserTask(){
    	init(mDefaultExecutionScope);
    }
    
    /**
     * Creates a new user task. This constructor must be invoked on the UI thread.
     */
    public UserTask(UserTaskExecutionScope scope) {
    	init(scope);
    }
    
    private void init(final UserTaskExecutionScope scope){
    	mExecutionScope = scope;
    	
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                return doInBackground(mParams);
            }
        };
                
        mFuture = new FutureTask<Result>(mWorker) {
            	
            @Override
            protected void done() {
                Message message;
                Result result = null;

                try {
                    result = get();//mTimeoutMilliseconds,TimeUnit.MILLISECONDS);
                } /*catch(TimeoutException e){
                	e.printStackTrace();
                	message = sHandler.obtainMessage(MESSAGE_POST_TIMEOUT, null);
                	message.sendToTarget();
                	return;
                }*/ catch (InterruptedException e) {
                  
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occured while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    message = sHandler.obtainMessage(MESSAGE_POST_CANCEL,
                            new UserTaskResult<Result>(UserTask.this, (Result[]) null));
                    message.sendToTarget();
                    return;
                } catch (Throwable t) {
                    throw new RuntimeException("An error occured while executing doInBackground()", t);
                }

                message = sHandler.obtainMessage(MESSAGE_POST_RESULT, new UserTaskResult<Result>(UserTask.this, result));
                message.sendToTarget();                
                String threadName = Thread.currentThread().getName();
                
                ThreadPoolExecutor executor = scope.sExecutor;
                //BlockingQueue<Runnable> pendingQueue = scope.sPendingQueue;
                Stack<Runnable> pendingQueue = scope.sPendingQueue;
                
                
                int poolSize = executor.getPoolSize();
                int corePoolSize = executor.getCorePoolSize();
                long taskCompleteCount = executor.getCompletedTaskCount();
                long taskCount = executor.getTaskCount();
                int maxPoolSize = executor.getMaximumPoolSize();
                int largestPoolSize = executor.getLargestPoolSize();
                
                //TODO Log comment out
                /*
                Logger.l(Logger.DEBUG, scope.mName, "[Thread:"+threadName+"] [FutureTask][done()] corePoolSize:"+corePoolSize+", poolSize: "+poolSize+", taskCompleted: "+taskCompleteCount+", task: "+taskCount+
                		", maxPoolSize:"+maxPoolSize+
                		", largestPoolSize:"+largestPoolSize +
                		", #pending task:"+pendingQueue.size()
                		
                );
                */
                
                try{        
                	executor.execute(pendingQueue.pop());
                	//TODO Log comment out
                	//Logger.l(Logger.DEBUG, threadName,"[Thread:"+threadName+"] [FutureTask][done()] adding one pending task to be execution queue. done.");
                }catch(Exception e){
                	//Logger.l(Logger.DEBUG, LOG_TAG, threadName+" [FutureTask][done()] transferring error: "+e.getMessage());
                }
            }
            
        };
        
    }

    /**
     * Returns the current status of this task.
     *
     * @return The current status.
     */
    public final Status getStatus() {
        return mStatus;
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute(Object[])}
     * by the caller of this task.
     *
     * This method can call {@link #publishProgress(Object[])} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     *
     * @return A result, defined by the subclass of this task.
     *
     * @see #onPreExecute()
     * @see #onPostExecute(Object)
     * @see #publishProgress(Object[])
     */
    protected abstract Result doInBackground(Params... params);

    /**
     * Runs on the UI thread before {@link #doInBackground(Object[])}.
     *
     * @see #onPostExecute(Object)
     * @see #doInBackground(Object[])
     */
    protected void onPreExecute() {
    	
    	
    }
    
    /**
     * Runs on the UI thread when timeout occurs
     *    
     */
    protected void onTimeout() {
    	
    	
    }

    /**
     * Runs on the UI thread after {@link #doInBackground(Object[])}. The
     * specified result is the value returned by {@link #doInBackground(Object[])}
     * or null if the task was cancelled or an exception occured.
     *
     * @param result The result of the operation computed by {@link #doInBackground(Object[])}.
     *
     * @see #onPreExecute()
     * @see #doInBackground(Object[])
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onPostExecute(Result result) {
    	
    }

    /**
     * Runs on the UI thread after {@link #publishProgress(Object[])} is invoked.
     * The specified values are the values passed to {@link #publishProgress(Object[])}.
     *
     * @param values The values indicating progress.
     *
     * @see #publishProgress(Object[])
     * @see #doInBackground(Object[])
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onProgressUpdate(Progress... values) {
    	
    }

    /**
     * Runs on the UI thread after {@link #cancel(boolean)} is invoked.
     *
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    protected void onCancelled() {
    	
    
    }

    /**
     * Returns <tt>true</tt> if this task was cancelled before it completed
     * normally.
     *
     * @return <tt>true</tt> if task was cancelled before it completed
     *
     * @see #cancel(boolean)
     */
    public final boolean isCancelled() {
        return mFuture.isCancelled();
    }

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when <tt>cancel</tt> is called,
     * this task should never run.  If the task has already started,
     * then the <tt>mayInterruptIfRunning</tt> parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this
     *        task should be interrupted; otherwise, in-progress tasks are allowed
     *        to complete.
     *
     * @return <tt>false</tt> if the task could not be cancelled,
     *         typically because it has already completed normally;
     *         <tt>true</tt> otherwise
     *
     * @see #isCancelled()
     * @see #onCancelled()
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
    	mStatus = Status.CANCELLED;
    	cleanUp();
        return mFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return The computed result.
     *
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException If the computation threw an exception.
     * @throws InterruptedException If the current thread was interrupted
     *         while waiting.
     */
    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result.
     *
     * @param timeout Time to wait before cancelling the operation.
     * @param unit The time unit for the timeout.
     *
     * @return The computed result.
     *
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException If the computation threw an exception.
     * @throws InterruptedException If the current thread was interrupted
     *         while waiting.
     * @throws TimeoutException If the wait timed out.
     */
    public final Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }


    		
    /**
     * Executes the task with the specified parameters. The task returns
     * itself (this) so that the caller can keep a reference to it.
     *
     * This method must be invoked on the UI thread.
     *
     * @param params The parameters of the task.
     *
     * @return This instance of UserTask.
     *
     * @throws IllegalStateException If {@link #getStatus()} returns either
     *         {@link UserTask.Status#RUNNING} or {@link UserTask.Status#FINISHED}.
     */
    public final UserTask<Params, Progress, Result> execute(Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();
        
        if(mUserTaskListener != null){
    		mUserTaskListener.onPreExecute();
    	}

        mWorker.mParams = params;
                
        /**
         * TODO [Dev.Note] NOTE: THIS RUNS ON UI THREAD
         * The implication of this is that the rejected handler of the executor would have to return immediately. 
         */
        try{
        	mExecutionScope.sExecutor.execute(mFuture);
        }catch(RejectedExecutionException e){
        	return null;
        }catch(OutOfMemoryError e){
        	return null;
        }
        
        return this;
    }

    /**
     * This method can be invoked from {@link #doInBackground(Object[])} to
     * publish updates on the UI thread while the background computation is
     * still running. Each call to this method will trigger the execution of
     * {@link #onProgressUpdate(Object[])} on the UI thread.
     *
     * @param values The progress values to update the UI with.
     *
     * @see # onProgressUpdate (Object[])
     * @see #doInBackground(Object[])
     */
    protected final void publishProgress(Progress... values) {
        sHandler.obtainMessage(MESSAGE_POST_PROGRESS,
                new UserTaskResult<Progress>(this, values)).sendToTarget();
    }

    private void finish(Result result) {
    	cleanUp();
    	mStatus = Status.FINISHED;
    	onPostExecute(result);    	
    }

    private void cleanUp(){
    	mExecutionScope = null;
    }
    
    private static class InternalHandler extends Handler {
        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        @Override
        public void handleMessage(Message msg) {
            UserTaskResult result = (UserTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    if(result.mTask.mUserTaskListener != null){
                    	result.mTask.mUserTaskListener.onPostExecute(result.mData[0]);
                	}    
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    if(result.mTask.mUserTaskListener != null){
                		result.mTask.mUserTaskListener.onProgressUpdate(result.mData);
                	}
                    break;
                case MESSAGE_POST_CANCEL:{
                    result.mTask.onCancelled();
                    if(result.mTask.mUserTaskListener != null){
                		result.mTask.mUserTaskListener.onCancelled();
                	}
                    break;
                }
                case MESSAGE_POST_TIMEOUT:{
                    result.mTask.onTimeout();
                    if(result.mTask.mUserTaskListener != null){
                		result.mTask.mUserTaskListener.onTimeout();
                	}
                    break;
                }
            }
        }
    }

    private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] mParams;
    }

    @SuppressWarnings({"RawUseOfParameterizedType"})
    private static class UserTaskResult<Data> {
        final UserTask mTask;
        final Data[] mData;

        UserTaskResult(UserTask task, Data... data) {
            mTask = task;
            mData = data;            
        }
    }
}
