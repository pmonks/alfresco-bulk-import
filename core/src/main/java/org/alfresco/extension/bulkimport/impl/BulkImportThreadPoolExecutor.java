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
{
    private final static Log log = LogFactory.getLog(BulkImportThreadPoolExecutor.class);
    
    private final static int      DEFAULT_THREAD_POOL_SIZE     = Runtime.getRuntime().availableProcessors() * 2;   // We naively assume 50% of time is spent blocked on I/O
    private final static long     DEFAULT_KEEP_ALIVE_TIME      = 1L;
    private final static TimeUnit DEFAULT_KEEP_ALIVE_TIME_UNIT = TimeUnit.MINUTES;
    private final static int      DEFAULT_QUEUE_SIZE           = 10000;
    
    
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
        super(threadPoolSize    <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize,                // Core pool size
              threadPoolSize    <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize,                // Max pool size (same as core pool size)
              keepAliveTime     <= 0    ? DEFAULT_KEEP_ALIVE_TIME      : keepAliveTime,                 // Keep alive
              keepAliveTimeUnit == null ? DEFAULT_KEEP_ALIVE_TIME_UNIT : keepAliveTimeUnit,             // Keep alive units
              new ArrayBlockingQueue<Runnable>(queueSize <= 0 ? DEFAULT_QUEUE_SIZE : queueSize, true),  // Queue, with fairness enabled (to get true FIFO, thereby minimising out-of-order retries)
              new BulkImportThreadFactory(),                                                            // Thread factory
              new ThreadPoolExecutor.AbortPolicy());                                                    // Rejection handler
              
        if (debug(log)) debug(log, "Creating new bulk import thread pool." +
                                   " Thread Pool Size=" + (threadPoolSize    <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize) +
                                   ", Queue Size=" +      (queueSize         <= 0    ? DEFAULT_QUEUE_SIZE           : queueSize) +
                                   ", Keep Alive Time=" + (keepAliveTime     <= 0    ? DEFAULT_KEEP_ALIVE_TIME      : keepAliveTime) +
                                   " " +    String.valueOf(keepAliveTimeUnit == null ? DEFAULT_KEEP_ALIVE_TIME_UNIT : keepAliveTimeUnit));
    }
    
    
    /**
     * @return The current size of the queue.
     */
    public int queueSize()
    {
        return(getQueue().size());
    }
    
    
    /**
     * @return Is the work queue for this thread pool empty?
     */
    public boolean isQueueEmpty()
    {
        return(getQueue().isEmpty());
    }

    
    /**
     * Pauses execution of the pool (NOT YET IMPLEMENTED!).
     */
    public void pause()
    {
        //####TODO: implement this!
        throw new UnsupportedOperationException("org.alfresco.extension.bulkimport.impl.BulkImportThreadPoolExecutor.pause() has not yet been implemented!");
    }

    
    /**
     * Resumes execution of a previously paused pool (NOT YET IMPLEMENTED!).
     */
    public void resume()
    {
        //####TODO: implement this!
        throw new UnsupportedOperationException("org.alfresco.extension.bulkimport.impl.BulkImportThreadPoolExecutor.resume() has not yet been implemented!");
    }
    
    
    /**
     * Indefinitely awaits termination of the thread pool.
     */
    public void await()
        throws InterruptedException
    {
        awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);  // Wait forever (technically merely a very long time, but whatevs...)
    }
    
}
