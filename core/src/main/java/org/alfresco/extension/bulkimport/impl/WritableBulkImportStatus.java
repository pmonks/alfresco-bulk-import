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

import java.util.List;

import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportSourceStatus;

/**
 * Defines a fully writable interface to the <code>BulkImportStatus</code>.
 *
 * @author Peter Monks (pmonks@gmail.com)
 * @see org.alfresco.extension.bulkimport.source.BulkImportSourceStatus
 *
 */
public interface WritableBulkImportStatus
    extends BulkImportSourceStatus
{
    void importStarted(String sourceName, String targetSpace, BlockingPausableExecutorService threadPool, long batchWeight, boolean inPlaceImportPossible, boolean isDryRun);
    void scanningComplete();
    void stopRequested();
    void importComplete();
    void unexpectedError(Throwable t);

    void setCurrentlyImporting(String name);
    void batchCompleted(List<BulkImportItem> batch);
    
    void incrementTargetCounter(String counterName);
    void incrementTargetCounter(String counterName, long value);
}
