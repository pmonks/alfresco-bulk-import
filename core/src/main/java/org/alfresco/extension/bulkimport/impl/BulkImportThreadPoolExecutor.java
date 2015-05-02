/*
 * Copyright (C) 2007-2015 Peter Monks.
 *               2012      Alain Sahli - Fix for issue 109: http://code.google.com/p/alfresco-bulk-filesystem-import/issues/detail?id=109.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This file is part of an unsupported extension to Alfresco.
 * 
 */

package org.alfresco.extension.bulkimport.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.alfresco.extension.bulkimport.BulkImportLogUtils.*;


/**
 * This class provides a simplified <code>ThreadPoolExecutor</code> specific
 * that uses sensible defaults for the bulk import tool.  Note that calls to
 * <code>execute</code> and <code>submit</code> can block.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public class BulkImportThreadPoolExecutor
    extends ThreadPoolExecutor
    implements BlockingPausableExecutorService
{
    private final static Log log = LogFactory.getLog(BulkImportThreadPoolExecutor.class);
    
    private final static int      DEFAULT_THREAD_POOL_SIZE     = Runtime.getRuntime().availableProcessors() * 2;   // We naively assume 50+% of time is spent blocked on I/O
    private final static long     DEFAULT_KEEP_ALIVE_TIME      = 1L;
    private final static TimeUnit DEFAULT_KEEP_ALIVE_TIME_UNIT = TimeUnit.MINUTES;
    private final static int      DEFAULT_QUEUE_SIZE           = 100000;
    
    // For "exponential" (Fibonacci) back-off - will sleep this many milliseconds X 10 on each retry
    private final static int[]    FIBONACCI_NUMBERS            = new int[] { 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610 };
    private final static int      FIBONACCI_COUNT              = FIBONACCI_NUMBERS.length;
    
    
    public BulkImportThreadPoolExecutor()
    {
        this(DEFAULT_THREAD_POOL_SIZE, DEFAULT_QUEUE_SIZE, DEFAULT_KEEP_ALIVE_TIME, DEFAULT_KEEP_ALIVE_TIME_UNIT);
    }
    
    public BulkImportThreadPoolExecutor(final int threadPoolSize)
    {
        this(threadPoolSize, DEFAULT_QUEUE_SIZE, DEFAULT_KEEP_ALIVE_TIME, DEFAULT_KEEP_ALIVE_TIME_UNIT);
    }
    
    public BulkImportThreadPoolExecutor(final int      threadPoolSize,
                                        final int      queueSize,
                                        final long     keepAliveTime,
                                        final TimeUnit keepAliveTimeUnit)
    {
        super(threadPoolSize    <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize,           // Core pool size
              threadPoolSize    <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize,           // Max pool size (same as core pool size)
              keepAliveTime     <= 0    ? DEFAULT_KEEP_ALIVE_TIME      : keepAliveTime,            // Keep alive
              keepAliveTimeUnit == null ? DEFAULT_KEEP_ALIVE_TIME_UNIT : keepAliveTimeUnit,        // Keep alive units
              new ArrayBlockingQueue<Runnable>(queueSize <= 0 ? DEFAULT_QUEUE_SIZE : queueSize),   // Queue
              new BulkImportThreadFactory(),                                                       // Thread factory
              new ThreadPoolExecutor.AbortPolicy());                                               // Handler
              

        if (debug(log)) debug(log, "Creating new bulk import thread pool." +
                                   "\n\tthreadPoolSize = " + threadPoolSize +
                                   "\n\tqueueSize = " + queueSize +
                                   "\n\tkeepAliveTime = " + keepAliveTime + " " + String.valueOf(keepAliveTimeUnit));
    }
    
    
    /**
     * @see java.util.concurrent.ThreadPoolExecutor#execute(java.lang.Runnable)
     */
    @Override
    public void execute(final Runnable command)
    {
        // o.O
        retryableCallWithFibonacciBackoff(new Callable<Object>()
            {
                @Override
                public Object call()
                {
                    BulkImportThreadPoolExecutor.super.execute(command);
                    return(null);
                }
            });
    }

    /**
     * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable)
     */
    @Override
    public Future<?> submit(final Runnable task)
    {
        // o.O
        return((Future<?>)retryableCallWithFibonacciBackoff(new Callable<Object>()
            {
                @Override
                public Future<?> call()
                {
                    return(BulkImportThreadPoolExecutor.super.submit(task));
                }
            }));
    }
    

    /**
     * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable, java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(final Runnable task, final T result_)
    {
        // o.O
        return((Future<T>)retryableCallWithFibonacciBackoff(new Callable<Object>()
            {
                @Override
                public Future<T> call()
                {
                    return(BulkImportThreadPoolExecutor.super.submit(task, result_));
                }
            }));
    }

    
    /**
     * @see java.util.concurrent.AbstractExecutorService#submit(java.util.concurrent.Callable)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(final Callable<T> task)
    {
        // o.O
        return((Future<T>)retryableCallWithFibonacciBackoff(new Callable<Object>()
            {
                @Override
                public Future<T> call()
                {
                    return(BulkImportThreadPoolExecutor.super.submit(task));
                }
            }));
    }

    
    /**
     * @see org.alfresco.extension.bulkimport.impl.BlockingPausableExecutorService#pause()
     */
    @Override
    public void pause()
    {
        //####TODO: implement this!
        throw new UnsupportedOperationException("org.alfresco.extension.bulkimport.impl.BulkImportThreadPoolExecutor.pause() has not yet been implemented!");
    }

    
    /**
     * @see org.alfresco.extension.bulkimport.impl.BlockingPausableExecutorService#resume()
     */
    @Override
    public void resume()
    {
        //####TODO: implement this!
        throw new UnsupportedOperationException("org.alfresco.extension.bulkimport.impl.BulkImportThreadPoolExecutor.resume() has not yet been implemented!");
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.impl.BlockingPausableExecutorService#await()
     */
    @Override
    public void await()
        throws InterruptedException
    {
        awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);  // Wait forever (technically merely a very long time, but whatevs...)
    }
    
    
    // Good lord I miss Clojure...
    private Object retryableCallWithFibonacciBackoff(final Callable<Object> work)
    {
        Object  result     = null;
        boolean retrying   = true;
        int     retryCount = 0;
        
        while (retrying)
        {
            try
            {
                result   = work.call();
                retrying = false;
            }
            catch (final RejectedExecutionException ree)
            {
                // If we're shutting down, bail out
                if (isTerminating() || isShutdown())
                {
                    throw ree;
                }
                
                // Otherwise, queue is full so sleep before trying again
                fibonacciSleep(retryCount);
                retryCount++;
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);  // @#%& checked exceptions!!!!
            }
        }
        
        return(result);
    }

    
    private void fibonacciSleep(int fibonacciIndex)
    {
        try
        {
            if (fibonacciIndex >= FIBONACCI_COUNT)
            {
                fibonacciIndex = FIBONACCI_COUNT - 1;
            }
            
            // Add some random jitter to each sleep
            long sleepMillis = (long)(FIBONACCI_NUMBERS[fibonacciIndex] * 10.0 * (Math.random() + 0.5));
            
            if (debug(log)) debug(log, "Queue is full (remaining capacity = " + getQueue().remainingCapacity() + ") - sleeping for " + sleepMillis + "ms before retrying.");
            Thread.sleep(sleepMillis);
        }
        catch (final InterruptedException ie)
        {
            // Swallow and move on
            if (debug(log)) debug(log, "Interrupted during retry sleep.", ie);
        }
    }
    
}
