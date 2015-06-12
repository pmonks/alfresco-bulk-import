/*
 * Copyright (C) 2007-2015 Peter Monks.
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

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.NamespaceService;


/**
 * This class provides some handy default implementations for some of the
 * methods in <code>BulkImportItem.Version</code>.  Its use is optional.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public abstract class AbstractBulkImportItemVersion<C, M>
    implements BulkImportItem.Version,
               Comparable<AbstractBulkImportItemVersion<C, M>>
{
    protected final String     name;
    protected final boolean    isDirectory;
    protected final BigDecimal versionNumber;
    
    protected C contentReference;
    protected M metadataReference;
    
    
    protected AbstractBulkImportItemVersion(final String  name,
                                            final boolean isDirectory,
                                            final String  versionLabel)
    {
        if (name == null || name.trim().length() == 0)
        {
            throw new IllegalArgumentException("name cannot be null, empty or blank.");
        }
        
        this.name        = name;
        this.isDirectory = isDirectory;
        
        if (!VERSION_LABEL_HEAD.equals(versionLabel))
        {
            this.versionNumber = new BigDecimal(versionLabel);
        }
        else
        {
            this.versionNumber = null;
        }
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getType()
     */
    @Override
    public String getType()
    {
        return(isDirectory() ? "cm:folder" : "cm:content");
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getParentAssoc()
     */
    @Override
    public String getParentAssoc()
    {
        return(ContentModel.ASSOC_CONTAINS.toString());
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getNamespace()
     */
    @Override
    public String getNamespace()
    {
        return(NamespaceService.CONTENT_MODEL_1_0_URI);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getName()
     */
    @Override
    public String getName()
    {
        return(name);
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#isDirectory()
     */
    @Override
    public boolean isDirectory()
    {
        return(isDirectory);
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getVersionNumber()
     */
    @Override
    public BigDecimal getVersionNumber()
    {
        return(versionNumber);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getAspects()
     */
    @Override
    public Set<String> getAspects()
    {
        return(null);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#contentIsInPlace()
     */
    @Override
    public boolean contentIsInPlace()
    {
        return(false);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#hasContent()
     */
    @Override
    public boolean hasContent()
    {
        return(contentReference != null);
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#hasMetadata()
     */
    @Override
    public boolean hasMetadata()
    {
        return(metadataReference != null);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getContentSource()
     */
    @Override
    public String getContentSource()
    {
        return(String.valueOf(contentReference));
    }

    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getMetadataSource()
     */
    @Override
    public String getMetadataSource()
    {
        return(String.valueOf(metadataReference));
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getMetadata()
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
}