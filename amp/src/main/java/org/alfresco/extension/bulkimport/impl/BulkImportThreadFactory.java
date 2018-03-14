/*
 * Copyright (C) 2007 Peter Monks
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

import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;


/**
 * This ThreadFactory provides human-readable names for threads initiated by the Bulk Filesystem Importer.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public class BulkImportThreadFactory
    implements ThreadFactory
{
    private final static String THREAD_NAME_PREFIX = "BulkImport-Importer-";
    
    private final DecimalFormat decimalFormat       = new DecimalFormat("0000");
    private final AtomicLong    currentThreadNumber = new AtomicLong(0);
    
    
    /**
     * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
     */
    @Override
    public Thread newThread(final Runnable runnable)
    {
        final Thread result = Executors.defaultThreadFactory().newThread(runnable);
        
        result.setName(THREAD_NAME_PREFIX + decimalFormat.format(currentThreadNumber.incrementAndGet()));
        result.setDaemon(true);
        
        return(result);
    }
    
    
    /**
     * Resets this thread pool (i.e. sets the thread number counter back to zero).
     */
    public void reset()
    {
        currentThreadNumber.set(0);
    }

}
