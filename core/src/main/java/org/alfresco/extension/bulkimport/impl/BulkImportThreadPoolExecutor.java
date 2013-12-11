/*
 * Copyright (C) 2007-2013 Peter Monks.
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

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.java.util.concurrent.NotifyingBlockingThreadPoolExecutor;


/**
 * This class provides a simplified <code>ThreadPoolExecutor</code> that uses sensible defaults for the bulk import tool.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public class BulkImportThreadPoolExecutor
    extends NotifyingBlockingThreadPoolExecutor
{
    private final static Log log = LogFactory.getLog(BulkImportThreadPoolExecutor.class);
    
    private final static int      DEFAULT_THREAD_POOL_SIZE     = Runtime.getRuntime().availableProcessors() * 2;   // We naively assume 50+% of time is spent blocked on I/O
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
        super(threadPoolSize    <= 0    ? DEFAULT_THREAD_POOL_SIZE     : threadPoolSize,
              queueSize         <= 0    ? DEFAULT_QUEUE_SIZE           : queueSize,
              keepAliveTime     <= 0    ? DEFAULT_KEEP_ALIVE_TIME      : keepAliveTime,
              keepAliveTimeUnit == null ? DEFAULT_KEEP_ALIVE_TIME_UNIT : keepAliveTimeUnit,
              new BulkImportThreadFactory());
        
        if (log.isDebugEnabled()) log.debug("Creating new bulk import thread pool." +
                                            "\n\tthreadPoolSize = " + threadPoolSize +
                                            "\n\tqueueSize = " + queueSize +
                                            "\n\tkeepAliveTime = " + keepAliveTime + " " + String.valueOf(keepAliveTimeUnit));
    }
    
}
