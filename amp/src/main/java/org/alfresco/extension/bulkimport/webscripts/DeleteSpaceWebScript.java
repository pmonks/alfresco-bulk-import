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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import static org.alfresco.extension.bulkimport.util.Utils.*;
import static org.alfresco.extension.bulkimport.util.LogUtils.*;


/**
 * Web Script class that deletes a space and all contents as fast as possible
 * (i.e. by disabling rules, auditing, archiving, etc. etc.).
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public class DeleteSpaceWebScript
    extends DeclarativeWebScript
{
    private final static Log log = LogFactory.getLog(DeleteSpaceWebScript.class);
    
    // Other Web Script URIs
    private final static String WEB_SCRIPT_URI_DELETE_SPACE = "/bulk/import/delete";
    
    // Web Script parameters
    private final static String PARAMETER_TARGET_PATH = "targetPath";
    
    // Attributes
    private final ServiceRegistry serviceRegistry;
    private final BehaviourFilter behaviourFilter;
    private final NodeService     unauditedNodeService;
    

    public DeleteSpaceWebScript(final ServiceRegistry serviceRegistry,
                                final BehaviourFilter behaviourFilter,
                                final NodeService     unauditedNodeService)
    {
        // PRECONDITIONS
        assert serviceRegistry      != null : "serviceRegistry must not be null.";
        assert behaviourFilter      != null : "behaviourFilter must not be null.";
        assert unauditedNodeService != null : "unauditedNodeService must not be null.";
        
        // Body
        this.serviceRegistry      = serviceRegistry;
        this.behaviourFilter      = behaviourFilter;
        this.unauditedNodeService = unauditedNodeService;
    }

    /**
     * @see org.springframework.extensions.webscripts.DeclarativeWebScript#executeImpl(org.springframework.extensions.webscripts.WebScriptRequest, org.springframework.extensions.webscripts.Status, org.springframework.extensions.webscripts.Cache)
     */
    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest request, final Status status, final Cache cache)
    {
        Map<String, Object> result        = null;
        String              targetPath    = null;
        NodeRef             targetNodeRef = null;
        
        cache.setNeverCache(true);
        
        try
        {
            targetPath = request.getParameter(PARAMETER_TARGET_PATH);

            if (targetPath == null || targetPath.trim().length() == 0)
            {
                throw new RuntimeException("Error: parameter '" + PARAMETER_TARGET_PATH + "' was not provided.");
            }
            
            targetPath    = targetPath.trim();
            targetNodeRef = convertPathToNodeRef(serviceRegistry, targetPath);
            
            fastDeleteSpace(targetPath, targetNodeRef);
            
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
        
        // If successful, redirect back to the delete GET Web Script
        status.setCode(Status.STATUS_MOVED_TEMPORARILY);
        status.setRedirect(true);
        status.setLocation(request.getServiceContextPath() + WEB_SCRIPT_URI_DELETE_SPACE);
        
        return(result);
    }
    

    private final void fastDeleteSpace(final String targetPath, final NodeRef nodeRef)
    {
        long start = System.nanoTime();
        if (info(log)) info(log, "Deleting space " + targetPath + " and all contents...");
        
        // Disable as much stuff as possible
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
        unauditedNodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
        
        // Now delete it, using the unaudited NodeService
        unauditedNodeService.deleteNode(nodeRef);

        long end = System.nanoTime();
        if (info(log)) info(log, "Deleted space " + targetPath + " and all contents in " + getHumanReadableDuration(end - start) + ".");
    }
    
}
