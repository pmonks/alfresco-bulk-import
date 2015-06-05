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

import java.util.Iterator;


/**
 * This class provides some handy default implementations for some of the
 * methods in <code>BulkImportItem</code>.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public abstract class AbstractBulkImportItem
    implements BulkImportItem
{
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#sizeInBytes()
     */
    @Override
    public long sizeInBytes()
    {
        long                             result = 0L;
        Iterator<BulkImportItem.Version> iter   = getVersions().iterator();
        
        while (iter.hasNext())
        {
            BulkImportItem.Version version = iter.next();
            
            if (version.hasContent())
            {
                result += version.sizeInBytes();
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
        return(getVersions().size());
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#numberOfAspects()
     */
    @Override
    public int numberOfAspects()
    {
        int                              result = 0;
        Iterator<BulkImportItem.Version> iter   = getVersions().iterator();
        
        while (iter.hasNext())
        {
            BulkImportItem.Version version = iter.next();
            
            if (version.hasMetadata())
            {
                result += version.getAspects().size();
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
        int                              result = 0;
        Iterator<BulkImportItem.Version> iter   = getVersions().iterator();
        
        while (iter.hasNext())
        {
            BulkImportItem.Version version = iter.next();
            
            if (version.hasMetadata())
            {
                result += version.getMetadata().size();
            }
        }
        
        return(result);
    }

}
