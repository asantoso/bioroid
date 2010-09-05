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

public abstract class AdvancedUserTask<Params, Progress, Result> extends UserTask<Params, Progress, Result>{
    private static final String LOG_TAG = "AdvancedUserTask";
    
}
