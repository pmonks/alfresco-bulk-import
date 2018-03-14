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

package org.alfresco.extension.bulkimport;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Interface defining which information can be obtained from the Bulk Filesystem Import engine.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public interface BulkImportStatus
{
    // Target counters
    public final static String TARGET_COUNTER_BATCHES_SUBMITTED            = "Batches submitted";
    public final static String TARGET_COUNTER_BATCHES_COMPLETE             = "Batches completed";
    public final static String TARGET_COUNTER_NODES_IMPORTED               = "Nodes imported";
    public final static String TARGET_COUNTER_IN_PLACE_CONTENT_LINKED      = "In place content linked";
    public final static String TARGET_COUNTER_CONTENT_STREAMED             = "Content streamed";
    public final static String TARGET_COUNTER_BYTES_IMPORTED               = "Bytes imported";
    public final static String TARGET_COUNTER_VERSIONS_IMPORTED            = "Versions imported";
    public final static String TARGET_COUNTER_ASPECTS_ASSOCIATED           = "Aspects associated";
    public final static String TARGET_COUNTER_METADATA_PROPERTIES_IMPORTED = "Metadata properties imported";
    public final static String TARGET_COUNTER_NODES_SKIPPED                = "Nodes skipped";

    
    public final static String[] DEFAULT_TARGET_COUNTERS = { TARGET_COUNTER_BATCHES_SUBMITTED,
                                                             TARGET_COUNTER_BATCHES_COMPLETE,
                                                             TARGET_COUNTER_NODES_IMPORTED,
                                                             TARGET_COUNTER_IN_PLACE_CONTENT_LINKED,
                                                             TARGET_COUNTER_CONTENT_STREAMED,
                                                             TARGET_COUNTER_BYTES_IMPORTED,
                                                             TARGET_COUNTER_VERSIONS_IMPORTED,
                                                             TARGET_COUNTER_ASPECTS_ASSOCIATED,
                                                             TARGET_COUNTER_METADATA_PROPERTIES_IMPORTED,
                                                             TARGET_COUNTER_NODES_SKIPPED };
    
    /**
     * @return The userId of the person who initiatied the import <i>(will be null if an import has never been run)</i>.
     */
    String getInitiatingUserId();

    /**
     * @return The name of the source used for the active (or previous) import <i>(will be null if an import has never been run)</i>.
     */
    String getSourceName();
    
    /**
     * @return A human-readable textual representation of the parameters that source received <i>(will be null if an import has never been run)</i>.
     */
    Map<String, String> getSourceParameters();
    
    /**
     * @return The path of the target space in the repository <i>(will be null if an import has never been run)</i>.
     */
    String getTargetPath();
    
    /**
     * @return A human-readable textual representation of the current processing state of the import <i>(will not be null)</i>.
     */
    String getProcessingState();

    /**
     * State query methods, as per this state table:
     * 
     * Major state       Minor states
     * -----------       ------------
     * In progress       Scanning
     *                   !Scanning
     *                   Paused
     *                   Stopping
     * !In progress      Never run
     *                   Succeeded
     *                   Failed
     *                   Stopped
     */
    boolean inProgress();
    boolean isScanning();
    boolean isPaused();
    boolean isStopping();
    boolean neverRun();
    boolean succeeded();
    boolean failed();
    boolean stopped();

    /**
     * @return True if an in-place import was possible for this import.  Result is undefined if <code>neverRun()</code> is true.
     */
    boolean inPlaceImportPossible();
    
    /**
     * @return True if this is a dry run.  Result is undefined if <code>neverRun()</code> is true.
     */
    boolean isDryRun();
    
    /**
     * @return The start date of the import <i>(will be null if an import has never been run)</i>.
     */
    Date getStartDate();
    
    /**
     * @return The date scanning ended for this the import <i>(will be null if an import has not yet completed)</i>.
     */
    Date getScanEndDate();
    
    /**
     * @return The end date of the import <i>(will be null if an import has not yet completed)</i>.
     */
    Date getEndDate();
    
    /**
     * @return The duration, in nanoseconds, of the import <i>(will be null if an import has never been run)</i>.
     */
    Long getDurationInNs();
    
    /**
     * @return The duration, in a human-readable textual representation, of the import <i>(will be null if an import has never been run)</i>.
     */
    String getDuration();
    
    /**
     * @return The scan duration, in nanoseconds, of the import <i>(will be null if an import has never been run)</i>.
     */
    Long getScanDurationInNs();
    
    /**
     * @return The scan duration, in a human-readable textual representation, of the import <i>(will be null if an import has never been run)</i>.
     */
    String getScanDuration();
    
    /**
     * @return The estimated remaining duration, in nanoseconds, of the current import <i>(will be null if an import is
     * not currently in progress, or if scanning is still under way - estimates cannot be made until scanning is complete)</i>.
     */
    Long getEstimatedRemainingDurationInNs();
    
    /**
     * @return The estimated remaining duration, in a human-readable textual representation, of the current import
     * <i>(will be null if an import is not currently in progress, or if scanning is still under way - estimates cannot
     * be made until scanning is complete)</i>.
     */
    String getEstimatedRemainingDuration();
    
    /**
     * @return The last exception thrown by the import tool <i>(will be null if an import has never been run, or if an exception was not thrown)</i>.
     */
    Throwable getLastException();
    
    /**
     * @return A human-readable textual representation of the last exception thrown by the import tool <i>(will be null if an import has never been run, or if an exception was not thrown)</i>.
     */
    String getLastExceptionAsString();

    /**
     * @return The batch weight used for the last import. Result is undefined if <code>neverRun()</code> is true.
     */
    long getBatchWeight();

    /**
     * @return The number of queued batches, waiting for an available worker thread (0 if an import isn't in progress, or if the multi-threaded phase hasn't been reached).
     */
    int getQueueSize();

    /**
     * @return The maximum number of queued batches allowed.
     */
    int getQueueCapacity();

    /**
     * @return The number of active threads (0 if an import isn't in progress).
     */
    int getNumberOfActiveThreads();
    
    /**
     * @return The total number of threads (0 if an import isn't in progress).
     */
    int getTotalNumberOfThreads();
    
    /**
     * @return A human-readable textual representation of the directory currently being scanned <i>(will be null if an import is not in progress, or scanning has completed)</i>.
     */
    String getCurrentlyScanning();
    
    /**
     * @return A human-readable textual representation of the item currently being imported <i>(will be null if an import has never been run, or an import is not in progress)</i>.
     */
    String getCurrentlyImporting();

    /**
     * @return The source counter names, in sorted order <i>(may be null or empty)<i>.
     */
    Set<String> getSourceCounterNames();
    
    /**
     * @param counterName The name of the source counter to retrieve <i>(must not be null, empty or blank)</i>.
     * @return The current value of that counter <i>(will be null if an import isn't in process, or if the counterName doesn't exist)</i>.
     */
    Long getSourceCounter(String counterName);
    
    /**
     * @param counterName The name of the source counter for which to retrieve the average rate per second <i>(must not be null, empty or blank)</i>.
     * @return The average rate of that source counter per second, for the elapsed duration of this import <i>(will be null if an import isn't in process, or if the counterName doesn't exist)</i>.
     */
    Float getSourceCounterRate(String counterName);
    
    /**
     * @param counterName The name of the source counter for which to retrieve the average rate <i>(must not be null, empty or blank)</i>.
     * @param units       The TimeUnits to calculate the rate for <i>(must not be null)</i>.
     * @return The average rate of that source counter, for the elapsed duration of this import <i>(will be null if an import isn't in process, or if the counterName doesn't exist)</i>.
     */
    Float getSourceCounterRate(String counterName, TimeUnit units);
    
    /**
     * @return The target counter names, in sorted order <i>(may be null or empty)<i>.
     */
    Set<String> getTargetCounterNames();
    
    /**
     * @param counterName The name of the target counter to retrieve <i>(must not be null, empty or blank)</i>.
     * @return The current value of that counter <i>(will be null if an import isn't in process, or if the counterName doesn't exist)</i>.
     */
    Long getTargetCounter(String counterName);

    /**
     * @param counterName The name of the target counter for which to retrieve the average rate per second <i>(must not be null, empty or blank)</i>.
     * @return The average rate of that source counter per second, for the elapsed duration of this import <i>(will be null if an import isn't in process, or if the counterName doesn't exist)</i>.
     */
    Float getTargetCounterRate(String counterName);

    /**
     * @param counterName The name of the target counter for which to retrieve the average rate <i>(must not be null, empty or blank)</i>.
     * @param units       The TimeUnits to calculate the rate for <i>(must not be null)</i>.
     * @return The average rate of that source counter, for the elapsed duration of this import <i>(will be null if an import isn't in process, or if the counterName doesn't exist)</i>.
     */
    Float getTargetCounterRate(String counterName, TimeUnit units);
}
