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

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.extension.bulkimport.source.BulkImportSource;


/**
 * Interface defining the bulk importer.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public interface BulkImporter
{
    /**
     * @return The bulk import sources installed in this Alfresco instance, with the map key as the sourceBeanId <i>(will not be null or empty)</i>.
     */
    Map<String, BulkImportSource> getBulkImportSources();
    
    
    /**
     * Initiates a bulk import. <code>getStatus().inProgress()</code> must be false prior to calling this method or an exception will be thrown.
     * 
     * @param sourceBeanId The bean id of the bulk import source to read content from <i>(must not be null, empty or blank)</i>.
     * @param parameters   The parameters (if any) provided by the initiator of the import <i>(will not be null, but may be empty)</i>.
     * @param target       The target space to ingest the content into <i>(must not be null and must be a valid, writable space in the repository)</i>.
     */
    void start(String sourceBeanId, Map<String, List<String>> parameters, NodeRef target);
    
    
    /**
     * Initiates a bulk import. <code>getStatus().inProgress()</code> must be false prior to calling this method or an exception will be thrown.
     * 
     * @param source     The source to read content from <i>(must not be null)</i>.
     * @param parameters The parameters (if any) provided by the initiator of the import <i>(will not be null, but may be empty)</i>.
     * @param target     The target space to ingest the content into <i>(must not be null and must be a valid, writable space in the repository)</i>.
     */
    void start(BulkImportSource source, Map<String, List<String>> parameters, NodeRef target);
    
    
    /**
     * Request that the import be paused, if one is in progress (which can be determined by calling <code>getStatus().inProgress()</code>).
     * Note that this is done asynchronously - it may take a little while for in-flight transactions to complete before they are paused.
     */
    void pause();


    /**
     * Request that a paused import be resumed, if one is paused (which can be determined by calling <code>getStatus().isPaused()</code>).
     */
    void resume();


    /**
     * Requests that an import be stopped, if one is in progress (which can be determined by calling <code>getStatus().inProgress()</code>).
     * Note that this is done asynchronously - it may take a little while for in-flight transactions to complete.
     */
    void stop();


    /**
     * @return A status object that describes the current state of the bulk filesystem importer <i>(will not be null)</i>.
     */
    BulkImportStatus getStatus();
}
