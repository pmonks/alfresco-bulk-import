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

package org.alfresco.extension.bulkimport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * Interface defining which information can be obtained from the Bulk Filesystem Import engine.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public interface BulkImportStatus
{
    // Standard counters
    public final static String TARGET_COUNTER_BATCHES_COMPLETE             = "Batches completed";
    public final static String TARGET_COUNTER_NODES_IMPORTED               = "Nodes imported";
    public final static String TARGET_COUNTER_BYTES_IMPORTED               = "Bytes imported";
    public final static String TARGET_COUNTER_VERSIONS_IMPORTED            = "Versions imported";
    public final static String TARGET_COUNTER_METADATA_PROPERTIES_IMPORTED = "Metadata properties imported";
    
    public final static List<String> DEFAULT_TARGET_COUNTERS = new ArrayList<String>() {
        private static final long serialVersionUID = -1608061226137240446L;

        {
            add(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE);
            add(BulkImportStatus.TARGET_COUNTER_BYTES_IMPORTED);
            add(BulkImportStatus.TARGET_COUNTER_METADATA_PROPERTIES_IMPORTED);
            add(BulkImportStatus.TARGET_COUNTER_NODES_IMPORTED);
            add(BulkImportStatus.TARGET_COUNTER_VERSIONS_IMPORTED);
        }
    };
    
    // General information
    String  getSource();
    String  getTargetSpace();
    
    /*
     * State table:
     * Major state       Minor states
     * -----------       ------------
     * In progress       Scanning
     *                   !Scanning
     *                   Stopping
     * !In progress      Succeeded
     *                   Failed
     *                   Stopped
     */
    boolean inProgress();
    boolean isScanning();
    boolean isStopping();
    boolean succeeded();
    boolean failed();
    boolean stopped();
    
    String getProcessingState();

    boolean inPlaceImportPossible();
    boolean isDryRun();
    
    Date      getStartDate();
    Date      getEndDate();
    Long      getDurationInNs();                    // Note: java.lang.Long, _not_ primitive long - may be null
    Float     getBatchesPerNs();                    // Note: java.lang.Float, _not_ primitive float - may be null
    Float     getBatchesPerSecond();                // Note: java.lang.Float, _not_ primitive float - may be null
    Float     getNodesPerNs();                      // Note: java.lang.Float, _not_ primitive float - may be null
    Float     getNodesPerSecond();                  // Note: java.lang.Float, _not_ primitive float - may be null
    Long      getEstimatedRemainingDurationInNs();  // Note: java.lang.Long, _not_ primitive long - may be null
    Throwable getLastException();
    String    getLastExceptionAsString();

    long getBatchWeight();
    
    int getNumberOfActiveThreads();
    int getTotalNumberOfThreads();
    
    String getCurrentlyScanning();
    String getCurrentlyImporting();

    // Counters
    Set<String> getSourceCounterNames();                // Returns the counter names in sorted order
    Long        getSourceCounter(String counterName);   // Note: java.lang.Long, _not_ primitive long - may be null
    
    Set<String> getTargetCounterNames();                // Returns the counter names in sorted order
    Long        getTargetCounter(String counterName);   // Note: java.lang.Long, _not_ primitive long - may be null
}
