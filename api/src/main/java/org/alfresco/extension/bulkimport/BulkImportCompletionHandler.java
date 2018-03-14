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

/**
 * This interface defines custom processing that can occur at the end of a bulk import.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public interface BulkImportCompletionHandler
{
    /**
     * Called when an import completes, regardless of state (succeeded, failed or stopped).
     * 
     * Notes:
     * <ol>
     * <li>The code must <u>not</u> use any Alfresco repository services whatsoever,
     * as this method is executed on a background thread that runs outside of both
     * an Alfresco authentication context and an Alfresco transaction.</li>
     * <li>The code should complete quickly. Lengthy processing (e.g. RPCs to external
     * services) should be done on a separate background thread.</li>
     * <li>The code should not throw exceptions - any it does throw will be caught, logged,
     * then swallowed (ignored).</li>
     * </ol>
     * 
     * @param status The R/O status object, as at the conclusion of the import <i>(will not be null)</li>.
     */
    void importComplete(BulkImportStatus status);

}
