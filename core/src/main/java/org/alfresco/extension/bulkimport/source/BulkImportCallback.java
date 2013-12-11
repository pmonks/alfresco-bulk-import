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

package org.alfresco.extension.bulkimport.source;

/**
 * This interface exposes a callback into the bulk importer for pluggable source implementations.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public interface BulkImportCallback
{
    /**
     * Enqueues the given importable item for import.
     * 
     * Notes:
     * <ol>
     * <li>This method is thread safe, however due to the requirement to ensure
     * ordered enqueuing of dependent items, it is not recommended to qneueue
     * items from multiple threads.</li>
     * <li>This method may block.</li>
     * <li>If interrupted, the caller of this method should let the
     * <code>InterruptedException</code> percolate up the call stack <b>without
     * catching it</b>. This is critically important to ensure stopped imports
     * stop in a timely fashion.</li>
     * </ol>
     * 
     * @param item The item to enqueue <i>(must not be null)</i>.
     * @throws InterruptedException If the thread is interrupted.
     */
    public void enqueue(BulkImportItem item)
        throws InterruptedException;
}
