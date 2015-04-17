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


/**
 * This class encapsulates the logic and state required to scan the source
 * and enquee batches of work for the importer thread pool.
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
            importStatus.importStarted(source.getName(),
                                       targetAsPath,
                                       importThreadPool,
                                       batchWeight,
                                       source.inPlaceImportPossible(parameters),
                                       dryRun);
            
            if (log.isTraceEnabled()) log.trace("Initiating scanning...");
            
            // Request the source to scan itself, calling us back with each item
            source.scan(parameters, importStatus, this);
            importStatus.scanningComplete();
            
            // We're done scanning, so submit whatever is left in the final batch...
            if (currentBatch != null)
            {
                importThreadPool.execute(new BatchImport(currentBatch));
                currentBatch = null;
            }
            
            // ...and wait for everything to wrap up
            if (log.isTraceEnabled()) log.trace("Scanning complete. Blocking until completion of import.");
            importThreadPool.await();
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
                 "com.hazelcast.core.RuntimeInterruptedException".equals(rootCauseClassName)))  // Thrown from 4.2 onward...
            {
                // A stop import was requested, so we don't really need to do anything
                if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " was interrupted by a stop request.", t);
            }
            else
            {
                // An unexpected exception occurred during scanning - log it and kill the import
                log.error("Bulk import from '" + source.getName() + "' failed.", t);
                importStatus.unexpectedError(t);
                
                if (log.isDebugEnabled()) log.debug("Shutting down import thread pool and awaiting termination.");
                importThreadPool.shutdownNow();
                
                try
                {
                    importThreadPool.await();
                }
                catch (final InterruptedException ie)
                {
                    // Not much we can do here but log it and keep on truckin'
                    if (log.isWarnEnabled()) log.warn(Thread.currentThread().getName() + " was interrupted while awaiting import thread pool termination.", ie);
                }
            }
        }
        finally
        {
            importStatus.importComplete();
            
            // Release our references to the per-import state, so it can be GCed (this thread object hangs around beyond completion)
            parameters       = null;
            importThreadPool = null;
            currentBatch     = null;
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
                currentBatch         = new ArrayList<BulkImportItem>(batchWeight / 2);  // Make an educated guess as to the size of the batch, recalling that batches are sized based on "weight", not number of items
                weightOfCurrentBatch = 0;
            }

            // Add the new item to the batch
            currentBatch.add(item);
            weightOfCurrentBatch += item.weight();
            
            // If we've got a full batch, enqueue it
            if (weightOfCurrentBatch > batchWeight)
            {
                importThreadPool.execute(new BatchImport(currentBatch));
                currentBatch = null;
            }
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
                pathElements = serviceRegistry.getFileFolderService().getNamePath(null, nodeRef);   // Note: violates issue #132, but allowable in this case since this is a R/O method without an obvious alternative

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
                
                if (importStatus.isStopping() &&
                    (rootCause instanceof InterruptedException ||
                     rootCause instanceof ClosedByInterruptException ||
                     "com.hazelcast.core.RuntimeInterruptedException".equals(rootCauseClassName)))  // For compatibility across 4.x *sigh*
                {
                    // A stop import was requested
                    if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " was interrupted by a stop request.", t);
                }
                else
                {
                    // An unexpected exception during import of the batch - log it and kill the entire import
                    log.error("Bulk import from '" + source.getName() + "' failed.", t);
                    importStatus.unexpectedError(t);
                    
                    if (log.isDebugEnabled()) log.debug("Shutting down import thread pool.");
                    importThreadPool.shutdownNow();
                }
            }
        }
    }
}
