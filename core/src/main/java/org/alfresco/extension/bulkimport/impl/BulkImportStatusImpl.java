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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Thread-safe implementation of Bulk Import Status.
 *
 * @author Peter Monks (pmonks@gmail.com)
 * @see org.alfresco.extension.bulkimport.impl.WriteableBulkImportStatus
 */
public class BulkImportStatusImpl
    implements WritableBulkImportStatus
{
    // General information
    private AtomicBoolean                inProgress            = new AtomicBoolean(false);
    private ProcessingState              state                 = ProcessingState.NEVER_RUN;
    private String                       source                = null;
    private String                       targetSpace           = null;
    private boolean                      inPlaceImportPossible = false;
    private boolean                      isDryRun              = false;
    private Date                         startDate             = null;
    private Date                         endDate               = null;
    private Long                         startNs               = null;
    private Long                         endNs                 = null;
    private Throwable                    lastException         = null;
    private String                       currentlyScanning     = null;
    private String                       currentlyImporting    = null;
    private long                         batchWeight           = 0;
    private BulkImportThreadPoolExecutor threadPool            = null;
    
    // Read-side information
    private ConcurrentMap<String, AtomicLong> sourceCounters = new ConcurrentHashMap<String, AtomicLong>(16);  // Start with a reasonable number of source counter slots
    private ConcurrentMap<String, AtomicLong> targetCounters = new ConcurrentHashMap<String, AtomicLong>(16);  // Start with a reasonable number of target counter slots

    
    // Public methods
    @Override public String  getSource()             { return(source); }
    @Override public String  getTargetSpace()        { return(targetSpace); }
    @Override public boolean inProgress()            { return(ProcessingState.SCANNING.equals(state) || ProcessingState.IMPORTING.equals(state) || ProcessingState.STOPPING.equals(state)); }
    @Override public boolean isScanning()            { return(ProcessingState.SCANNING.equals(state)); }
    @Override public boolean isStopping()            { return(ProcessingState.STOPPING.equals(state)); }
    @Override public boolean succeeded()             { return(ProcessingState.SUCCEEDED.equals(state)); }
    @Override public boolean failed()                { return(ProcessingState.FAILED.equals(state)); }
    @Override public boolean stopped()               { return(ProcessingState.STOPPED.equals(state)); }
    @Override public boolean inPlaceImportPossible() { return(inPlaceImportPossible); }
    @Override public boolean isDryRun()              { return(isDryRun); }
    @Override public Date    getStartDate()          { return(copyDate(startDate)); }
    @Override public Date    getEndDate()            { return(copyDate(endDate)); }
    @Override public String  getProcessingState()    { return(state.name()); }
    
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
    
    @Override public long        getBatchWeight()                     { return(batchWeight); }
    @Override public int         getNumberOfActiveThreads()           { return(threadPool == null ? 0 : threadPool.getActiveCount()); }
    @Override public int         getTotalNumberOfThreads()            { return(threadPool == null ? 0 : threadPool.getPoolSize()); }
    @Override public String      getCurrentlyScanning()               { return(currentlyScanning); }
    @Override public String      getCurrentlyImporting()              { return(currentlyImporting); }
    @Override public Set<String> getSourceCounterNames()              { return(Collections.unmodifiableSet(new TreeSet<String>(sourceCounters.keySet()))); }  // Use TreeSet to sort the set
    @Override public Long        getSourceCounter(String counterName) { return(sourceCounters.get(counterName) == null ? null : sourceCounters.get(counterName).get()); }
    @Override public Set<String> getTargetCounterNames()              { return(Collections.unmodifiableSet(new TreeSet<String>(targetCounters.keySet()))); }  // Use TreeSet to sort the set
    @Override public Long        getTargetCounter(String counterName) { return(targetCounters.get(counterName) == null ? null : targetCounters.get(counterName).get()); }

    @Override
    public void importStarted(final String                       sourceName,
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
        this.source                = sourceName;
        this.targetSpace           = targetSpace;
        this.threadPool            = threadPool;
        this.batchWeight           = batchWeight;
        this.inPlaceImportPossible = inPlaceImportPossible;
        this.isDryRun              = isDryRun;
        
        this.sourceCounters.clear();
        this.targetCounters.clear();

        this.currentlyScanning  = null;
        this.currentlyImporting = null;
        
        this.lastException = null;
        
        this.endDate = null;
        this.endNs   = null;
        
        this.startDate = new Date();
        this.startNs   = Long.valueOf(System.nanoTime());
    }
    
    @Override public void scanningComplete() { this.state = ProcessingState.IMPORTING; }
    @Override public void stopRequested()    { this.state = ProcessingState.STOPPING; }
    
    @Override
    public void importComplete()
    {
        if (!inProgress.compareAndSet(true, false))
        {
            throw new IllegalStateException("Import not in progress.");
        }
        
        this.endNs   = new Long(System.nanoTime());
        this.endDate = new Date();

        this.threadPool = null;
        
        if (ProcessingState.STOPPING.equals(this.state))
        {
            this.state = ProcessingState.STOPPED;
        }
        else if (this.lastException != null)
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
        incrementTargetCounter("Batches completed");
        incrementTargetCounter("Nodes imported successfully", batch.size());
        
        //####TODO: add more interesting stats here...
    }
    
    @Override
    public void preregisterSourceCounters(final List<String> counterNames)
    {
        if (counterNames != null)
        {
            for (final String counterName : counterNames)
            {
                sourceCounters.putIfAbsent(counterName, new AtomicLong(0));
            }
        }
    }
    
    @Override public void preregisterSourceCounters(final String[] counterNames) { preregisterSourceCounters(Arrays.asList(counterNames)); }
    @Override public void incrementSourceCounter(final String counterName) { incrementSourceCounter(counterName, 1); }
    @Override public void incrementSourceCounter(final String counterName, final long value) { sourceCounters.putIfAbsent(counterName, new AtomicLong(value)); }
    
    @Override
    public void preregisterTargetCounters(final List<String> counterNames)
    {
        if (counterNames != null)
        {
            for (final String counterName : counterNames)
            {
                targetCounters.putIfAbsent(counterName, new AtomicLong(0));
            }
        }
    }
    
    @Override public void preregisterTargetCounters(final String[] counterNames) { preregisterTargetCounters(Arrays.asList(counterNames)); }
    @Override public void incrementTargetCounter(final String counterName) { incrementTargetCounter(counterName, 1); }
    @Override public void incrementTargetCounter(final String counterName, final long value) { targetCounters.putIfAbsent(counterName, new AtomicLong(value)); }
    
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
    
    // Private enum for tracking current execution state
    private enum ProcessingState
    {
        SCANNING, IMPORTING, STOPPING,         // In-progress states
        NEVER_RUN, SUCCEEDED, FAILED, STOPPED  // Not in-progress states
    }
    
}
