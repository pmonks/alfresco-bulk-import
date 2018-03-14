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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.alfresco.extension.bulkimport.source.BulkImportSource;

import static java.util.concurrent.TimeUnit.*;
import static org.alfresco.extension.bulkimport.util.LogUtils.*;


/**
 * Thread-safe implementation of Bulk Import Status.
 *
 * @author Peter Monks (pmonks@gmail.com)
 * @see org.alfresco.extension.bulkimport.impl.WritableBulkImportStatus
 */
public class BulkImportStatusImpl
    implements WritableBulkImportStatus
{
    // General information
    private AtomicBoolean                inProgress            = new AtomicBoolean(false);
    private volatile ProcessingState     state                 = ProcessingState.NEVER_RUN;
    private volatile ProcessingState     priorState            = state;
    private String                       initiatingUserId      = null;
    private BulkImportSource             source                = null;
    private String                       targetSpace           = null;
    private boolean                      inPlaceImportPossible = false;
    private boolean                      isDryRun              = false;
    private Date                         startDate             = null;
    private Date                         scanEndDate           = null;
    private Date                         endDate               = null;
    private Long                         startNs               = null;
    private Long                         endScanNs             = null;
    private Long                         endNs                 = null;
    private Throwable                    lastException         = null;
    private String                       currentlyScanning     = null;
    private String                       currentlyImporting    = null;
    private long                         batchWeight           = 0;
    private BulkImportThreadPoolExecutor threadPool            = null;
    
    // Counters
    private ConcurrentMap<String, AtomicLong> sourceCounters = new ConcurrentHashMap<>(16);  // Start with a reasonable number of source counter slots
    private ConcurrentMap<String, AtomicLong> targetCounters = new ConcurrentHashMap<>(16);  // Start with a reasonable number of target counter slots
    
    // Public methods
    @Override public String              getInitiatingUserId()   { return(initiatingUserId); };
    @Override public String              getSourceName()         { String              result = null; if (source != null) result = source.getName();       return(result); }
    @Override public Map<String, String> getSourceParameters()   { Map<String, String> result = null; if (source != null) result = source.getParameters(); return(result); }
    @Override public String              getTargetPath()         { return(targetSpace); }
    @Override public boolean             inProgress()            { return(ProcessingState.SCANNING.equals(state) || ProcessingState.IMPORTING.equals(state) || ProcessingState.PAUSED.equals(state) || ProcessingState.STOPPING.equals(state)); }
    @Override public boolean             isScanning()            { return(ProcessingState.SCANNING.equals(state)); }
    @Override public boolean             isPaused()              { return(ProcessingState.PAUSED.equals(state)); }
    @Override public boolean             isStopping()            { return(ProcessingState.STOPPING.equals(state)); }
    @Override public boolean             neverRun()              { return(ProcessingState.NEVER_RUN.equals(state)); }
    @Override public boolean             succeeded()             { return(ProcessingState.SUCCEEDED.equals(state)); }
    @Override public boolean             failed()                { return(ProcessingState.FAILED.equals(state)); }
    @Override public boolean             stopped()               { return(ProcessingState.STOPPED.equals(state)); }
    @Override public boolean             inPlaceImportPossible() { return(inPlaceImportPossible); }
    @Override public boolean             isDryRun()              { return(isDryRun); }
    @Override public Date                getStartDate()          { return(copyDate(startDate)); }
    @Override public Date                getScanEndDate()        { return(copyDate(scanEndDate)); }
    @Override public Date                getEndDate()            { return(copyDate(endDate)); }
    @Override public String              getProcessingState()    { return state.toString(); }

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
    
    @Override
    public String getDuration()
    {
        return(getHumanReadableDuration(getDurationInNs()));
    }
    
    @Override
    public Long getScanDurationInNs()
    {
        Long result = null;
        
        if (startNs != null)
        {
            if (endScanNs != null)
            {
                result = Long.valueOf(endScanNs - startNs);
            }
            else
            {
                result = Long.valueOf(System.nanoTime() - startNs);
            }
        }
        
        return(result);
    }
    
    @Override
    public String getScanDuration()
    {
        return(getHumanReadableDuration(getScanDurationInNs()));
    }

    @Override
    public Long getEstimatedRemainingDurationInNs()
    {
        Long result = null;
        
        // Only calculate an estimated remaining duration once scanning has completed, and if we're not paused
        if (inProgress() && !isScanning() && !isPaused())
        {
            final Float batchesPerNs = getTargetCounterRate(TARGET_COUNTER_BATCHES_COMPLETE, NANOSECONDS);
    
            if (batchesPerNs != null && batchesPerNs.floatValue() > 0.0F && threadPool != null)
            {
                final long batchesInProgress = threadPool.getQueueSize() + threadPool.getActiveCount();
                
                result = (long)(batchesInProgress / batchesPerNs.floatValue());
            }
        }
        
        return(result);
    }
    
    @Override
    public String getEstimatedRemainingDuration()
    {
        return(getHumanReadableDuration(getEstimatedRemainingDurationInNs(), false));
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
    
    @Override public long        getBatchWeight()                                                        { return(batchWeight); }
    @Override public int         getQueueSize()                                                          { return(threadPool == null ? 0 : threadPool.getQueueSize()); }
    @Override public int         getQueueCapacity()                                                      { return(threadPool == null ? 0 : threadPool.getQueueCapacity()); }
    @Override public int         getNumberOfActiveThreads()                                              { return(threadPool == null ? 0 : threadPool.getActiveCount()); }
    @Override public int         getTotalNumberOfThreads()                                               { return(threadPool == null ? 0 : threadPool.getPoolSize()); }
    @Override public String      getCurrentlyScanning()                                                  { return(currentlyScanning); }
    @Override public String      getCurrentlyImporting()                                                 { return(currentlyImporting); }
    @Override public Set<String> getSourceCounterNames()                                                 { return(Collections.unmodifiableSet(new TreeSet<>(sourceCounters.keySet()))); }  // Use TreeSet to sort the set
    @Override public Long        getSourceCounter(final String counterName)                              { return(sourceCounters.get(counterName) == null ? null : sourceCounters.get(counterName).get()); }
    @Override public Float       getSourceCounterRate(final String counterName)                          { return(calculateRate(getSourceCounter(counterName), getScanDurationInNs(), TimeUnit.SECONDS)); }
    @Override public Float       getSourceCounterRate(final String counterName, final TimeUnit timeUnit) { return(calculateRate(getSourceCounter(counterName), getScanDurationInNs(), timeUnit)); }
    @Override public Set<String> getTargetCounterNames()                                                 { return(Collections.unmodifiableSet(new TreeSet<>(targetCounters.keySet()))); }  // Use TreeSet to sort the set
    @Override public Long        getTargetCounter(String counterName)                                    { return(targetCounters.get(counterName) == null ? null : targetCounters.get(counterName).get()); }
    @Override public Float       getTargetCounterRate(final String counterName)                          { return(calculateRate(getTargetCounter(counterName), getDurationInNs(), TimeUnit.SECONDS)); }
    @Override public Float       getTargetCounterRate(final String counterName, final TimeUnit timeUnit) { return(calculateRate(getTargetCounter(counterName), getDurationInNs(), timeUnit)); }
    
    @Override
    public void importStarted(final String                       initiatingUserId,
                              final BulkImportSource             source,
                              final String                       targetSpace,
                              final BulkImportThreadPoolExecutor threadPool,
                              final long                         batchWeight,
                              final boolean                      inPlaceImportPossible,
                              final boolean                      isDryRun)
    {
        if (!inProgress.compareAndSet(false, true))
        {
            throw new IllegalStateException("Import already in progress.");
        }
        
        this.state                 = ProcessingState.SCANNING;
        this.initiatingUserId      = initiatingUserId;
        this.source                = source;
        this.targetSpace           = targetSpace;
        this.threadPool            = threadPool;
        this.batchWeight           = batchWeight;
        this.inPlaceImportPossible = inPlaceImportPossible;
        this.isDryRun              = isDryRun;
        
        this.sourceCounters.clear();
        this.targetCounters.clear();
        preregisterTargetCounters(DEFAULT_TARGET_COUNTERS);

        this.currentlyScanning  = null;
        this.currentlyImporting = null;
        
        this.lastException = null;
        
        this.endScanNs   = null;
        this.scanEndDate = null;
        this.endDate     = null;
        this.endNs       = null;
        
        this.startDate = new Date();
        this.startNs   = Long.valueOf(System.nanoTime());
    }
    
    @Override public void scanningComplete() { this.state = ProcessingState.IMPORTING; this.currentlyScanning = null; this.scanEndDate = new Date(); this.endScanNs = Long.valueOf(System.nanoTime()); }
    @Override public void pauseRequested()   { this.priorState = this.state; this.state = ProcessingState.PAUSED; }
    @Override public void resumeRequested()  { this.state = ProcessingState.PAUSED; this.state = this.priorState; }
    @Override public void stopRequested()    { this.state = ProcessingState.STOPPING; }
    
    @Override
    public void importComplete()
    {
        if (!inProgress.compareAndSet(true, false))
        {
            throw new IllegalStateException("Import not in progress.");
        }
        
        this.endNs      = Long.valueOf(System.nanoTime());
        this.endDate    = new Date();
        this.threadPool = null;
        
        if (isStopping())
        {
            this.state = ProcessingState.STOPPED;
        }
        else if (getLastException() != null)
        {
            this.state = ProcessingState.FAILED;
        }
        else
        {
            this.state              = ProcessingState.SUCCEEDED;
            this.currentlyScanning  = null;
            this.currentlyImporting = null;
        }
    }
    
    @Override public void unexpectedError(final Throwable t) { this.lastException = t; }
    @Override public void setCurrentlyScanning(String name)  { this.currentlyScanning = name; }
    @Override public void setCurrentlyImporting(String name) { this.currentlyImporting = name; }
    
    @Override
    public void batchCompleted(final Batch batch)
    {
        incrementTargetCounter(TARGET_COUNTER_BATCHES_COMPLETE);
        incrementTargetCounter(TARGET_COUNTER_NODES_IMPORTED,               batch.size());
        incrementTargetCounter(TARGET_COUNTER_BYTES_IMPORTED,               batch.sizeInBytes());
        incrementTargetCounter(TARGET_COUNTER_VERSIONS_IMPORTED,            batch.numberOfVersions());
        incrementTargetCounter(TARGET_COUNTER_METADATA_PROPERTIES_IMPORTED, batch.numberOfMetadataProperties());
        incrementTargetCounter(TARGET_COUNTER_ASPECTS_ASSOCIATED,           batch.numberOfAspects());
    }
    
    @Override
    public void preregisterSourceCounters(final String[] counterNames)
    {
        if (counterNames != null)
        {
            for (final String counterName : counterNames)
            {
                sourceCounters.putIfAbsent(counterName, new AtomicLong(0));
            }
        }
    }
    
    @Override public void incrementSourceCounter(final String counterName) { incrementSourceCounter(counterName, 1); }
    
    @Override
    public void incrementSourceCounter(final String counterName, final long value)
    {
        final AtomicLong previous = sourceCounters.putIfAbsent(counterName, new AtomicLong(value));
        
        if (previous != null)
        {
            previous.addAndGet(value);
        }
    }
    
    @Override
    public void preregisterTargetCounters(final String[] counterNames)
    {
        if (counterNames != null)
        {
            for (final String counterName : counterNames)
            {
                targetCounters.putIfAbsent(counterName, new AtomicLong(0));
            }
        }
    }
    
    @Override public void incrementTargetCounter(final String counterName) { incrementTargetCounter(counterName, 1); }

    @Override
    public void incrementTargetCounter(final String counterName, final long value)
    {
        final AtomicLong previous = targetCounters.putIfAbsent(counterName, new AtomicLong(value));
        
        if (previous != null)
        {
            previous.addAndGet(value);
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
    
    private final Float calculateRate(final Long counterValue, final Long durationInNs, final TimeUnit timeUnit)
    {
        Float result = null;
        
        if (counterValue != null && durationInNs != null && durationInNs.longValue() > 0L)
        {
            // Alternative to TimeUnit.convert that doesn't lose precision
            result = (counterValue * (float)NANOSECONDS.convert(1, timeUnit)) / (float)durationInNs;
        }
        
        return(result);
    }
    
    // Private enum for tracking current execution state
    private enum ProcessingState
    {
        SCANNING, IMPORTING, PAUSED, STOPPING,  // In-progress states
        NEVER_RUN, SUCCEEDED, FAILED, STOPPED;  // Not in-progress states
        
        @Override
        public String toString()
        {
            String result = null;
            
            switch (this)
            {
                case SCANNING:
                    result = "Scanning";
                    break;
                    
                case IMPORTING:
                    result = "Importing";
                    break;

                case PAUSED:
                    result = "Paused";
                    break;
                    
                case STOPPING:
                    result = "Stopping";
                    break;
                    
                case NEVER_RUN:
                    result = "Never run";
                    break;

                case SUCCEEDED:
                    result = "Succeeded";
                    break;
                    
                case FAILED:
                    result = "Failed";
                    break;
                    
                case STOPPED:
                    result = "Stopped";
                    break;
            }
            
            return(result);
        }        
    }
    
}
