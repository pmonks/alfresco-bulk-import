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

package org.alfresco.extension.bulkimport;

import java.util.Date;
import java.util.Set;


/**
 * Interface defining which information can be obtained from the Bulk Filesystem Import engine.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public interface BulkImportStatus
{
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

    boolean inPlaceImportPossible();
    boolean isDryRun();
    
    Date      getStartDate();
    Date      getEndDate();
    Long      getDurationInNs();  // Note: java.lang.Long, _not_ primitive long - may be null
    Throwable getLastException();
    String    getLastExceptionAsString();

    long getBatchWeight();
    
    int  getNumberOfActiveThreads();
    int  getTotalNumberOfThreads();
    
    String getCurrentlyScanning();
    String getCurrentlyImporting();

    // Counters
    Set<String> getSourceCounterNames();  // Returns the counter names in sorted order
    Long        getSourceCounter(String counterName);   // Note: java.lang.Long, _not_ primitive long - may be null
    
    Set<String> getTargetCounterNames();  // Returns the counter names in sorted order
    Long        getTargetCounter(String counterName);   // Note: java.lang.Long, _not_ primitive long - may be null
}
