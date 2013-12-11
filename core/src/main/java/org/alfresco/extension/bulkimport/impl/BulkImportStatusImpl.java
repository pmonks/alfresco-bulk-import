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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.alfresco.extension.bulkimport.BulkImportStatus;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportSourceStatus;


/**
 * Thread-safe implementation of Bulk Import Status.
 *
 * @author Peter Monks (pmonks@gmail.com)
 * @see org.alfresco.extension.bulkimport.BulkImportStatus
 * @see org.alfresco.extension.bulkimport.source.BulkImportSourceStatus
 */
public class BulkImportStatusImpl
    implements BulkImportStatus,
               BulkImportSourceStatus
{
    // General information
    private AtomicBoolean      inProgress                = new AtomicBoolean(false);
    private ProcessingState    processingState           = ProcessingState.NEVER_RUN;
    private String             sourceDirectory           = null;
    private String             targetSpace               = null;
    private ImportType         importType                = null;
    private Date               startDate                 = null;
    private Date               endDate                   = null;
    private Long               startNs                   = null;
    private Long               endNs                     = null;
    private Throwable          lastException             = null;
    private String             currentFileBeingProcessed = null;
    private AtomicLong         batchWeight               = new AtomicLong();
    private ThreadPoolExecutor threadPool                = null;
    private AtomicLong         numberOfBatchesCompleted  = new AtomicLong();
    
    // Read-side information
    private AtomicLong numberOfFoldersScanned                  = new AtomicLong();
    private AtomicLong numberOfFilesScanned                    = new AtomicLong();
    private AtomicLong numberOfUnreadableEntries               = new AtomicLong(); 
    
    private AtomicLong numberOfContentFilesRead                = new AtomicLong();
    private AtomicLong numberOfContentBytesRead                = new AtomicLong();
    
    private AtomicLong numberOfMetadataFilesRead               = new AtomicLong();
    private AtomicLong numberOfMetadataBytesRead               = new AtomicLong();
    
    private AtomicLong numberOfContentVersionFilesRead         = new AtomicLong();
    private AtomicLong numberOfContentVersionBytesRead         = new AtomicLong();
    
    private AtomicLong numberOfMetadataVersionFilesRead        = new AtomicLong();
    private AtomicLong numberOfMetadataVersionBytesRead        = new AtomicLong();
    
    // Write-side information
    private AtomicLong numberOfSpaceNodesCreated               = new AtomicLong();
    private AtomicLong numberOfSpaceNodesReplaced              = new AtomicLong();
    private AtomicLong numberOfSpaceNodesSkipped               = new AtomicLong();
    private AtomicLong numberOfSpacePropertiesWritten          = new AtomicLong();
    
    private AtomicLong numberOfContentNodesCreated             = new AtomicLong();
    private AtomicLong numberOfContentNodesReplaced            = new AtomicLong();
    private AtomicLong numberOfContentNodesSkipped             = new AtomicLong();
    private AtomicLong numberOfContentBytesWritten             = new AtomicLong();
    private AtomicLong numberOfContentPropertiesWritten        = new AtomicLong();
    
    private AtomicLong numberOfContentVersionsCreated          = new AtomicLong();
    private AtomicLong numberOfContentVersionBytesWritten      = new AtomicLong();
    private AtomicLong numberOfContentVersionPropertiesWritten = new AtomicLong();


    // General information
    @Override public boolean         inProgress()         { return(inProgress.get()); }
    @Override public ProcessingState getProcessingState() { return(processingState); }
    @Override public String          getSourceDirectory() { return(sourceDirectory); }
    @Override public String          getTargetSpace()     { return(targetSpace); }
    @Override public ImportType      getImportType()      { return(importType); }
    @Override public Date            getStartDate()       { return(copyDate(startDate)); }
    @Override public Date            getEndDate()         { return(copyDate(endDate)); }
    
    @Override
    public Long getDurationInNs()
    {
        Long result = null;
        
        if (startNs != null)
        {
            if (endNs != null)
            {
                result = Long.valueOf(endNs - startNs);
            }
            else
            {
                result = Long.valueOf(System.nanoTime() - startNs);
            }
        }
        
        return(result);
    }
    
    @Override public Throwable getLastException() { return(lastException); }
    
    @Override
    public String getLastExceptionAsString()
    {
        String result = null;
        
        if (lastException != null)
        {
            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw, true);
            
            lastException.printStackTrace(pw);
            
            pw.flush();
            sw.flush();
            
            result = sw.toString();
        }
        
        return(result);
    }
    
    @Override public long    getBatchWeight()           { return(batchWeight.get()); }
    @Override public int     getNumberOfActiveThreads() { return(threadPool == null ? 1 : threadPool.getActiveCount()); }
    @Override public int     getTotalNumberOfThreads()  { return(threadPool == null ? 1 : threadPool.getPoolSize()); }

    
    @Override public String  getCurrentFileBeingProcessed()                                       { return(currentFileBeingProcessed); }
    public void              setCurrentFileBeingProcessed(final String currentFileBeingProcessed) { this.currentFileBeingProcessed = currentFileBeingProcessed; }
    
    @Override public long getNumberOfBatchesCompleted()       { return(numberOfBatchesCompleted.get()); }
    public void           incrementNumberOfBatchesCompleted() { numberOfBatchesCompleted.incrementAndGet(); }
    
    public void startImport(final String sourceDirectory, final String targetSpace, final ImportType importType, final long batchWeight)
    {
        startImport(sourceDirectory, targetSpace, importType, batchWeight, null);
    }
    
    public void startImport(final String sourceDirectory, final String targetSpace, final ImportType importType, final long batchWeight, final ThreadPoolExecutor threadPool)
    {
        if (!inProgress.compareAndSet(false, true))
        {
            throw new RuntimeException("Import already in progress.");
        }
        
        // General information
        this.processingState           = ProcessingState.RUNNING;
        this.sourceDirectory           = sourceDirectory;
        this.targetSpace               = targetSpace;
        this.importType                = importType;
        this.startDate                 = new Date();
        this.endDate                   = null;
        this.lastException             = null;
        this.currentFileBeingProcessed = null;
        this.batchWeight.set(batchWeight);
        this.threadPool                = threadPool;
        this.numberOfBatchesCompleted.set(0);
        
        // Read-side information
        this.numberOfFoldersScanned.set(1);   // We set this to one to count the initial starting directory (which doesn't otherwise get counted)
        this.numberOfFilesScanned.set(0);
        this.numberOfUnreadableEntries.set(0);
        
        this.numberOfContentFilesRead.set(0);
        this.numberOfContentBytesRead.set(0);
        
        this.numberOfMetadataFilesRead.set(0);
        this.numberOfMetadataBytesRead.set(0);
        
        this.numberOfContentVersionFilesRead.set(0);
        this.numberOfContentVersionBytesRead.set(0);
        
        this.numberOfMetadataVersionFilesRead.set(0);
        this.numberOfMetadataVersionBytesRead.set(0);
        
        // Write-side information
        this.numberOfSpaceNodesCreated.set(0);
        this.numberOfSpaceNodesReplaced.set(0);
        this.numberOfSpaceNodesSkipped.set(0);
        this.numberOfSpacePropertiesWritten.set(0);
        
        this.numberOfContentNodesCreated.set(0);
        this.numberOfContentNodesReplaced.set(0);
        this.numberOfContentNodesSkipped.set(0);
        this.numberOfContentBytesWritten.set(0);
        this.numberOfContentPropertiesWritten.set(0);
        
        this.numberOfContentVersionsCreated.set(0);
        this.numberOfContentVersionBytesWritten.set(0);
        this.numberOfContentVersionPropertiesWritten.set(0);
        
        this.startNs = System.nanoTime();
        this.endNs   = null;
    }
    
    @Override
    public boolean isStopping()
    {
        return(ProcessingState.STOPPING.equals(processingState));
    }
    
    public void stopping()
    {
        processingState = ProcessingState.STOPPING;
    }
    
    public void importSucceeded()
    {
        if (!inProgress.compareAndSet(true, false))
        {
            throw new RuntimeException("Import not in progress.");
        }
        
        endNs            = System.nanoTime();
        endDate          = new Date();
        processingState  = ProcessingState.SUCCESSFUL;
    }
    
    public void importStopped()
    {
        importSucceeded();
        processingState = ProcessingState.STOPPED;
    }
    
    public void importFailed(final Throwable lastException)
    {
        importSucceeded();
        this.lastException   = lastException;
        this.processingState = ProcessingState.FAILED;
    }
    
    
    
    // Read-side information
    @Override public long getNumberOfFoldersScanned()              { return(numberOfFoldersScanned.longValue()); }
    @Override public long getNumberOfFilesScanned()                { return(numberOfFilesScanned.longValue()); }
    @Override public long getNumberOfUnreadableEntries()           { return(numberOfUnreadableEntries.longValue()); }
    
    @Override public long getNumberOfContentFilesRead()            { return(numberOfContentFilesRead.longValue()); }
    @Override public long getNumberOfContentBytesRead()            { return(numberOfContentBytesRead.longValue()); }
    
    @Override public long getNumberOfMetadataFilesRead()           { return(numberOfMetadataFilesRead.longValue()); }
    @Override public long getNumberOfMetadataBytesRead()           { return(numberOfMetadataBytesRead.longValue()); }
    
    @Override public long getNumberOfContentVersionFilesRead()     { return(numberOfContentVersionFilesRead.longValue()); }
    @Override public long getNumberOfContentVersionBytesRead()     { return(numberOfContentVersionBytesRead.longValue()); }
    
    @Override public long getNumberOfMetadataVersionFilesRead()    { return(numberOfMetadataVersionFilesRead.longValue()); }
    @Override public long getNumberOfMetadataVersionBytesRead()    { return(numberOfMetadataVersionBytesRead.longValue()); }
    
    public void incrementImportableItemsRead(final BulkImportItem importableItem, final boolean isDirectory)
    {
        if (importableItem.getHeadRevision().contentFileExists())
        {
            if (!isDirectory)
            {
                numberOfContentFilesRead.incrementAndGet();
                numberOfContentBytesRead.addAndGet(importableItem.getHeadRevision().getContentFileSize());
            }
        }
        
        if (importableItem.getHeadRevision().metadataFileExists())
        {
            numberOfMetadataFilesRead.incrementAndGet();
            numberOfMetadataBytesRead.addAndGet(importableItem.getHeadRevision().getMetadataFileSize());
        }
        
        if (!isDirectory && importableItem.hasVersionEntries())
        {
            for (final BulkImportItem.ContentAndMetadata versionEntry : importableItem.getVersionEntries())
            {
                if (versionEntry.contentFileExists())
                {
                    numberOfContentVersionFilesRead.incrementAndGet();
                    numberOfContentVersionBytesRead.addAndGet(versionEntry.getContentFileSize());
                }
                
                if (versionEntry.metadataFileExists())
                {
                    numberOfMetadataVersionFilesRead.incrementAndGet();
                    numberOfMetadataVersionBytesRead.addAndGet(versionEntry.getMetadataFileSize());
                }
            }
        }
    }
    
    public void incrementNumberOfFilesScanned()
    {
        numberOfFilesScanned.incrementAndGet();
    }
    
    public void incrementNumberOfFoldersScanned()
    {
        numberOfFoldersScanned.incrementAndGet();
    }
    
    public void incrementNumberOfUnreadableEntries()
    {
        numberOfUnreadableEntries.incrementAndGet();
    }
    
    public void incrementImportableItemsSkipped(final BulkImportItem importableItem, final boolean isDirectory)
    {
        if (importableItem.getHeadRevision().contentFileExists())
        {
            long ignored = isDirectory ? numberOfSpaceNodesSkipped.incrementAndGet() : numberOfContentNodesSkipped.incrementAndGet();
        }
        
        // We don't track the number of properties or version entries skipped
    }
    
    
    
    // Write-side information
    @Override public long getNumberOfSpaceNodesCreated()               { return(numberOfSpaceNodesCreated.longValue()); }
    @Override public long getNumberOfSpaceNodesReplaced()              { return(numberOfSpaceNodesReplaced.longValue()); }
    @Override public long getNumberOfSpaceNodesSkipped()               { return(numberOfSpaceNodesSkipped.longValue()); }
    @Override public long getNumberOfSpacePropertiesWritten()          { return(numberOfSpacePropertiesWritten.longValue()); }
    
    @Override public long getNumberOfContentNodesCreated()             { return(numberOfContentNodesCreated.longValue()); }
    @Override public long getNumberOfContentNodesReplaced()            { return(numberOfContentNodesReplaced.longValue()); }
    @Override public long getNumberOfContentNodesSkipped()             { return(numberOfContentNodesSkipped.longValue()); }
    @Override public long getNumberOfContentBytesWritten()             { return(numberOfContentBytesWritten.longValue()); }
    @Override public long getNumberOfContentPropertiesWritten()        { return(numberOfContentPropertiesWritten.longValue()); }
    
    @Override public long getNumberOfContentVersionsCreated()          { return(numberOfContentVersionsCreated.longValue()); }
    @Override public long getNumberOfContentVersionBytesWritten()      { return(numberOfContentVersionBytesWritten.longValue()); }
    @Override public long getNumberOfContentVersionPropertiesWritten() { return(numberOfContentVersionPropertiesWritten.longValue()); }
    
    public void incrementNodesWritten(final BulkImportItem importableItem,
                                      final boolean        isSpace,
                                      final NodeState      nodeState,
                                      final long           numProperties,
                                      final long           numVersionProperties)
    {
        long ignored;
        
        if (importableItem.getHeadRevision().contentFileExists())
        {
            switch (nodeState)
            {
                case SKIPPED:
                    ignored = isSpace ? numberOfSpaceNodesSkipped.incrementAndGet() : numberOfContentNodesSkipped.incrementAndGet();
                    break;
                    
                case CREATED:
                    ignored = isSpace ? numberOfSpaceNodesCreated.incrementAndGet() : numberOfContentNodesCreated.incrementAndGet();
                    numberOfContentBytesWritten.addAndGet(importableItem.getHeadRevision().getContentFileSize());
                    break;
                    
                case REPLACED:
                    ignored = isSpace ? numberOfSpaceNodesReplaced.incrementAndGet() : numberOfContentNodesReplaced.incrementAndGet();
                    numberOfContentBytesWritten.addAndGet(importableItem.getHeadRevision().getContentFileSize());
                    break;
            }
        }

        switch (nodeState)
        {
            case SKIPPED:
                // We don't track the number of properties skipped
                break;
                
            case CREATED:
            case REPLACED:
                ignored = isSpace ? numberOfSpacePropertiesWritten.addAndGet(numProperties) : numberOfContentPropertiesWritten.addAndGet(numProperties);
                break;
        }

        if (!isSpace && importableItem.hasVersionEntries())
        {
            numberOfContentVersionPropertiesWritten.addAndGet(numVersionProperties);
            
            for (final BulkImportItem.ContentAndMetadata versionEntry : importableItem.getVersionEntries())
            {
                if (versionEntry.contentFileExists())
                {
                    switch (nodeState)
                    {
                        case SKIPPED:
                            // We only track the number of items skipped on the read side
                            break;
                            
                        case CREATED:
                        case REPLACED:
                            numberOfContentVersionsCreated.incrementAndGet();
                            numberOfContentVersionBytesWritten.addAndGet(versionEntry.getContentFileSize());
                            break;
                    }
                }
            }
        }
    }
    
    

    
    // Private helper methods
    private final Date copyDate(final Date date)
    {
        // Defensively copy the date to prevent shenanigans.  Immutability ftw...
        Date result = null;
        
        if (date != null)
        {
            result = new Date(date.getTime());
        }
        
        return(result);
    }
    
    
    public enum NodeState { SKIPPED, CREATED, REPLACED };
    
    
}
