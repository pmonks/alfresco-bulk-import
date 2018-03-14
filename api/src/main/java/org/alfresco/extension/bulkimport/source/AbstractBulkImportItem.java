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

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.NamespaceService;


/**
 * This class provides some handy default implementations for some of the
 * methods in <code>BulkImportItem</code>.  Its use is optional.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public abstract class AbstractBulkImportItem<T extends BulkImportItemVersion>
    implements BulkImportItem<T>
{
    protected final String          name;
    protected final boolean         isDirectory;
    protected final String          relativePathOfParent;
    protected final NavigableSet<T> versions;
    
    
    protected AbstractBulkImportItem(final String          name,
                                     final boolean         isDirectory,
                                     final String          relativePathOfParent,
                                     final NavigableSet<T> versions)
    {
        if (name == null || name.trim().length() == 0)
        {
            throw new IllegalArgumentException("name cannot be null, empty or blank.");
        }
        
        if (versions == null || versions.size() <= 0)
        {
            throw new IllegalArgumentException("versions cannot be null or empty.");
        }

        this.name                 = name;
        this.isDirectory          = isDirectory;
        this.relativePathOfParent = relativePathOfParent;
        this.versions             = versions;
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getName()
     */
    @Override
    public String getName()
    {
        return(name);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getParentAssoc()
     */
    @Override
    public String getParentAssoc()
    {
        return(ContentModel.ASSOC_CONTAINS.toString());
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getNamespace()
     */
    @Override
    public String getNamespace()
    {
        return(NamespaceService.CONTENT_MODEL_1_0_URI);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#isDirectory()
     */
    @Override
    public boolean isDirectory()
    {
        return(isDirectory);
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getRelativePathOfParent()
     */
    @Override
    public String getRelativePathOfParent()
    {
        return(relativePathOfParent);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getVersions()
     */
    @Override
    public NavigableSet<T> getVersions()
    {
        return(versions);
    }

    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#sizeInBytes()
     */
    @Override
    public long sizeInBytes()
    {
        long                  result   = 0L;
        final NavigableSet<T> versions = getVersions();

        if (versions != null)
        {
            for (final T version : versions)
            {
                if (version.hasContent())
                {
                    result += version.sizeInBytes();
                }
            }
        }
        
        return(result);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#numberOfVersions()
     */
    @Override
    public int numberOfVersions()
    {
        int result = 0;
        
        if (getVersions() != null)
        {
            result = getVersions().size();
        }
        
        return(result);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#numberOfAspects()
     */
    @Override
    public int numberOfAspects()
    {
        int                   result   = 0;
        final NavigableSet<T> versions = getVersions();

        if (versions != null)
        {
            for (final T version : versions)
            {
                if (version.hasMetadata())
                {
                    result += version.getAspects().size();
                }
            }
        }
        
        return(result);
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#numberOfMetadataProperties()
     */
    @Override
    public int numberOfMetadataProperties()
    {
        int                   result   = 0;
        final NavigableSet<T> versions = getVersions();

        if (versions != null)
        {
            for (final T version : versions)
            {
                if (version.hasMetadata())
                {
                    result += version.getMetadata().size();
                }
            }
        }
        
        return(result);
    }

    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        final StringBuilder   result       = new StringBuilder();
        final int             versionCount = numberOfVersions();
        final NavigableSet<T> versions     = getVersions();
        
        result.append(getName() + " (" + versionCount + " version" + (versionCount != 1 ? "s)" : ")") + ":");

        if (versions != null)
        {
            for (final T version : versions)
            {
                result.append("\n\t");
                result.append(String.valueOf(version));
            }
        }
        
        return(result.toString());
    }
}
