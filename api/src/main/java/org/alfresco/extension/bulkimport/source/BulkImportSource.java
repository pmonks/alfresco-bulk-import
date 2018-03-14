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

package org.alfresco.extension.bulkimport.source;

import java.util.List;
import java.util.Map;

import org.alfresco.extension.bulkimport.BulkImportCallback;


/**
 * This interface represents a pluggable source for bulk imports.
 * 
 * Notes:
 * <ol>
 * <li>The <code>scan</code> method must frequently check whether the thread
 * has been interrupted, and throw an <code>InterruptedException</code> if so.
 * This should look something like:
 * <blockquote><code>if (Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted.  Terminating early.");</code></blockquote></li>
 * <li></li>
 * </ol>
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public interface BulkImportSource
{
    /**
     * @return The human readable name of this bulk import source <i>(must not be null, empty or blank)</i>.
     */
    String getName();
    
    
    /**
     * @return A description of this bulk import source - may contain HTML tags <i>(may be null, empty or blank)</i>.
     */
    String getDescription();
    
    
    /**
     * @return The parameters used to initialise this import source, as human-readable text <i>(may be null or empty)</i>.
     */
    Map<String, String> getParameters();

    
    /**
     * @return The URI of the Web Script to display in the initiation form, when this source is selected <i>(may be null)</i>.
     */
    String getConfigWebScriptURI();
    
    
    /**
     * Called before anything else, so that the source can validate and parse its parameters.  If the parameters are invalid,
     * sources should throw an <code>IllegalArgumentException</code> with details on which parameters were missing / invalid.
     * 
     * @param status     The status object to use to pre-register any source-side statistics <i>(will not be null)</li>.
     * @param parameters The parameters (if any) provided by the initiator of the import <i>(will not be null, but may be empty)</i>.
     */
    void init(BulkImportSourceStatus status, Map<String, List<String>> parameters);
    
    
    /**
     * Query to determine whether an "in-place" import is possible, given the provided parameters.  Note that this doesn't imply
     * that all content in the source must be imported in-place - that can be decided by a source implementation on a file-by-file
     * basis.  Instead this is indicative of whether any amount of in-place import is possible or not.
     * 
     * @return True if an in-place import is possible with the given parameters, or false otherwise.
     */
    boolean inPlaceImportPossible();
    
    
    /**
     * Called when the folder scanning phase of a bulk import is commenced.  Importable items should be submitted via the callback,
     * and status may (optionally) be provided via the status object.
     * 
     * Notes:
     * <ol>
     * <li>Items must be submitted in dependency order i.e. parents before children.
     * Failure to do so will have a <b>severe</b> impact on performance (due to
     * transactional rollbacks, followed by retries).</li>
     * <li>This code <u>must not</u> use any Alfresco repository services whatsoever,
     * as this method is executed on a background thread that runs outside of both
     * an Alfresco authentication context and an Alfresco transaction.</li>
     * </ol>
     * 
     * @param status     The status object to use to report source-side statistics <i>(will not be null)</li>.
     * @param callback   The callback into the bulk import engine with which to enqueue items discovered <i>(will not be null)</li>.
     * @throws InterruptedException Should be thrown if the thread running the scan is interrupted.
     */
    void scanFolders(BulkImportSourceStatus status, BulkImportCallback callback)
        throws InterruptedException;

    
    /**
     * Called when the file scanning phase of a bulk import is commenced.  Importable items should be submitted via the callback,
     * and status may (optionally) be provided via the status object.
     * 
     * Notes:
     * <ol>
     * <li>This code must <u>not</u> use any Alfresco repository services whatsoever,
     * as this method is executed on a background thread that runs outside of both
     * an Alfresco authentication context and an Alfresco transaction.</li>
     * </ol>
     * 
     * @param status     The status object to use to report source-side statistics <i>(will not be null)</li>.
     * @param callback   The callback into the bulk import engine with which to enqueue items discovered <i>(will not be null)</li>.
     * @throws InterruptedException Should be thrown if the thread running the scan is interrupted.
     */
    void scanFiles(BulkImportSourceStatus status, BulkImportCallback callback)
        throws InterruptedException;
    
}
