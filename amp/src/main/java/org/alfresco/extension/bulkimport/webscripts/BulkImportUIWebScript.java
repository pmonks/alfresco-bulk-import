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

package org.alfresco.extension.bulkimport.webscripts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import org.alfresco.extension.bulkimport.BulkImporter;
import org.alfresco.extension.bulkimport.source.BulkImportSource;


/**
 * Web Script class that invokes a BulkImporter implementation.
 *
 * @author Peter Monks (peter.monks@alfresco.com)
 */
public class BulkImportUIWebScript
    extends DeclarativeWebScript
{
    // Other Web Script URIs
    private final static String WEB_SCRIPT_URI_BULK_IMPORT_STATUS = "/bulk/import/status";
    
    // Attributes
    private final BulkImporter importer;
    
    
    public BulkImportUIWebScript(final BulkImporter importer)
    {
        // PRECONDITIONS
        assert importer != null : "importer must not be null.";
        
        //BODY
        this.importer = importer;
    }
    
    
    /**
     * @see org.springframework.extensions.webscripts.DeclarativeWebScript#executeImpl(org.springframework.extensions.webscripts.WebScriptRequest, org.springframework.extensions.webscripts.Status, org.springframework.extensions.webscripts.Cache)
     */
    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest request, final Status status, final Cache cache)
    {
        Map<String, Object> result = null;
        
        if (importer.getStatus().inProgress())
        {
            // If an import is already in progress, redirect to the status Web Script
            status.setCode(Status.STATUS_MOVED_TEMPORARILY);
            status.setRedirect(true);
            status.setLocation(request.getServiceContextPath() + WEB_SCRIPT_URI_BULK_IMPORT_STATUS);
        }
        else
        {
            // Construct the list of available bulk import sources
            Map<String, BulkImportSource> bulkImportSources = importer.getBulkImportSources();
            
            if (bulkImportSources != null && bulkImportSources.size() > 0)
            {
                List<Map<String, String>> sources = new ArrayList<Map<String, String>>(bulkImportSources.size());
                
                for (final String beanId : bulkImportSources.keySet())
                {
                    BulkImportSource    source      = bulkImportSources.get(beanId);
                    Map<String, String> sourceAsMap = new HashMap<String, String>();
                    
                    sourceAsMap.put("name",               source.getName());
                    sourceAsMap.put("description",        source.getDescription());
                    sourceAsMap.put("beanId",             beanId);
                    sourceAsMap.put("configWebScriptURI", source.getConfigWebScriptURI());
                    
                    sources.add(sourceAsMap);
                }
                
                result = new HashMap<>();
                result.put("sources", sources);
            }
        }
        
        return(result);
    }

}
