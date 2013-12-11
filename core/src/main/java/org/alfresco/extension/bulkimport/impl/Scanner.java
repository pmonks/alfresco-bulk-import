/*
 * Copyright (C) 2007-2013 Peter Monks.
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

import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.alfresco.extension.bulkimport.BulkImportStatus.ProcessingState;
import org.alfresco.extension.bulkimport.source.BulkImportCallback;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportSource;
import org.alfresco.extension.bulkimport.source.BulkImportItem.Version;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java.util.concurrent.NotifyingBlockingThreadPoolExecutor;

/**
 * This class encapulates the logic of scanning the source and enqueuing
 * batches of work.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public final class Scanner
    implements Runnable,
               BulkImportCallback
{
    private final static Log log = LogFactory.getLog(Scanner.class);
    
    private final static String PARAMETER_REPLACE_EXISTING = "replaceExisting";
    private final static String PARAMETER_DRY_RUN          = "dryRun";
    
    private final String                              userId;
    private final int                                 batchWeight;
    private final BulkImportStatusImpl                importStatus;
    private final BulkImportSource                    source;
    private final Map<String, String>                 parameters;
    private final NodeRef                             target;
    private final NotifyingBlockingThreadPoolExecutor importThreadPool;
    private final BatchImporter                       batchImporter;
    private final boolean                             replaceExisting;
    private final boolean                             dryRun;

    // Stateful unpleasantness
    private List<BulkImportItem> currentBatch;
    private int                  currentBatchesWeight;
    
    
    public Scanner(final String               userId,
                   final int                  batchWeight,
                   final BulkImportStatusImpl importStatus,
                   final BulkImportSource     source,
                   final Map<String, String>  parameters,
                   final NodeRef              target,
                   final NotifyingBlockingThreadPoolExecutor importThreadPool,
                   final BatchImporter        batchImporter)
    {
        // PRECONDITIONS
        assert userId           != null : "userId must not be null.";
        assert batchWeight      > 0     : "batchWeight must be > 0.";
        assert importStatus     != null : "importStatus must not be null.";
        assert source           != null : "source must not be null.";
        assert parameters       != null : "parameters must not be null.";
        assert target           != null : "target must not be null.";
        assert importThreadPool != null : "importThreadPool must not be null.";
        assert batchImporter    != null : "batchImporter must not be null.";
        
        // Body
        this.userId           = userId;
        this.batchWeight      = batchWeight;
        this.importStatus     = importStatus;
        this.source           = source;
        this.parameters       = parameters;
        this.target           = target;
        this.importThreadPool = importThreadPool;
        this.batchImporter    = batchImporter;
        
        this.replaceExisting  = Boolean.parseBoolean(parameters.get(PARAMETER_REPLACE_EXISTING));
        this.dryRun           = Boolean.parseBoolean(parameters.get(PARAMETER_DRY_RUN));
    }
    
    
    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " started.");
        
        try
        {
            // Request the source to scan itself, calling us back with each item
            source.scan(parameters, importStatus, this);
            
            // At this point the scan is complete, so wait until all processing is complete
            importThreadPool.await();
            
            //####TODO: REGISTER COMPLETION OF SCANNING WITH IMPORT STATUS
        }
        catch (final Throwable t)
        {
            Throwable rootCause = t;
            
            while (rootCause.getCause() != null)
            {
                rootCause = rootCause.getCause();
            }
            
            String rootCauseClassName = rootCause.getClass().getName();
            
            if (importStatus.getProcessingState().equals(ProcessingState.STOPPING) &&
                (rootCause instanceof InterruptedException ||
                 rootCause instanceof ClosedByInterruptException ||
                 "com.hazelcast.core.RuntimeInterruptedException".equals(rootCauseClassName)))  // For compatibility across 4.x *sigh*
            {
                // A stop import was requested
                if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " was interrupted by a stop request.", t);
            }
            else
            {
                // An unexpected exception during scanning - log it and kill the import
                log.error("Bulk import from '" + source.getName() + "' failed.", t);
                
                if (log.isDebugEnabled()) log.debug("Shutting down import thread pool.");
                importThreadPool.shutdownNow();
                importStatus.importFailed(t);
            }
        }
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportCallback#enqueue(org.alfresco.extension.bulkimport.source.BulkImportItem)
     */
    @Override
    public void enqueue(final BulkImportItem item)
        throws InterruptedException
    {
        if (item == null)
        {
            throw new IllegalArgumentException("item must not be null.");
        }

        if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
        
        // No one should be calling us multi-threaded, but just in case...
        synchronized(currentBatch)
        {
            if (currentBatch == null)
            {
                currentBatch         = new ArrayList<BulkImportItem>(batchWeight / 2);  // Make a guess as to the size of the batch
                currentBatchesWeight = 0;
            }
            
            currentBatch.add(item);
            currentBatchesWeight += weight(item);
            
            if (currentBatchesWeight > batchWeight)
            {
                importThreadPool.execute(new BatchImport(currentBatch));
                currentBatch = null;
            }
        }
    }
    
    
    /**
     * Simplistic measure of the "weight" of an item.
     * 
     * @param item The item to "weigh" <i>(may be null)</i>.
     * @return A number indicating the approximate "weight" of the item <i>(will be >= 0)</i>.
     */
    private final int weight(final BulkImportItem item)
    {
        int result = 0;
        
        if (item != null)
        {
            for (final Version version : item.getVersions())
            {
                if (version.hasContent()) result++;
                if (version.hasMetadata()) result++;
            }
        }
        
        return(result);
    }
    
    
    
    private final class BatchImport
        implements Runnable
    {
        private final List<BulkImportItem> batch;
        
        public BatchImport(final List<BulkImportItem> batch)
        {
            this.batch = batch;
        }
        
        
        @Override
        public void run()
        {
            try
            {
                batchImporter.importBatch(userId, target, batch, replaceExisting, dryRun);
            }
            catch (final Throwable t)
            {
                Throwable rootCause = t;
                
                while (rootCause.getCause() != null)
                {
                    rootCause = rootCause.getCause();
                }
                
                String rootCauseClassName = rootCause.getClass().getName();
                
                if (importStatus.getProcessingState().equals(ProcessingState.STOPPING) &&
                    (rootCause instanceof InterruptedException ||
                     rootCause instanceof ClosedByInterruptException ||
                     "com.hazelcast.core.RuntimeInterruptedException".equals(rootCauseClassName)))  // For compatibility across 4.x *sigh*
                {
                    // A stop import was requested
                    if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " was interrupted by a stop request.", t);
                }
                else
                {
                    // An unexpected exception during scanning - log it and kill the import
                    log.error("Bulk import from '" + source.getName() + "' failed.", t);
                    
                    if (log.isDebugEnabled()) log.debug("Shutting down import thread pool.");
                    importThreadPool.shutdownNow();
                    importStatus.importFailed(t);
                }
            }
        }
    }
}
