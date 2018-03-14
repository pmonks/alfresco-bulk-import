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

import java.util.HashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import org.alfresco.extension.bulkimport.BulkImporter;


/**
 * Web Script class that provides status information on the bulk filesystem import process.
 *
 * @author Peter Monks (peter.monks@alfresco.com)
 */
public class BulkImportStatusWebScript
    extends DeclarativeWebScript
{
    // Output parameters (for Freemarker)
    private final static String RESULT_IMPORT_STATUS = "importStatus";
    
    // Attributes
    private final BulkImporter importer;
    
    
    public BulkImportStatusWebScript(final BulkImporter importer)
    {
        // PRECONDITIONS
        assert importer != null : "importer must not be null.";
        
        //BODY
        this.importer = importer;
    }
    

    /**
     * @see org.alfresco.web.scripts.DeclarativeWebScript#executeImpl(org.alfresco.web.scripts.WebScriptRequest, org.alfresco.web.scripts.Status, org.alfresco.web.scripts.Cache)
     */
    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest request,
                                              final Status           status,
                                              final Cache            cache)
    {
        Map<String, Object> result = new HashMap<>();
        
        cache.setNeverCache(true);
        
        result.put(RESULT_IMPORT_STATUS, importer.getStatus());
        
        return(result);
    }
    
}
