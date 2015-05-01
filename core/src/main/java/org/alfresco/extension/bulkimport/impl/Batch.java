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


package org.alfresco.extension.bulkimport.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.alfresco.extension.bulkimport.source.BulkImportItem;

/**
 * This class represents a single batch of items that need to be imported.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public class Batch
    implements Iterable<BulkImportItem>
{
    private final int                  number;
    private final List<BulkImportItem> contents;

    public Batch(final int                  number,
                 final List<BulkImportItem> contents)
    {
        this.number   = number;
        this.contents = contents;
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<BulkImportItem> iterator()
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
    public List<BulkImportItem> getContents()
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
    
}
