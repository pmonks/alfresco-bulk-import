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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportItemVersion;


/**
 * This class represents a single batch of items that need to be imported.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class Batch
    implements Iterable<BulkImportItem<BulkImportItemVersion>>
{
    private final int                                         number;
    private final List<BulkImportItem<BulkImportItemVersion>> contents;

    public Batch(final int                                         number,
                 final List<BulkImportItem<BulkImportItemVersion>> contents)
    {
        if (number <= 0)
        {
            throw new IllegalArgumentException("Invalid batch number: " + number);
        }
        
        if (contents == null || contents.size() <= 0)
        {
            throw new IllegalArgumentException("Batch #" + number + " is empty.");
        }
        
        this.number   = number;
        this.contents = contents;
    }
    

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<BulkImportItem<BulkImportItemVersion>> iterator()
    {
        return(Collections.unmodifiableList(contents).iterator());
    }
    

    /**
     * @return The number of this batch.
     */
    public int getNumber()
    {
        return(number);
    }
    

    /**
     * @return The contents of this batch.
     */
    public List<BulkImportItem<BulkImportItemVersion>> getContents()
    {
        return(Collections.unmodifiableList(contents));
    }
    

    /**
     * @return The size of (number of items in) this batch.
     */
    public int size()
    {
        return(contents.size());
    }
    
    
    /**
     * @return The size of the batch, in bytes. Will usually be 0 for items that represent directories.
     */
    public long sizeInBytes()
    {
        long result = 0;
        
        for (final BulkImportItem<BulkImportItemVersion> item : contents)
        {
            result += item.sizeInBytes();
        }
        
        return(result);
    }
    

    /**
     * @return The number of versions in this batch.
     */
    public int numberOfVersions()
    {
        int result = 0;
        
        for (final BulkImportItem<BulkImportItemVersion> item : contents)
        {
            int numberOfVersions = item.numberOfVersions();
            
            // Items with only one "version" don't get counted
            if (numberOfVersions > 1)
            {
                result += numberOfVersions;
            }
        }
        
        return(result);
    }
    
    
    /**
     * @return The number of aspects in this batch.
     */
    public int numberOfAspects()
    {
        int result = 0;
        
        // Items with only one "version" don't get counted
        for (final BulkImportItem<BulkImportItemVersion> item : contents)
        {
            result += item.numberOfAspects();
        }
        
        return(result);
    }
    
    
    /**
     * @return The number of properties in this batch.
     */
    public long numberOfMetadataProperties()
    {
        long result = 0;
        
        for (final BulkImportItem<BulkImportItemVersion> item : contents)
        {
            result += item.numberOfMetadataProperties();
        }
        
        return(result);
    }
    
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        int size = size();
        
        return("Batch #" + number + " (" + size + " item" + (size == 1 ? "" : "s") + ")");
    }
}
