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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alfresco.extension.bulkimport.util.ThreadPauser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;

import org.alfresco.extension.bulkimport.BulkImportCompletionHandler;
import org.alfresco.extension.bulkimport.BulkImportStatus;
import org.alfresco.extension.bulkimport.BulkImporter;
import org.alfresco.extension.bulkimport.source.BulkImportSource;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;


/**
 * This class implements multi-threaded Bulk Importer logic.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public abstract class BulkImporterImpl   // Note: this class is only abstract because of the shenanigans required to make thread pooling work
    implements BulkImporter,
               ApplicationContextAware
{
    private final static Log log = LogFactory.getLog(BulkImporterImpl.class);

    private final static int    DEFAULT_BATCH_WEIGHT = 100;
    private final static String SCANNER_THREAD_NAME  = "BulkImport-Scanner";

    private final ServiceRegistry       serviceRegistry;
    private final NodeService           nodeService;
    private final DictionaryService     dictionaryService;
    private final PermissionService     permissionService;
    private final AuthenticationService authenticationService;
    
    private final WritableBulkImportStatus          importStatus;
    private final ThreadPauser                      pauser;
    private final BatchImporter                     batchImporter;
    private final int                               batchWeight;
    private final List<BulkImportCompletionHandler> completionHandlers;
    
    private ApplicationContext appContext;
    
    // Transient state while an import is in progress
    private Thread scannerThread;

    
    public BulkImporterImpl(final ServiceRegistry                   serviceRegistry,
                            final WritableBulkImportStatus          importStatus,
                            final ThreadPauser                      pauser,
                            final BatchImporter                     batchImporter,
                            final int                               batchWeight,
                            final List<BulkImportCompletionHandler> completionHandlers)
    {
        // PRECONDITIONS
        assert serviceRegistry != null : "serviceRegistry must not be null.";
        assert importStatus    != null : "importStatus must not be null.";
        assert pauser          != null : "pauser must not be null.";
        assert batchImporter   != null : "batchImporter must not be null.";

        // Body
        this.serviceRegistry       = serviceRegistry;
        this.nodeService           = serviceRegistry.getNodeService();
        this.dictionaryService     = serviceRegistry.getDictionaryService();
        this.permissionService     = serviceRegistry.getPermissionService();
        this.authenticationService = serviceRegistry.getAuthenticationService();
        
        this.importStatus  = importStatus;
        this.pauser        = pauser;
        this.batchImporter = batchImporter;
        this.batchWeight   = batchWeight <= 0 ? DEFAULT_BATCH_WEIGHT : batchWeight;
        
        this.completionHandlers = completionHandlers;
    }
    
    
    /**
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(final ApplicationContext appContext)
        throws BeansException
    {
        // PRECONDITIONS
        assert appContext != null : "appContext must not be null.";
        
        // Body
        this.appContext = appContext;
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#getBulkImportSources()
     */
    @Override
    public Map<String, BulkImportSource> getBulkImportSources()
    {
        return(appContext.getBeansOfType(BulkImportSource.class));
    }


    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#start(java.lang.String, java.util.Map, org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    public void start(final String                    bulkImportSourceBeanId,
                      final Map<String, List<String>> parameters,
                      final NodeRef                   target)
    {
        BulkImportSource source = appContext.getBean(bulkImportSourceBeanId, BulkImportSource.class);
        
        start(source, parameters, target);
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#start(org.alfresco.extension.bulkimport.source.BulkImportSource, java.util.Map, org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    public void start(final BulkImportSource          source,
                      final Map<String, List<String>> parameters,
                      final NodeRef                   target)
    {
        // PRECONDITIONS
        if (source == null)
        {
            throw new IllegalArgumentException("Bulk import source bean must not be null.");
        }
        
        if (parameters == null)
        {
            throw new IllegalArgumentException("Bulk import parameters must not be null.");
        }
        
        if (target == null)
        {
            throw new IllegalArgumentException("Bulk import target nodeRef must not be null.");
        }
        
        if (!nodeService.exists(target))
        {
            throw new IllegalArgumentException("Bulk import target nodeRef " + String.valueOf(target) + " does not exist.");
        }
        
        if (!AccessStatus.ALLOWED.equals(permissionService.hasPermission(target, PermissionService.ADD_CHILDREN)))
        {
            throw new IllegalArgumentException("User " + authenticationService.getCurrentUserName() +
                                               " does not have permission to add children to target nodeRef " + String.valueOf(target) + ".");
        }

        if (!dictionaryService.isSubClass(nodeService.getType(target), ContentModel.TYPE_FOLDER))
        {
            throw new IllegalArgumentException("Target '" + String.valueOf(target) + "' is not a space.");
        }
        
        if (importStatus.inProgress())
        {
            throw new IllegalStateException("An import is already in progress.");
        }
            
        // Body
        if (debug(log)) debug(log, source.getName() + " bulk import started with parameters " + Arrays.toString(parameters.entrySet().toArray()) + ".");

        // Create the threads used by the bulk import tool
        scannerThread = new Thread(new Scanner(serviceRegistry,
                                               AuthenticationUtil.getRunAsUser(),
                                               batchWeight,
                                               importStatus,
                                               pauser,
                                               source,
                                               parameters,
                                               target,
                                               createThreadPool(),
                                               batchImporter,
                                               completionHandlers));
        
        scannerThread.setName(SCANNER_THREAD_NAME);
        scannerThread.setDaemon(true);
        scannerThread.start();
    }


    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#pause()
     */
    public void pause()
    {
        if (!importStatus.inProgress())
        {
            throw new IllegalStateException("No import in progress.");
        }

        if (pauser.isPaused())
        {
            throw new IllegalStateException("Import is already paused.");
        }

        if (info(log)) info(log, "Pause requested.");
        pauser.pause();
        importStatus.pauseRequested();
    }


    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#resume()
     */
    public void resume()
    {
        if (!importStatus.inProgress())
        {
            throw new IllegalStateException("No import in progress.");
        }

        if (!pauser.isPaused())
        {
            throw new IllegalStateException("Import is not paused.");
        }

        if (info(log)) info(log, "Resume requested.");
        pauser.resume();
        importStatus.resumeRequested();
    }


    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#stop()
     */
    @Override
    public void stop()
    {
        if (importStatus.inProgress())
        {
            if (info(log)) info(log, "Stop requested.");
            
            // Note: this must be called first, as the various threads look for this status to determine if their
            //       interruption was expected or not.
            importStatus.stopRequested();
            
            if (scannerThread != null)
            {
                scannerThread.interrupt();  // This indirectly whacks the entire import thread pool too
            }
            else
            {
                if (warn(log)) warn(log, "Scanner thread was null.");
            }
        }
        else
        {
            throw new IllegalStateException("No import in progress.");
        }
    }


    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#getStatus()
     */
    @Override
    public BulkImportStatus getStatus()
    {
        return(importStatus);
    }


    /**
     * Spring "lookup method" that will return a new BulkImportThreadPoolExecutor
     * each time it's called.
     * We have to go to these extremes because:
     * 1. We need to be able to stop an entire import (including all of the worker threads)
     * 2. Java's ExecutorService framework only offers one way to do this: shutting down the entire ExecutorService
     * 3. Once shutdown, a Java ExecutorService can't be restarted / reused
     * 
     * Ergo this stuff...  *sigh*
     * 
     * @return A new BulkImportThreadPoolExecutor instance <i>(will not be null, assuming Spring is configured correctly)</i>.
     */
    protected abstract BulkImportThreadPoolExecutor createThreadPool();
    
}
