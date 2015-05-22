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
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.extension.bulkimport.BulkImportCallback;
import org.alfresco.extension.bulkimport.BulkImportStatus;
import org.alfresco.extension.bulkimport.impl.WritableBulkImportStatus;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportSource;

import static java.util.concurrent.TimeUnit.*;
import static org.alfresco.extension.bulkimport.util.Utils.*;
import static org.alfresco.extension.bulkimport.util.LogUtils.*;


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
    
    private final static long MAX_SLEEP_TIME_IN_MS = 10L * 60L * 1000L;   // 10 minutes
    private final static long MIN_SLEEP_TIME_IN_MS = 1000L;               // 1 second
    
    private final static String PARAMETER_REPLACE_EXISTING = "replaceExisting";
    private final static String PARAMETER_DRY_RUN          = "dryRun";
    
    private final static int MULTITHREADING_THRESHOLD = 3;  // The number of batches above which multi-threading kicks in
    
    private final static int ONE_GIGABYTE = (int)Math.pow(2, 30);
    
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
    private Map<String, List<String>>    parameters;
    private BulkImportThreadPoolExecutor importThreadPool;
    private int                          currentBatchNumber;
    private List<BulkImportItem>         currentBatch;
    private int                          weightOfCurrentBatch;
    private boolean                      multiThreadedImport;
    
    
    public Scanner(final ServiceRegistry              serviceRegistry,
                   final String                       userId,
                   final int                          batchWeight,
                   final WritableBulkImportStatus     importStatus,
                   final BulkImportSource             source,
                   final Map<String, List<String>>    parameters,
                   final NodeRef                      target,
                   final BulkImportThreadPoolExecutor importThreadPool,
                   final BatchImporter                batchImporter)
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
        this.userId           = userId;
        this.batchWeight      = batchWeight;
        this.importStatus     = importStatus;
        this.source           = source;
        this.parameters       = parameters;
        this.target           = target;
        this.targetAsPath     = convertNodeRefToPath(serviceRegistry, target);
        this.importThreadPool = importThreadPool;
        this.batchImporter    = batchImporter;
        
        this.replaceExisting  = parameters.get(PARAMETER_REPLACE_EXISTING) == null ? false : Boolean.parseBoolean(parameters.get(PARAMETER_REPLACE_EXISTING).get(0));
        this.dryRun           = parameters.get(PARAMETER_DRY_RUN)          == null ? false : Boolean.parseBoolean(parameters.get(PARAMETER_DRY_RUN).get(0));

        currentBatchNumber    = 0;
        currentBatch          = null;
        weightOfCurrentBatch  = 0;
        multiThreadedImport   = false;
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
            source.init(parameters);
            inPlacePossible = source.inPlaceImportPossible();
            
            if (info(log)) info(log, (inPlacePossible ? "In place" : "Streaming") + " bulk import started.");
            
            importStatus.importStarted(source,
                                       targetAsPath,
                                       importThreadPool,
                                       batchWeight,
                                       inPlacePossible,
                                       dryRun);
            
            // Default pool sizes (which get overridden per phase)
            final int folderPhasePoolSize = importThreadPool.getCorePoolSize();
            final int filePhasePoolSize   = importThreadPool.getMaximumPoolSize();

            // ---------------------------------------------------------------
            // Phase 1 - Folder scanning
            // ---------------------------------------------------------------

            // Minimise level of concurrency, to reduce risk of out-of-order batches (child before parent)

            importThreadPool.setCorePoolSize(folderPhasePoolSize);
            importThreadPool.setMaximumPoolSize(folderPhasePoolSize);
            source.scanFolders(importStatus, this);
            submitCurrentBatch();  // Submit whatever is left in the final (partial) folder batch...
            
            if (info(log)) info(log, "Folder scan complete in " + getHumanReadableDuration(importStatus.getDurationInNs()) + ".");
            
            // ---------------------------------------------------------------
            // Phase 2 - File scanning
            // ---------------------------------------------------------------

            // Maximise level of concurrency, since there's no longer any risk of out-of-order batches
            importThreadPool.setCorePoolSize(filePhasePoolSize);
            importThreadPool.setMaximumPoolSize(filePhasePoolSize);
            source.scanFiles(importStatus, this);
            submitCurrentBatch();  // Submit whatever is left in the final (partial) file batch...

            if (info(log)) info(log, "File scan complete in " + getHumanReadableDuration(importStatus.getDurationInNs()) + ".");
            
            importStatus.scanningComplete();
            
            // ---------------------------------------------------------------
            // Phase 3 - Wait for multi-threaded import to complete
            // ---------------------------------------------------------------

            awaitCompletion();

            // ... and finally shutdown the thread pool.
            importThreadPool.shutdown();
            importThreadPool.await();
            if (debug(log)) debug(log, "Import complete" + (multiThreadedImport ? ", thread pool shutdown" : "") + ".");
        }
        catch (final Throwable t)
        {
            Throwable rootCause          = getRootCause(t);  // Unwrap exceptions to get the root cause
            String    rootCauseClassName = rootCause.getClass().getName();
            
            if (rootCause instanceof InterruptedException ||
                rootCause instanceof ClosedByInterruptException ||
                "com.hazelcast.core.RuntimeInterruptedException".equals(rootCauseClassName))  // Avoid a static dependency on Hazelcast...
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
                importThreadPool.await();
            }
            catch (final InterruptedException ie)
            {
                // Not much we can do here but log it and keep on truckin'
                if (warn(log)) warn(log, Thread.currentThread().getName() + " was interrupted while awaiting shutdown of import thread pool.", ie);
            }
        }
        finally
        {
            importStatus.importComplete();

            if (info(log))
            {
                final String processingState            = importStatus.getProcessingState();
                final String durationStr                = getHumanReadableDuration(importStatus.getDurationInNs());
                final long   batchesImported            = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE)              == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE);
                final float  batchesPerSecond           = importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE, SECONDS) == null ? 0.0F : importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE, SECONDS);
                final float  nodesPerSecond             = importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_NODES_IMPORTED, SECONDS)   == null ? 0.0F : importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_NODES_IMPORTED, SECONDS);
                final float  bytesPerSecond             = importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BYTES_IMPORTED, SECONDS)   == null ? 0.0F : importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BYTES_IMPORTED, SECONDS);
                final long   nodesImported              = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_IMPORTED)                == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_IMPORTED);
                final long   versionsImported           = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_VERSIONS_IMPORTED)             == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_VERSIONS_IMPORTED);
                final long   metadataPropertiesImported = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_METADATA_PROPERTIES_IMPORTED)  == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_METADATA_PROPERTIES_IMPORTED);
                final long   bytesImported              = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BYTES_IMPORTED)                == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BYTES_IMPORTED);
                final long   contentInPlace             = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_IN_PLACE_CONTENT_LINKED)       == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_IN_PLACE_CONTENT_LINKED);
                final long   contentStreamed            = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_CONTENT_STREAMED)              == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_CONTENT_STREAMED);
                final long   filesSkipped               = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_SKIPPED)                 == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_SKIPPED);
                final long   outOfOrderBatches          = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_OUT_OF_ORDER_RETRIES)          == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_OUT_OF_ORDER_RETRIES);
                
                try
                {
                    final String message = String.format("%s bulk import completed (%s) in %s.\n" +
                                                         "\tBatch%s:\t\t%d imported of %d total (%.3f / sec)\n" +
                                                         "\tNode%s:\t\t\t%d (%.3f / sec)\n" +
                                                         "\tByte%s:\t\t\t%d (%.3f / sec)\n" +
                                                         "\tVersion%s:\t\t%d\n" +
                                                         "\tMetadata propert%s:\t%d\n" +
                                                         "\tFiles:\t\t\t%d in-place, %d streamed, %d skipped\n" +
                                                         "\tOut-of-order batch%s:\t%d",
                                                         (inPlacePossible ? "In place" : "Streaming"),    processingState, durationStr,
                                                         pluralise(batchesImported, "es"),                batchesImported, currentBatchNumber, batchesPerSecond,
                                                         pluralise(nodesImported),                        nodesImported,   nodesPerSecond,
                                                         pluralise(bytesImported),                        bytesImported,   bytesPerSecond,
                                                         pluralise(versionsImported),                     versionsImported,
                                                         (metadataPropertiesImported == 1 ? "y" : "ies"), metadataPropertiesImported,
                                                         contentInPlace,                                  contentStreamed, filesSkipped,
                                                         pluralise(outOfOrderBatches, "es"),              outOfOrderBatches);
    
                    info(log, message);
                }
                catch (final IllegalFormatException ife)
                {
                    // To help troubleshoot bugs in the String.format call above
                    error(log, ife);
                }
            }
        }
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.BulkImportCallback#submit(org.alfresco.extension.bulkimport.source.BulkImportItem)
     */
    @Override
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
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
        
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
            currentBatch         = new ArrayList<BulkImportItem>(batchWeight);
            weightOfCurrentBatch = 0;
        }
        
        // Finally, add the item to the current batch
        currentBatch.add(item);
        weightOfCurrentBatch += weight;
    }
    
    
    private synchronized void submitCurrentBatch()
        throws InterruptedException
    {
        if (currentBatch != null && currentBatch.size() > 0)
        {
            final Batch batch = new Batch(currentBatchNumber, currentBatch);
            
            // Prepare for the next batch
            currentBatch = null;
            
            if (multiThreadedImport)
            {
                // Submit the batch to the thread pool
                submitBatch(batch);
            }
            else
            {
                // Execute the batch directly on this thread
                batchImporter.importBatch(this, userId, target, batch, replaceExisting, dryRun);
                
                // Check if the multi-threading threshold has been reached
                multiThreadedImport = currentBatchNumber >= MULTITHREADING_THRESHOLD;
                
                if (multiThreadedImport && info(log)) info(log, "Multi-threading threshold (" + MULTITHREADING_THRESHOLD + " batch" + pluralise(MULTITHREADING_THRESHOLD, "es") + ") reached - switching to multi-threaded import.");
            }
        }
    }
    
    
    /**
     * Used to submit a batch to the import thread pool.  Note that this method
     * can block (due to the use of a blocking queue in the thread pool).
     * 
     * @param batch The batch to submit <i>(may be null or empty, although that will result in a no-op)</i>.
     */
    void submitBatch(final Batch batch)    // Note package scope - this is deliberate!
    {
        if (batch != null &&
            batch.size() > 0)
        {
            importThreadPool.execute(new BatchImportJob(batch));
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
        // No need to wait if we didn't go multi-threaded
        if (multiThreadedImport)
        {
            // ...wait for everything to wrap up...
            if (debug(log)) debug(log, "Scanning complete. Waiting for completion of multithreaded import.");
            
            boolean done = false;
                    
            while (!done)
            {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
                
                final int batchesInProgress = importThreadPool.queueSize() + importThreadPool.getActiveCount();
                
                done = batchesInProgress <= 0;
                
                if (!done)
                {
                    final Float batchesPerSecond            = importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE, SECONDS);
                    final Long  estimatedCompletionTimeInNs = importStatus.getEstimatedRemainingDurationInNs();
                    
                    long sleepTimeInMs = MIN_SLEEP_TIME_IN_MS;
                    
                    if (estimatedCompletionTimeInNs != null && estimatedCompletionTimeInNs > 0L)
                    {
                        // Sleep less than what we estimated - better to err on the side of checking again early
                        sleepTimeInMs = (long)(NANOSECONDS.toMillis(estimatedCompletionTimeInNs) * 0.5);
                        
                        // Clamp to our sleep limits
                        sleepTimeInMs = Math.max(sleepTimeInMs, MIN_SLEEP_TIME_IN_MS);
                        sleepTimeInMs = Math.min(sleepTimeInMs, MAX_SLEEP_TIME_IN_MS);
                    }
                    
                    if (info(log))
                    {
                        String message = null;
                        
                        if (batchesPerSecond != null && estimatedCompletionTimeInNs != null)
                        {
                            message = String.format("Multithreaded import in progress - %d batch%s yet to be imported. " +
                                                    "At current rate (%.3f batches per second), estimated completion in %s. " +
                                                    "Will check again in %s.",
                                                    batchesInProgress,
                                                    (batchesInProgress != 1 ? "es" : ""),
                                                    batchesPerSecond,
                                                    getHumanReadableDuration(estimatedCompletionTimeInNs, false),
                                                    getHumanReadableDuration(MILLISECONDS.toNanos(sleepTimeInMs), false));
                        }
                        else
                        {
                            message = String.format("Multithreaded import in progress - %d batch%s yet to be imported. " +
                                                    "Will check again in %s.",
                                                    batchesInProgress,
                                                    (batchesInProgress != 1 ? "es" : ""),
                                                    getHumanReadableDuration(MILLISECONDS.toNanos(sleepTimeInMs), false));
                        }
                        
                        info(log, message);
                    }
                    
                    Thread.sleep(sleepTimeInMs);
                }
            }
        }
    }
    
    
    /*
     * Estimates the "weight" (a unitless value) of the given item.  This is
     * counted as 1 per content and metadata file in the item, plus 100 per
     * gigabyte of streamed data (so that files of 1GB or more cause the batch
     * to end).
     */
    private final int weight(final BulkImportItem item)
    {
        int result = 0;
        
        for (final BulkImportItem.Version version : item.getVersions())
        {
            if (version.hasContent())
            {
                result++;
                
                if (!version.contentIsInPlace())
                {
                    result += (int)((float)item.sizeInBytes() / ONE_GIGABYTE * 100);
                }
            }

            if (version.hasMetadata()) result++;
        }

        return(result);
    }
        
        
    private final class BatchImportJob
        implements Runnable
    {
        private final Batch batch;
        
        public BatchImportJob(final Batch batch)
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
                
                if (rootCause instanceof InterruptedException ||
                    rootCause instanceof ClosedByInterruptException ||
                    "com.hazelcast.core.RuntimeInterruptedException".equals(rootCauseClassName))  // For compatibility across 4.x *sigh*
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
