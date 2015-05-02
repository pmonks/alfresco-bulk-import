/*
 * Copyright (C) 2007-2015 Peter Monks.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.extension.bulkimport.BulkImportCallback;
import org.alfresco.extension.bulkimport.impl.WritableBulkImportStatus;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportSource;

import static org.alfresco.extension.bulkimport.BulkImportLogUtils.*;


/**
 * This class encapsulates the logic and state required to scan the source
 * and enqueue batches of work for the importer thread pool.  It is a stateful
 * class that is instantiated per-import.
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
    
    private final ServiceRegistry          serviceRegistry;
    private final String                   userId;
    private final int                      batchWeight;
    private final WritableBulkImportStatus importStatus;
    private final BulkImportSource         source;
    private final NodeRef                  target;
    private final String                   targetAsPath;
    private final BatchImporter            batchImporter;
    private final boolean                  replaceExisting;
    private final boolean                  dryRun;

    // Stateful unpleasantness
    private Map<String, List<String>>       parameters;
    private BlockingPausableExecutorService importThreadPool;
    private int                             currentBatchNumber;
    private List<BulkImportItem>            currentBatch;
    private int                             weightOfCurrentBatch;
    
    
    public Scanner(final ServiceRegistry                 serviceRegistry,
                   final String                          userId,
                   final int                             batchWeight,
                   final WritableBulkImportStatus        importStatus,
                   final BulkImportSource                source,
                   final Map<String, List<String>>       parameters,
                   final NodeRef                         target,
                   final BlockingPausableExecutorService importThreadPool,
                   final BatchImporter                   batchImporter)
    {
        // PRECONDITIONS
        assert serviceRegistry  != null : "serviceRegistry must not be null.";
        assert userId           != null : "userId must not be null.";
        assert batchWeight      > 0     : "batchWeight must be > 0.";
        assert importStatus     != null : "importStatus must not be null.";
        assert source           != null : "source must not be null.";
        assert parameters       != null : "parameters must not be null.";
        assert target           != null : "target must not be null.";
        assert importThreadPool != null : "importThreadPool must not be null.";
        assert batchImporter    != null : "batchImporter must not be null.";
        
        // Body
        this.serviceRegistry  = serviceRegistry;
        this.userId           = userId;
        this.batchWeight      = batchWeight;
        this.importStatus     = importStatus;
        this.source           = source;
        this.parameters       = parameters;
        this.target           = target;
        this.targetAsPath     = getRepositoryPath(target);
        this.importThreadPool = importThreadPool;
        this.batchImporter    = batchImporter;
        
        this.replaceExisting  = parameters.get(PARAMETER_REPLACE_EXISTING) == null ? false : Boolean.parseBoolean(parameters.get(PARAMETER_REPLACE_EXISTING).get(0));
        this.dryRun           = parameters.get(PARAMETER_DRY_RUN)          == null ? false : Boolean.parseBoolean(parameters.get(PARAMETER_DRY_RUN).get(0));

        currentBatchNumber    = 0;
        currentBatch          = null;
        weightOfCurrentBatch  = 0;
    }
    
    
    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        if (info(log)) info(log, "Bulk import started.");
        
        try
        {
            importStatus.importStarted(source.getName(),
                                       targetAsPath,
                                       importThreadPool,
                                       batchWeight,
                                       source.inPlaceImportPossible(parameters),
                                       dryRun);
            
            if (debug(log)) debug(log, "Initiating scanning on " + Thread.currentThread().getName() + "...");
            
            // Request the source to scan itself, folders first, calling us back with each item
            source.scanFolders(parameters, importStatus, this);

            // Submit any leftover folders in their own batch
            synchronized(this)
            {
                if (currentBatch != null)
                {
                    submitBatch(new Batch(currentBatchNumber, currentBatch));
                    currentBatch = null;
                }
            }
            
            // Scan files
            source.scanFolders(parameters, importStatus, this);

            importStatus.scanningComplete();
            
            // We're done scanning, so submit whatever is left in the final batch...
            synchronized(this)
            {
                if (currentBatch != null)
                {
                    submitBatch(new Batch(currentBatchNumber, currentBatch));
                    currentBatch = null;
                }
            }
            
            // ...and wait for everything to wrap up
            if (debug(log)) debug(log, "Scanning complete. Waiting for completion of import.");
            importThreadPool.shutdown();
            
            try
            {
                importThreadPool.await();
            }
            catch (final InterruptedException ie)
            {
                // Not much we can do here but log it and keep on truckin'
                if (warn(log)) warn(log, Thread.currentThread().getName() + " was interrupted while awaiting import thread pool termination.", ie);
            }
            if (debug(log)) debug(log, "Import complete, thread pool terminated.");
        }
        catch (final Throwable t)
        {
            Throwable rootCause          = getRootCause(t);
            String    rootCauseClassName = rootCause.getClass().getName();
            
            if (importStatus.isStopping() &&
                (rootCause instanceof InterruptedException ||
                 rootCause instanceof ClosedByInterruptException ||
                 "com.hazelcast.core.RuntimeInterruptedException".equals(rootCauseClassName)))  // Avoid a static dependency on Hazelcast...
            {
                // A stop import was requested
                if (debug(log)) debug(log, Thread.currentThread().getName() + " was interrupted by a stop request.", t);
            }
            else
            {
                // An unexpected exception occurred during scanning - log it and kill the import
                error(log, "Bulk import from '" + source.getName() + "' failed.", t);
                importStatus.unexpectedError(t);
            }
            
            if (debug(log)) debug(log, "Shutting down import thread pool and awaiting termination.");
            importThreadPool.shutdownNow();
            
            try
            {
                importThreadPool.await();
            }
            catch (final InterruptedException ie)
            {
                // Not much we can do here but log it and keep on truckin'
                if (warn(log)) warn(log, Thread.currentThread().getName() + " was interrupted while awaiting import thread pool termination.", ie);
            }
            if (debug(log)) debug(log, "Import complete, thread pool terminated.");
        }
        finally
        {
            importStatus.importComplete();

            if (info(log)) info(log, "Bulk import complete. " +
                                     currentBatchNumber +
                                     " batches prepared, and " +
                                     importStatus.getTargetCounter("Batches completed") == null ? 0 : importStatus.getTargetCounter("Batches completed") +
                                     " successfully imported, in " +
                                     getHumanReadableDuration(importStatus.getDurationInNs()));

            // Reset the stateful crap
            parameters           = null;
            importThreadPool     = null;

            synchronized(this)
            {
                currentBatchNumber   = 0;
                currentBatch         = null;
                weightOfCurrentBatch = 0;
            }
        }
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.BulkImportCallback#submit(org.alfresco.extension.bulkimport.source.BulkImportItem)
     */
    @Override
    public void submit(final BulkImportItem item)
        throws InterruptedException
    {
        // PRECONDITIONS
        assert item != null : "item must not be null.";

        // Body
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
        
        // Ensure submissions from multi-threaded import sources are orderly
        synchronized(this)
        {
            // Create a new List to hold the batch, if necessary
            if (currentBatch == null)
            {
                currentBatchNumber++;
                currentBatch         = new ArrayList<BulkImportItem>(batchWeight / 2);  // Make an educated guess as to the size of the batch, recalling that batches are sized based on "weight", not number of items
                weightOfCurrentBatch = 0;
            }

            // Add the new item to the batch
            currentBatch.add(item);
            weightOfCurrentBatch += item.weight();
            
            // If we've got a full batch, enqueue it
            if (weightOfCurrentBatch > batchWeight)
            {
                submitBatch(new Batch(currentBatchNumber, currentBatch));
                currentBatch = null;
            }
        }
    }
    
    
    /**
     * Used to submit a batch to the import thread pool.
     * 
     * @param batchNumber The batch number of this batch.
     * @param batch       The batch to submit <i>(may be null or empty)</i>.
     */
    void submitBatch(final Batch batch)    // Note package scope - this is deliberate!
    {
        if (batch != null &&
            batch.size() > 0)
        {
            importThreadPool.execute(new BatchImport(batch));
        }
    }
    
    
    /**
     * Returns a human-readable rendition of the repository path of the given NodeRef.
     * 
     * @param nodeRef The nodeRef from which to dervice a path <i>(may be null)</i>.
     * @return The human-readable path <i>(will be null if the nodeRef is null or the nodeRef doesn't exist)</i>.
     */
    private final String getRepositoryPath(final NodeRef nodeRef)
    {
        String result = null;
        
        if (nodeRef != null)
        {
            List<FileInfo> pathElements = null;
            
            try
            {
                pathElements = serviceRegistry.getFileFolderService().getNamePath(null, nodeRef);   // Note: violates Google Code issue #132, but allowable in this case since this is a R/O method without an obvious alternative

                if (pathElements != null && pathElements.size() > 0)
                {
                    StringBuilder temp = new StringBuilder();
                    
                    for (FileInfo pathElement : pathElements)
                    {
                        temp.append("/");
                        temp.append(pathElement.getName());
                    }
                    
                    result = temp.toString();
                }
            }
            catch (final FileNotFoundException fnfe)
            {
                // Do nothing
            }
        }
        
        return(result);
    }
    
    
    private final Throwable getRootCause(final Throwable t)
    {
        Throwable result = null;
        
        if (t != null)
        {
            result = t;
            
            while (result.getCause() != null)
            {
                result = result.getCause();
            }
        }
        
        return(result);
    }
    
    
    /**
     * @param durationInNs A duration in nanoseconds.
     * @return A human readable string representing that duration as "Ud Vh Wm Xs Y.Zms".
     */
    private final String getHumanReadableDuration(final Long durationInNs)
    {
        String result = null;
        
        if (durationInNs == null || durationInNs <= 0)
        {
            result = "0d 0h 0m 0s 0.0ms";
        }
        else
        {
            int days         = ((int)(durationInNs / (1000L * 1000 * 1000 * 60 * 60 * 24)));
            int hours        = ((int)(durationInNs / (1000L * 1000 * 1000 * 60 * 60))) % 24;
            int minutes      = ((int)(durationInNs / (1000L * 1000 * 1000 * 60))) % 60;
            int seconds      = ((int)(durationInNs / (1000L * 1000 * 1000))) % 60;
            int milliseconds = ((int)(durationInNs / (1000L * 1000))) % 1000;
            int microseconds = ((int)(durationInNs / (1000L))) % 1000;
                    
            result = days    + "d " +
                     hours   + "h " +
                     minutes + "m " +
                     seconds + "s " +
                     milliseconds + "." + microseconds + "ms";
        }
        
        return(result);
    }
    
    
    private final class BatchImport
        implements Runnable
    {
        private final Batch batch;
        
        public BatchImport(final Batch batch)
        {
            this.batch = batch;
        }
        
        
        @Override
        public void run()
        {
            try
            {
                batchImporter.importBatch(Scanner.this, userId, target, batch, replaceExisting, dryRun);
            }
            catch (final Throwable t)
            {
                Throwable rootCause = t;
                
                while (rootCause.getCause() != null)
                {
                    rootCause = rootCause.getCause();
                }
                
                String rootCauseClassName = rootCause.getClass().getName();
                
                if (importStatus.isStopping() &&
                    (rootCause instanceof InterruptedException ||
                     rootCause instanceof ClosedByInterruptException ||
                     "com.hazelcast.core.RuntimeInterruptedException".equals(rootCauseClassName)))  // For compatibility across 4.x *sigh*
                {
                    // A stop import was requested
                    if (debug(log)) debug(log, Thread.currentThread().getName() + " was interrupted by a stop request.", t);
                }
                else
                {
                    // An unexpected exception during import of the batch - log it and kill the entire import
                    error(log, "Bulk import from '" + source.getName() + "' failed.", t);
                    importStatus.unexpectedError(t);
                    
                    if (debug(log)) debug(log, "Shutting down import thread pool.");
                    importThreadPool.shutdownNow();
                }
            }
        }
    }
}
