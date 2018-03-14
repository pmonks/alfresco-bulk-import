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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ContentWriter;

/**
 * This interface identifies a single version within an importable item.
 * 
 * Invariants:
 * * hasContent() || hasMetadata() == true
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public interface BulkImportItemVersion
{
    /**
     * Constant to represent an "unversioned" (i.e. HEAD) version.
     */
    public final static BigDecimal VERSION_HEAD = BigDecimal.valueOf(Long.MAX_VALUE);
    
    /**
     * @return Returns the decimal representation of the version number <i>(can be null if this version doesn't have a version number)</i>.
     */
    BigDecimal getVersionNumber();

    /**
     * @return Returns the comment for this version, if any <i>(can be null if this version doesn't have a version comment)</i>.
     */
    String getVersionComment();

    /**
     * @return The type for this version <i>(may be null, but must be a valid Alfresco type if not null)</i>.
     */
    String getType();
    
    /**
     * @return The aspect(s) for this version <i>(may be null or empty)</i>.
     */
    Set<String> getAspects();
    
    /**
     * @return True if this version has content, false if not.
     */
    boolean hasContent();
    
    /**
     * @return A human-readable reference to the source of the content (used in error messages) <i>(may be null if hasContent() = false)</i>.
     */
    String getContentSource();
    
    /**
     * @return The size (in bytes) of this version, defined as the size of the content (_not_ metadata!) file (will usually be 0 for directories).
     */
    long sizeInBytes();
    
    /**
     * @return True if the content is already in-place (in which case the content url property must be returned from the <code>getMetadata</code> call).
     */
    boolean contentIsInPlace();
    
    /**
     * Called when the content of this version is ready to be streamed into the repository.
     * 
     * Notes:
     * <ol>
     * <li>This method is not called if contentIsInPlace() returns true.</li>
     * <li>It is the implementer's responsibility to set the MIME type, encoding and/or locale of the content being written.
     * Neither the import tool nor Alfresco will "guess" these values.</li>
     * </ol>
     * 
     * @param writer The ContentWriter to use for this version <i>(will not be null)</i>.
     */
    void putContent(ContentWriter writer);
    
    /**
     * @return True if this version has metadata, false if not.
     */
    boolean hasMetadata();

    /**
     * @return A human-readable reference to the source of the metadata (used in error messages) <i>(may be null if hasMetadata() = false)</i>.
     */
    String getMetadataSource();
    
    /**
     * @return The metadata of this version, if any <i>(may return null or empty)</i>.
     */
    Map<String,Serializable> getMetadata();
}