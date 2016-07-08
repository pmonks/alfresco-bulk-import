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

import java.util.concurrent.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;


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
    private final static long     DEFAULT_KEEP_ALIVE_TIME      = 10L;
    private final static TimeUnit DEFAULT_KEEP_ALIVE_TIME_UNIT = TimeUnit.MINUTES;
    private final static int      DEFAULT_QUEUE_SIZE           = 100;

    private final Semaphore semaphore;
    
    public BulkImportThreadPoolExecutor(final int      threadPoolSize,
                                        final int      queueSize,
                                        final long     keepAliveTime,
                                        final TimeUnit keepAliveTimeUnit)
    {
        super(threadPoolSize                              <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize,      // Core pool size
              threadPoolSize                              <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize,      // Max pool size (same as core pool size)
              keepAliveTime                               <= 0    ? DEFAULT_KEEP_ALIVE_TIME      : keepAliveTime,       // Keep alive
              keepAliveTimeUnit                           == null ? DEFAULT_KEEP_ALIVE_TIME_UNIT : keepAliveTimeUnit,   // Keep alive units
              new LinkedBlockingQueue<Runnable>((queueSize <= 0 ? DEFAULT_QUEUE_SIZE : queueSize)),                     // Queue (with pre-allocated size)
              new BulkImportThreadFactory(),                                                                            // Thread factory
              new ThreadPoolExecutor.AbortPolicy());                                                                    // Rejection handler

        final int queuePlusPoolSize = (queueSize      <= 0 ? DEFAULT_QUEUE_SIZE       : queueSize) +
                                      (threadPoolSize <= 0 ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize);
        this.semaphore = new Semaphore(queuePlusPoolSize);

        if (debug(log)) debug(log, "Created new bulk import thread pool." +
                                   " Thread Pool Size="        + (threadPoolSize    <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize) +
                                   ", Queue Size="             + (queueSize         <= 0    ? DEFAULT_QUEUE_SIZE           : queueSize) +
                                   ", Keep Alive Time="        + (keepAliveTime     <= 0    ? DEFAULT_KEEP_ALIVE_TIME      : keepAliveTime) +
                                   " "           + String.valueOf(keepAliveTimeUnit == null ? DEFAULT_KEEP_ALIVE_TIME_UNIT : keepAliveTimeUnit));
    }


    /**
     * Schedule the given command to run on the thread pool.  Note: will block if the queue is full.
     *
     * @param command The Runnable to schedule.
     */
    @Override
    public void execute(final Runnable command)
    {
        try
        {
            semaphore.acquire();
        }
        catch (final InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);   // Checked exceptions are the bane of my existence...
        }

        try
        {
            super.execute(command);
        }
        finally
        {
            semaphore.release();
        }
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
     * Indefinitely awaits termination of the thread pool.
     */
    public void await()
        throws InterruptedException
    {
        awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);  // Wait forever (technically merely a very long time, but whatevs...)
    }
    
}
