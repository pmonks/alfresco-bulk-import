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


import org.alfresco.service.cmr.repository.NodeRef;


/**
 * This interface declares the logic for importing a batch into Alfresco.  It
 * should not normally be extended by custom sources.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public interface BatchImporter
{
    /**
     * A method that will import a single batch of <code>BulkImportItem</code>s.
     * 
     * @param userId                The userId with which to run the import <i>(must not be null, empty or blank, and must be a valid Alfresco userId)</i>.
     * @param target                The nodeRef of the target space in which to perform the import <i>(must not be null, and the target space must exist and be writeable)</i>.
     * @param batch                 The batch to import <i>(may be null or empty, though both of those states result in nothing happening)</i>.
     * @param replaceExisting       Flag indicating whether existing nodes are to be replaced or skipped.
     * @param dryRun                Flag indicating that the import should be a "dry run" (nothing written to the repository).
     * @throws InterruptedException If the batch is interrupted during processing.
     */
    public void importBatch(String  userId,
                            NodeRef target,
                            Batch   batch,
                            boolean replaceExisting,
                            boolean dryRun)
        throws InterruptedException;
}
