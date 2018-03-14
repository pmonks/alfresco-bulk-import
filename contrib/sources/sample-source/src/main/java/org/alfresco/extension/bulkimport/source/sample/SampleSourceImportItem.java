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

package org.alfresco.extension.bulkimport.source.sample;

import java.math.BigDecimal;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.extension.bulkimport.source.AbstractBulkImportItem;


/**
 * This class represents the <code>BulkImportSource</code> portion of the
 * sample custom bulk import source.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class SampleSourceImportItem
    extends AbstractBulkImportItem<SampleSourceImportItemVersion>
{
    private final static Log log = LogFactory.getLog(SampleSourceImportItem.class);
    

    
    public SampleSourceImportItem(final String  parentPath,
                                  final String  name,
                                  final boolean isDirectory,
                                  final int     numVersions)
    {
        super(name, isDirectory, parentPath, synthesiseVersions(name, isDirectory, numVersions));
    }


    private final static NavigableSet<SampleSourceImportItemVersion> synthesiseVersions(final String name, final boolean isDirectory, final int numVersions)
    {
        NavigableSet<SampleSourceImportItemVersion> result = new TreeSet<>();
        
        // Add at least one version
        result.add(new SampleSourceImportItemVersion(name, isDirectory, BigDecimal.ONE));
        
        if (!isDirectory && numVersions > 1)
        {
            for (int i = 1; i < numVersions; i++)
            {
                result.add(new SampleSourceImportItemVersion(name, isDirectory, BigDecimal.valueOf(i + 1)));
            }
        }
        
        return(result);
    }

}
