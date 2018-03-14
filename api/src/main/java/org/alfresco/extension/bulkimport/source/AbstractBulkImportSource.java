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

import java.util.List;
import java.util.Map;


/**
 * This class provides some handy default implementations for some of the
 * methods in <code>BulkImportSource</code>.  Its use is optional.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public abstract class AbstractBulkImportSource
    implements BulkImportSource
{
    protected final BulkImportSourceStatus importStatus;
    
    private final String   name;
    private final String   description;
    private final String   configWebScriptUri;
    private final String[] counterNames;
    
    
    protected AbstractBulkImportSource(final BulkImportSourceStatus importStatus,
                                       final String                 name,
                                       final String                 description,
                                       final String                 configWebScriptUri,
                                       final String[]               counterNames)
    {
        this.importStatus       = importStatus;
        this.name               = name;
        this.description        = description;
        this.configWebScriptUri = configWebScriptUri;
        this.counterNames       = counterNames;
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#getName()
     */
    @Override
    public String getName()
    {
        return(name);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#getDescription()
     */
    @Override
    public String getDescription()
    {
        return(description);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#getConfigWebScriptURI()
     */
    @Override
    public String getConfigWebScriptURI()
    {
        return(configWebScriptUri);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#init(org.alfresco.extension.bulkimport.source.BulkImportSourceStatus, java.util.Map)
     */
    @Override
    public void init(BulkImportSourceStatus status, Map<String, List<String>> parameters)
    {
        if (counterNames != null)
        {
            status.preregisterSourceCounters(counterNames);
        }
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#getParameters()
     */
    @Override
    public Map<String, String> getParameters()
    {
        // Default action: no parameters
        return(null);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#inPlaceImportPossible()
     */
    @Override
    public boolean inPlaceImportPossible()
    {
        // Default action: source doesn't support in-place imports
        return(false);
    }

}
