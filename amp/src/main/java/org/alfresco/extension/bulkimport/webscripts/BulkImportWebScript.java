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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.extension.bulkimport.BulkImporter;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;
import static org.alfresco.extension.bulkimport.util.Utils.*;


/**
 * Web Script class that invokes a BulkImporter implementation.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public class BulkImportWebScript
    extends DeclarativeWebScript
{
    private final static Log log = LogFactory.getLog(BulkImportWebScript.class);
    
    private final static String DEFAULT_SOURCE_BEAN_ID = "bit.fs.source";
    
    // Other Web Script URIs
    private final static String WEB_SCRIPT_URI_BULK_IMPORT_STATUS = "/bulk/import/status";
    
    // Web Script parameters
    private final static String PARAMETER_TARGET_NODEREF = "targetNodeRef";
    private final static String PARAMETER_TARGET_PATH    = "targetPath";
    private final static String PARAMETER_SOURCE_BEAN_ID = "sourceBeanId";
    private final static String PARAMETER_SUBMIT_BUTTON  = "submit";
    
    // Attributes
    private final ServiceRegistry serviceRegistry;
    private final BulkImporter    importer;
    
    
    public BulkImportWebScript(final ServiceRegistry serviceRegistry,
                               final BulkImporter    importer)
    {
        // PRECONDITIONS
        assert serviceRegistry != null : "serviceRegistry must not be null.";
        assert importer        != null : "importer must not be null.";
        
        //BODY
        this.serviceRegistry = serviceRegistry;
        this.importer        = importer;
    }
    

    /**
     * @see org.springframework.extensions.webscripts.DeclarativeWebScript#executeImpl(org.springframework.extensions.webscripts.WebScriptRequest, org.springframework.extensions.webscripts.Status, org.springframework.extensions.webscripts.Cache)
     */
    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest request, final Status status, final Cache cache)
    {
        Map<String, Object> result = null;
        String targetPath          = null;
        
        cache.setNeverCache(true);
        
        try
        {
            if (!importer.getStatus().inProgress())
            {
                String                    targetNodeRefStr = null;
                NodeRef                   targetNodeRef    = null;
                String                    sourceBeanId     = null;
                String[]                  parameterNames   = request.getParameterNames();
                Map<String, List<String>> parameters       = new HashMap<>();
                
                // Retrieve all parameters POSTed to the Web Script
                for (final String parameterName : parameterNames)
                {
                    switch (parameterName)
                    {
                        case PARAMETER_TARGET_NODEREF:
                            targetNodeRefStr = request.getParameter(PARAMETER_TARGET_NODEREF);
                            break;
                            
                        case PARAMETER_TARGET_PATH:
                            targetPath = request.getParameter(PARAMETER_TARGET_PATH);
                            break;

                        case PARAMETER_SOURCE_BEAN_ID:
                            sourceBeanId = request.getParameter(PARAMETER_SOURCE_BEAN_ID);
                            break;
                            
                        case PARAMETER_SUBMIT_BUTTON:
                            // Remove the submit button from the parameters that are passed to the import source
                            break;
                            
                        default:
                            parameters.put(parameterName, Arrays.asList(request.getParameterValues(parameterName)));
                            break;
                    }
                }
                
                // Validate parameters
                if (targetNodeRefStr == null || targetNodeRefStr.trim().length() == 0)
                {
                    if (targetPath == null || targetPath.trim().length() == 0)
                    {
                        throw new RuntimeException("Error: neither parameter '" + PARAMETER_TARGET_NODEREF +
                                                   "' nor parameter '" + PARAMETER_TARGET_PATH +
                                                   "' was provided, but at least one is required.");
                    }
                    
                    targetNodeRef = convertPathToNodeRef(serviceRegistry, targetPath.trim());
                }
                else
                {
                    targetNodeRef = new NodeRef(targetNodeRefStr.trim());
                }
                
                if (sourceBeanId == null || sourceBeanId.trim().length() == 0)
                {
                    if (info(log)) info(log, "No source bean id provided, defaulting to " + DEFAULT_SOURCE_BEAN_ID);
                    sourceBeanId = DEFAULT_SOURCE_BEAN_ID;
                }
                
                // Initiate the import
                importer.start(sourceBeanId, parameters, targetNodeRef);
            }
            else
            {
                throw new WebScriptException(Status.STATUS_CONFLICT, "An import is already in progress.");
            }
        }
        catch (final WebScriptException wse)
        {
            throw wse;
        }
        catch (final FileNotFoundException fnfe)
        {
            throw new WebScriptException(Status.STATUS_NOT_FOUND, "The path " + targetPath + " does not exist.", fnfe);
        }
        catch (final Throwable t)
        {
            throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, buildTextMessage(t), t);
        }
        
        // If successful, redirect to the status Web Script
        status.setCode(Status.STATUS_MOVED_TEMPORARILY);
        status.setRedirect(true);
        status.setLocation(request.getServiceContextPath() + WEB_SCRIPT_URI_BULK_IMPORT_STATUS);
        
        return(result);
    }
    
}
