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

import org.alfresco.extension.bulkimport.source.BulkImportItem;

/**
 * This interface exposes a callback into the bulk importer for pluggable source implementations.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public interface BulkImportCallback
{
    /**
     * Submits the given folder for import.
     * 
     * Notes:
     * <ol>
     * <li>The implementation of this method is synchronized, and therefore
     * thread safe.</li>
     * <li>This method may block (e.g. if the import work queue is full).</li>
     * <li>The caller of this method <b>must</b> let the
     * <code>InterruptedException</code> percolate up the call stack <b>without
     * catching it</b>. This is critically important to ensure manually stopped
     * imports stop in a timely fashion.</li>
     * </ol>
     * 
     * @param item The folder being submitted<i>(must not be null)</i>.
     * @throws InterruptedException If the thread is interrupted.
     */
    @SuppressWarnings("rawtypes")
    void submit(BulkImportItem item)
        throws InterruptedException;
    
}
