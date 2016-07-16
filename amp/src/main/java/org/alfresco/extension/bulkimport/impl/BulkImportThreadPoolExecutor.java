/*
 * Copyright (C) 2007-2016 Peter Monks.
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
    
    private final static int      DEFAULT_THREAD_POOL_SIZE     = Runtime.getRuntime().availableProcessors() * 4;   // We naively assume 75% of time is spent blocked on I/O
    private final static long     DEFAULT_KEEP_ALIVE_TIME      = 10L;
    private final static TimeUnit DEFAULT_KEEP_ALIVE_TIME_UNIT = TimeUnit.MINUTES;
    private final static int      DEFAULT_QUEUE_SIZE           = 100;

    private final Semaphore queueSemaphore;

    private volatile boolean       paused;
    private final    ReentrantLock pauseLock;
    private final    Condition     pauseCondition;


    public BulkImportThreadPoolExecutor(final int      threadPoolSize,
                                        final int      queueSize,
                                        final long     keepAliveTime,
                                        final TimeUnit keepAliveTimeUnit)
    {
        super(threadPoolSize                       <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize,      // Core pool size
              threadPoolSize                       <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize,      // Max pool size (same as core pool size)
              keepAliveTime                        <= 0    ? DEFAULT_KEEP_ALIVE_TIME      : keepAliveTime,       // Keep alive
              keepAliveTimeUnit                    == null ? DEFAULT_KEEP_ALIVE_TIME_UNIT : keepAliveTimeUnit,   // Keep alive units
              new LinkedBlockingQueue<Runnable>(),                                                               // Queue of maximum size
              new BulkImportThreadFactory(),                                                                     // Thread factory
              new ThreadPoolExecutor.AbortPolicy());                                                             // Rejection handler (shouldn't ever be called)

        final int queuePlusPoolSize = (queueSize      <= 0 ? DEFAULT_QUEUE_SIZE       : queueSize) +
                                      (threadPoolSize <= 0 ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize);
        this.queueSemaphore = new Semaphore(queuePlusPoolSize);

        this.paused         = false;
        this.pauseLock      = new ReentrantLock();
        this.pauseCondition = pauseLock.newCondition();

        if (debug(log)) debug(log, "Created new bulk import thread pool." +
                                   " Thread Pool Size="        + (threadPoolSize    <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize) +
                                   ", Queue Size="             + ((queueSize        <= 0    ? DEFAULT_QUEUE_SIZE           : queueSize) + 2) +
                                   ", Keep Alive Time="        + (keepAliveTime     <= 0    ? DEFAULT_KEEP_ALIVE_TIME      : keepAliveTime)  +
                                   " "                         + String.valueOf(keepAliveTimeUnit == null ? DEFAULT_KEEP_ALIVE_TIME_UNIT : keepAliveTimeUnit));
    }


    /**
     * @see {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)}
     */
    @Override
    protected void beforeExecute(Thread thread, Runnable runnable)
    {
        super.beforeExecute(thread, runnable);

        if (paused)
        {
            pauseLock.lock();

            try
            {
                while (paused) pauseCondition.await();
            }
            catch (InterruptedException ie)
            {
                thread.interrupt();
            }
            finally
            {
                pauseLock.unlock();
            }
        }
    }


    /**
     * @see {@link ThreadPoolExecutor#execute(Runnable)}
     */
    @Override
    public void execute(final Runnable command)
    {
        try
        {
            if (debug(log) && queueSemaphore.availablePermits() <= 0) debug(log, "Worker threads are saturated, scanning will block.");

            queueSemaphore.acquire();
        }
        catch (final InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);   // Checked exceptions are the bane of my existence...
        }

        try
        {
            if (super.isTerminating() || super.isShutdown() || super.isTerminated())
            {
                if (debug(log)) debug(log, "New work submitted during shutdown - ignoring new work.");
            }
            else
            {
                super.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            command.run();
                        }
                        finally
                        {
                            // Note: queueSemaphore must be released by the worker thread, not the scanner thread!
                            queueSemaphore.release();
                        }
                    }
                });
            }
        }
        catch (final RejectedExecutionException ree)
        {
            // If this triggers, it's a bug in the back-pressure logic
            queueSemaphore.release();
            throw new IllegalStateException("Worker threads were saturated (available permits = " + String.valueOf(queueSemaphore.availablePermits()) + "), " +
                                            "but scanning didn't block, resulting in a RejectedExecutionException. " +
                                            "This is probably a bug in the bulk import tool - please raise an issue at https://github.com/pmonks/alfresco-bulk-import/issues/, including this full stack trace (and all \"caused by\" stack traces).",
                                            ree);
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
     * @return Is work paused?
     */
    public boolean isPaused()
    {
        return(paused);
    }


    /**
     * Pause the worker thread pool (no-op if the pool is already paused).
     */
    public void pause()
    {
        pauseLock.lock();

        try
        {
            paused = true;
        }
        finally
        {
            pauseLock.unlock();
        }
    }


    /**
     * Resume the worker thread pool (no-op if the pool is not paused).
     */
    public void resume()
    {
        pauseLock.lock();

        try
        {
            paused = false;
            pauseCondition.signalAll();
        }
        finally
        {
            pauseLock.unlock();
        }
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
