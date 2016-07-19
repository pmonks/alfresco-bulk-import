/*
 * Copyright (C) 2007-2016 Peter Monks.
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
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.nio.channels.ClosedByInterruptException;

import org.alfresco.extension.bulkimport.util.ThreadPauser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.extension.bulkimport.BulkImportCallback;
import org.alfresco.extension.bulkimport.BulkImportCompletionHandler;
import org.alfresco.extension.bulkimport.BulkImportStatus;

import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportSource;
import org.alfresco.extension.bulkimport.source.BulkImportItemVersion;

import static java.util.concurrent.TimeUnit.*;
import static org.alfresco.extension.bulkimport.util.Utils.*;
import static org.alfresco.extension.bulkimport.util.LogUtils.*;


/**
 * This class encapsulates the logic and state required to scan the source
 * and enqueue batches of work for the importer thread pool.  It is a stateful
 * class that is instantiated once per-import.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public final class Scanner
    implements Runnable,
               BulkImportCallback
{
    private final static Log log = LogFactory.getLog(Scanner.class);
    
    private final static long     SLEEP_TIME = 10L;
    private final static TimeUnit SLEEP_TIME_UNITS = TimeUnit.MINUTES;
    
    private final static String PARAMETER_REPLACE_EXISTING = "replaceExisting";
    private final static String PARAMETER_DRY_RUN          = "dryRun";
    
    private final static int MULTITHREADING_THRESHOLD = 3;    // The number of batches above which multi-threading kicks in

    private final static int ONE_GIGABYTE = (int)Math.pow(2, 30);

    private final static BulkImportCompletionHandler loggingBulkImportCompletionHandler = new LoggingBulkImportCompletionHandler();

    private final String                            userId;
    private final int                               batchWeight;
    private final WritableBulkImportStatus          importStatus;
    private final ThreadPauser                      pauser;
    private final BulkImportSource                  source;
    private final NodeRef                           target;
    private final String                            targetAsPath;
    private final BatchImporter                     batchImporter;
    private final List<BulkImportCompletionHandler> completionHandlers;
    
    // Parameters
    private final boolean replaceExisting;
    private final boolean dryRun;

    // Stateful unpleasantness
    private Map<String, List<String>>                   parameters;
    private BulkImportThreadPoolExecutor                importThreadPool;
    private int                                         currentBatchNumber;
    private List<BulkImportItem<BulkImportItemVersion>> currentBatch;
    private int                                         weightOfCurrentBatch;
    private boolean                                     filePhase;
    private boolean                                     multiThreadedImport;

    
    public Scanner(final ServiceRegistry                   serviceRegistry,
                   final String                            userId,
                   final int                               batchWeight,
                   final WritableBulkImportStatus          importStatus,
                   final ThreadPauser                      pauser,
                   final BulkImportSource                  source,
                   final Map<String, List<String>>         parameters,
                   final NodeRef                           target,
                   final BulkImportThreadPoolExecutor      importThreadPool,
                   final BatchImporter                     batchImporter,
                   final List<BulkImportCompletionHandler> completionHandlers)
    {
        // PRECONDITIONS
        assert serviceRegistry  != null : "serviceRegistry must not be null.";
        assert userId           != null : "userId must not be null.";
        assert batchWeight      > 0     : "batchWeight must be > 0.";
        assert importStatus     != null : "importStatus must not be null.";
        assert pauser           != null : "pauser must not be null.";
        assert source           != null : "source must not be null.";
        assert parameters       != null : "parameters must not be null.";
        assert target           != null : "target must not be null.";
        assert importThreadPool != null : "importThreadPool must not be null.";
        assert batchImporter    != null : "batchImporter must not be null.";
        
        // Body
        this.userId             = userId;
        this.batchWeight        = batchWeight;
        this.importStatus       = importStatus;
        this.pauser             = pauser;
        this.source             = source;
        this.parameters         = parameters;
        this.target             = target;
        this.targetAsPath       = convertNodeRefToPath(serviceRegistry, target);
        this.importThreadPool   = importThreadPool;
        this.batchImporter      = batchImporter;
        this.completionHandlers = completionHandlers;
        
        this.replaceExisting = parameters.get(PARAMETER_REPLACE_EXISTING) == null ? false : Boolean.parseBoolean(parameters.get(PARAMETER_REPLACE_EXISTING).get(0));
        this.dryRun          = parameters.get(PARAMETER_DRY_RUN)          == null ? false : Boolean.parseBoolean(parameters.get(PARAMETER_DRY_RUN).get(0));

        this.currentBatchNumber   = 0;
        this.currentBatch         = null;
        this.weightOfCurrentBatch = 0;
        this.filePhase            = false;
        this.multiThreadedImport  = false;
    }
    
    
    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        boolean inPlacePossible = false;
        
        try
        {
            source.init(importStatus, parameters);
            inPlacePossible = source.inPlaceImportPossible();
            
            if (info(log)) info(log, "Import (" + (inPlacePossible ? "in-place" : "streaming") + ") started from " + source.getName() + ".");
            
            importStatus.importStarted(userId,
                                       source,
                                       targetAsPath,
                                       importThreadPool,
                                       batchWeight,
                                       inPlacePossible,
                                       dryRun);

            // ------------------------------------------------------------------
            // Phase 1 - Folder scanning (single threaded)
            // ------------------------------------------------------------------

            source.scanFolders(importStatus, this);
            
            if (debug(log)) debug(log, "Folder import complete in " + getHumanReadableDuration(importStatus.getDurationInNs()) + ".");
            
            // ------------------------------------------------------------------
            // Phase 2 - File scanning
            // ------------------------------------------------------------------

            filePhase = true;
            
            // Maximise level of concurrency, since there's no longer any risk of out-of-order batches
            source.scanFiles(importStatus, this);

            if (debug(log)) debug(log, "File scan complete in " + getHumanReadableDuration(importStatus.getDurationInNs()) + ".");
            
            importStatus.scanningComplete();
            
            // ------------------------------------------------------------------
            // Phase 3 - Wait for multi-threaded import to complete and shutdown
            // ------------------------------------------------------------------

            submitCurrentBatch();  // Submit whatever is left in the final (partial) batch...
            awaitCompletion();
            
            if (debug(log)) debug(log, "Import complete" + (multiThreadedImport ? ", thread pool shutdown" : "") + ".");
        }
        catch (final Throwable t)
        {
            Throwable rootCause          = getRootCause(t);
            String    rootCauseClassName = rootCause.getClass().getName();
            
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
                // An unexpected exception occurred during scanning - log it and kill the import
                error(log, "Bulk import from '" + source.getName() + "' failed.", t);
                importStatus.unexpectedError(t);
            }
                
            if (debug(log)) debug(log, "Forcibly shutting down import thread pool and awaiting shutdown...");
            importThreadPool.shutdownNow();
            
            try
            {
                importThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);  // Wait forever (technically merely a very long time, but whatevs...)
            }
            catch (final InterruptedException ie)
            {
                // Not much we can do here but log it and keep on truckin'
                if (warn(log)) warn(log, Thread.currentThread().getName() + " was interrupted while awaiting shutdown of import thread pool.", ie);
            }
        }
        finally
        {
            // Reset the thread factory
            if (importThreadPool.getThreadFactory() instanceof BulkImportThreadFactory)
            {
                ((BulkImportThreadFactory)importThreadPool.getThreadFactory()).reset();
            }

            // Mark the import complete
            importStatus.importComplete();
            
            // Invoke the completion handlers (if any)
            if (completionHandlers != null)
            {
                for (final BulkImportCompletionHandler handler : completionHandlers)
                {
                    try
                    {
                        handler.importComplete(importStatus);
                    }
                    catch (final Exception e)
                    {
                        if (error(log)) error(log, "Completion handler threw an unexpected exception. It will be ignored.", e);
                    }
                }
            }
            
            // Always invoke the logging completion handler last
            loggingBulkImportCompletionHandler.importComplete(importStatus);
        }
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.BulkImportCallback#submit(org.alfresco.extension.bulkimport.source.BulkImportItem)
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized void submit(final BulkImportItem item)
        throws InterruptedException
    {
        // PRECONDITIONS
        if (item == null)
        {
            throw new IllegalArgumentException("Import source '" + source.getName() + "' has logic errors - a null import item was submitted.");
        }
        
        if (item.getVersions() == null ||
            item.getVersions().size() <= 0)
        {
            throw new IllegalArgumentException("Import source '" + source.getName() + "' has logic errors - an empty import item was submitted.");
        }

        // Body
        if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
        
        // If the weight of the new item would blow out the current batch, submit the batch as-is (i.e. *before* adding the newly submitted item).
        // This ensures that heavy items start a new batch (and possibly end up in a batch by themselves).
        int weight = weight(item);
        
        if (weightOfCurrentBatch + weight > batchWeight)
        {
            submitCurrentBatch();
        }
        
        // Create a new batch, if necessary
        if (currentBatch == null)
        {
            currentBatchNumber++;
            currentBatch         = new ArrayList<>(batchWeight);
            weightOfCurrentBatch = 0;
        }
        
        // Finally, add the item to the current batch
        currentBatch.add(item);
        weightOfCurrentBatch += weight;
    }


    private synchronized void submitCurrentBatch()
        throws InterruptedException
    {
        // Implement pauses at batch boundaries only
        pauser.blockIfPaused();

        if (currentBatch != null && currentBatch.size() > 0)
        {
            final Batch batch = new Batch(currentBatchNumber, currentBatch);
            
            // Prepare for the next batch
            currentBatch = null;
            importStatus.incrementTargetCounter(BulkImportStatus.TARGET_COUNTER_BATCHES_SUBMITTED);
            
            if (multiThreadedImport)
            {
                // Submit the batch to the thread pool
                submitBatch(batch);
            }
            else
            {
                // Import the batch directly on this thread
                batchImporter.importBatch(userId, target, batch, replaceExisting, dryRun);
                
                // Check if the multi-threading threshold has been reached
                multiThreadedImport = filePhase && currentBatchNumber >= MULTITHREADING_THRESHOLD;
                
                if (multiThreadedImport && debug(log)) debug(log, "Multi-threading threshold (" + MULTITHREADING_THRESHOLD + " batch" + pluralise(MULTITHREADING_THRESHOLD, "es") + ") reached - switching to multi-threaded import.");
            }
        }
    }
    
    
    /**
     * Used to submit a batch to the import thread pool.  Note that this method
     * can block (due to the use of a blocking queue in the thread pool).
     * 
     * @param batch The batch to submit <i>(may be null or empty, although that will result in a no-op)</i>.
     */
    private void submitBatch(final Batch batch)
    {
        if (batch        != null &&
            batch.size() >  0)
        {
            if (importStatus.inProgress() &&
                !importStatus.isStopping())
            {
                importThreadPool.execute(new BatchImportJob(batch));
            }
            else
            {
                if (warn(log)) warn(log, "New batch submitted during shutdown - ignoring new work.");
            }
        }
    }
    

    /**
     * Awaits completion of the import, by checking if the import thread pool
     * and associated queue are empty, with sleeps in between polls.
     * 
     * @throws InterruptedException If a sleep is interrupted.
     */
    private final void awaitCompletion()
        throws InterruptedException
    {
        if (multiThreadedImport)
        {
            // Log status then wait for everything to wrap up...
            if (debug(log)) debug(log, "Scanning complete. Waiting for completion of multithreaded import.");
            logStatusInfo();
        }

        importThreadPool.shutdown();  // Orderly shutdown (lets the queue drain)

        // Log status every hour, then go back to waiting - in single threaded case this won't wait at all
        while (!importThreadPool.awaitTermination(1, TimeUnit.HOURS))
        {
            logStatusInfo();
        }
    }
    
    
    /**
     * Writes a detailed informational status message to the log, at INFO level
     */
    private final void logStatusInfo()
    {
        if (info(log))
        {
            try
            {
                final int   batchesInProgress           = importThreadPool.getQueueSize() + importThreadPool.getActiveCount();
                final Float batchesPerSecond            = importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE, SECONDS);
                final Long  estimatedCompletionTimeInNs = importStatus.getEstimatedRemainingDurationInNs();
                String      message                     = null;
                
                if (batchesPerSecond != null && estimatedCompletionTimeInNs != null)
                {
                    message = String.format("Multithreaded import in progress - %d batch%s yet to be imported. " +
                                            "At current rate (%.3f batch%s per second), estimated completion in %s.",
                                            batchesInProgress, pluralise(batchesInProgress, "es"),
                                            batchesPerSecond,  pluralise(batchesPerSecond, "es"), getHumanReadableDuration(estimatedCompletionTimeInNs, false));
                }
                else
                {
                    message = String.format("Multithreaded import in progress - %d batch%s yet to be imported.",
                                            batchesInProgress, pluralise(batchesInProgress, "es"));
                }
                
                info(log, message);
            }
            catch (final IllegalFormatException ife)
            {
                // To help troubleshoot bugs in the String.format calls above
                error(log, ife);
            }
        }
    }
    
    
    /*
     * Estimates the "weight" (a unitless value) of the given item.  This is
     * counted as 1 per content and metadata file in the item, plus 100 per
     * gigabyte of streamed data (so that files of 1GB or more cause the batch
     * to end).
     */
    private final int weight(final BulkImportItem<BulkImportItemVersion> item)
    {
        int result = 0;
        
        for (final BulkImportItemVersion version : item.getVersions())
        {
            result++;
            
            if (version.hasContent() && !version.contentIsInPlace())
            {
                result += (int)((float)item.sizeInBytes() / ONE_GIGABYTE * 100);
            }
        }

        return(result);
    }
        
        
    private final class BatchImportJob
        implements Runnable
    {
        private final Batch  batch;
        
        public BatchImportJob(final Batch batch)
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
                Throwable rootCause          = getRootCause(t);
                String    rootCauseClassName = rootCause.getClass().getName();
                
                if (importStatus.isStopping() &&
                    (rootCause instanceof InterruptedException ||
                     rootCause instanceof ClosedByInterruptException ||
                     "com.hazelcast.core.RuntimeInterruptedException".equals(rootCauseClassName)))  // For compatibility across 4.x *sigh*
                {
                    // A stop import was requested
                    if (debug(log)) debug(log, Thread.currentThread().getName() + " was interrupted by a stop request.", t);
                    Thread.currentThread().interrupt();                    
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
