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

import java.util.NavigableSet;


/**
 * This interface represents a "bulk import item" - a series of versions that
 * will be imported as a single node (content OR space) into the repository.
 * 
 * Notes:
 * <ol>
 * <li>Implementations of this interface must be as lightweight as possible, as
 * there may be many instances of these in memory at once.  For this reason
 * it is recommended that implementations only maintain references to the underlying
 * content and metadata sources (e.g. <code>java.io.File</code> objects, rather
 * than <code>InputStreams</code> or a <code>Map</code> containing metadata).</li>
 * <li>The import tool will call the putContent and getMetadata methods at most once,
 * allowing implementations to convert those references to actual values exactly once,
 * without retaining those values.</li>
 * </ol>
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public interface BulkImportItem<T extends BulkImportItemVersion>
{
    /**
     * @return The path (delimited by '/' characters), relative to the root of the source, of this item's parent <i>(null indicates that the parent is the root of the source)</i>.
     */
    String getRelativePathOfParent();
    
    /**
     * @return The parent association type to use for this item <i>(may be null)</i>.
     */
    String getParentAssoc();
    
    /**
     * @return The namespace for the item <i>(may be null)</i>.
     */
    String getNamespace();
    
    /**
     * @return The name of the item as it is / should be in the repository <i>(must not be null, empty or blank, and must meet Alfresco's naming rules for nodes)</i>.
     */
    String getName();
    
    /**
     * @return True if this item is a directory (cm:folder or descendent type), false otherwise.
     */
    boolean isDirectory();
    
    /**
     * @return The size (in bytes) of this item, defined as the size of all content (_not_ metadata!) files in all versions (will usually be 0 for directories).
     */
    long sizeInBytes();
    
    /**
     * @return The number of versions in this item. Should normally be implemented as return(getVersions().size()).
     */
    int numberOfVersions();
    
    /**
     * @return The number of aspects in this item, in all versions.
     */
    int numberOfAspects();
    
    /**
     * @return The number of metadata properties in this item, in all versions.
     */
    int numberOfMetadataProperties();
    
    /**
     * @return The set of versions comprising this item, sorted by version number <i>(must not be null or empty, and should contain only one entry if this item is a directory)</i>.
     */
    NavigableSet<T> getVersions();
}    
