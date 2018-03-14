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

/**
 * This class provides some handy default implementations for some of the
 * methods in <code>BulkImportItem.Version</code>.  Its use is optional.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public abstract class AbstractBulkImportItemVersion<C, M>
    implements BulkImportItemVersion,
               Comparable<AbstractBulkImportItemVersion<C, M>>
{
    protected final String     type;
    protected final BigDecimal versionNumber;
    
    protected C contentReference;
    protected M metadataReference;
    
    
    protected AbstractBulkImportItemVersion(final String     type,
                                            final BigDecimal versionNumber)
    {
        // PRECONDITIONS
        if (type          == null || type.trim().length() <= 0) throw new IllegalArgumentException("type cannot be null, empty or blank.");
        if (versionNumber == null)                              throw new IllegalArgumentException("versionNumber cannot be null.");
        
        // Body
        this.type          = type;
        this.versionNumber = versionNumber;
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#getType()
     */
    @Override
    public String getType()
    {
        return(type);
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#getVersionNumber()
     */
    @Override
    public BigDecimal getVersionNumber()
    {
        return(versionNumber);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#getVersionComment()
     */
    @Override
    public String getVersionComment()
    {
        return(null);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#getAspects()
     */
    @Override
    public Set<String> getAspects()
    {
        return(null);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#contentIsInPlace()
     */
    @Override
    public boolean contentIsInPlace()
    {
        return(false);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#hasContent()
     */
    @Override
    public boolean hasContent()
    {
        return(contentReference != null);
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#hasMetadata()
     */
    @Override
    public boolean hasMetadata()
    {
        return(metadataReference != null);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#getContentSource()
     */
    @Override
    public String getContentSource()
    {
        return(String.valueOf(contentReference));
    }

    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#getMetadataSource()
     */
    @Override
    public String getMetadataSource()
    {
        return(String.valueOf(metadataReference));
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#getMetadata()
     */
    @Override
    public Map<String, Serializable> getMetadata()
    {
        return(null);
    }
    
    
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final AbstractBulkImportItemVersion<C, M> other)
    {
        if (this.versionNumber == null && other.versionNumber == null) return(0);
        if (this.versionNumber == null) return(1);
        if (other.versionNumber == null) return(-1);
        return(this.versionNumber.compareTo(other.versionNumber));
    }
    

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) return(true);
        if (!this.getClass().equals(other.getClass())) return(false);
        if (!(other instanceof AbstractBulkImportItemVersion)) return(false);
        
        @SuppressWarnings("unchecked")
        AbstractBulkImportItemVersion<C, M> otherVersion = (AbstractBulkImportItemVersion<C, M>)other;
        return(compareTo(otherVersion) == 0);
    }
    

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return(versionNumber.hashCode());
    }
    
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return((VERSION_HEAD.equals(versionNumber) ? "HEAD" : ("v" + String.valueOf(versionNumber))) + ": " +
               (hasContent() ? "<content>" : "<no content>") + " " + (hasMetadata() ? "<metadata>" : "<no metadata>"));
    }
}
