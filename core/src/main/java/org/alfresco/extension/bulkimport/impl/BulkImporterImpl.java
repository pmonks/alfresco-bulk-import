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

import java.util.List;
import java.util.Map;

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
import org.alfresco.service.cmr.security.PermissionService;

import org.alfresco.extension.bulkimport.BulkImportStatus;
import org.alfresco.extension.bulkimport.BulkImporter;
import org.alfresco.extension.bulkimport.source.BulkImportSource;

import static org.alfresco.extension.bulkimport.BulkImportLogUtils.*;


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
    private final static String SCANNER_THREAD_NAME  = "BulkImport-ScannerThread";

    private final ServiceRegistry   serviceRegistry;
    private final NodeService       nodeService;
    private final DictionaryService dictionaryService;
    private final PermissionService permissionService;
    
    private final WritableBulkImportStatus importStatus;
    private final BatchImporter            batchImporter;
    private final int                      batchWeight;
    
    private ApplicationContext appContext;
    
    // Transient state while an import is in progress
    private Thread                          scannerThread;
    private BlockingPausableExecutorService importThreadPool;
    
    
    public BulkImporterImpl(final ServiceRegistry          serviceRegistry,
                            final WritableBulkImportStatus importStatus,
                            final BatchImporter            batchImporter,
                            final int                      batchWeight)
    {
        // PRECONDITIONS
        assert serviceRegistry != null : "serviceRegistry must not be null.";
        assert importStatus    != null : "importStatus must not be null.";
        assert batchImporter   != null : "batchImporter must not be null.";

        // Body
        this.serviceRegistry   = serviceRegistry;
        this.nodeService       = serviceRegistry.getNodeService();
        this.dictionaryService = serviceRegistry.getDictionaryService();
        this.permissionService = serviceRegistry.getPermissionService();
        
        this.importStatus  = importStatus;
        this.batchImporter = batchImporter;
        this.batchWeight   = batchWeight <= 0 ? DEFAULT_BATCH_WEIGHT : batchWeight;
    }
    
    
    public void setApplicationContext(ApplicationContext appContext)
        throws BeansException
    {
        // PRECONDITIONS
        assert appContext != null : "appContext must not be null.";
        
        // Body
        this.appContext = appContext;
    }


    /*--------------------------------------------------------------------------*
     * Implemented methods
     *--------------------------------------------------------------------------*/

    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#start(java.lang.String, java.util.Map, org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    public void start(final String bulkImportSourceBeanId, final Map<String, List<String>> parameters, final NodeRef target)
        throws Throwable
    {
        BulkImportSource source = appContext.getBean(bulkImportSourceBeanId, BulkImportSource.class);
        
        start(source, parameters, target);
    }
    
    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#start(org.alfresco.extension.bulkimport.source.BulkImportSource, java.util.Map, org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    public void start(final BulkImportSource source, final Map<String, List<String>> parameters, final NodeRef target)
        throws Throwable
    {
        // PRECONDITIONS
        assert target     != null : "target must not be null.";
        assert source     != null : "source must not be null.";
        assert parameters != null : "parameters must not be null.";
        
        // Body
        if (importStatus.inProgress())
        {
            throw new IllegalStateException("An import is already in progress.");
        }
            
        validateTarget(target);
        
        if (info(log)) info(log, source.getName() + " bulk import started with parameters '" + String.valueOf(parameters) + "'...");
//        if (debug(log)) debug(log, "---- Data Dictionary:\n" + dataDictionaryBuilder.toString());

        // Create the threads used by the bulk import tool
        importThreadPool = createThreadPool();
        scannerThread    = new Thread(new Scanner(serviceRegistry,
                                                  AuthenticationUtil.getRunAsUser(),
                                                  batchWeight,
                                                  importStatus,
                                                  source,
                                                  parameters,
                                                  target,
                                                  importThreadPool,
                                                  batchImporter));
        
        scannerThread.setName(SCANNER_THREAD_NAME);
        scannerThread.setDaemon(true);
        scannerThread.start();
    }


    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#stopImport()
     */
    @Override
    public void stop()
    {
        // Note: this must be called first, as the various threads look for this status to determine if their
        //       interruption was expected or not.
        importStatus.stopRequested();
        
        if (scannerThread != null &&
            scannerThread.isAlive())
        {
            scannerThread.interrupt();  // This will whack the thread pool for us
            scannerThread = null;
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

    
    /*--------------------------------------------------------------------------*
     * Private methods
     *--------------------------------------------------------------------------*/

    /**
     * Spring "lookup method" that will return a new ThreadPoolExecutor each time it's called.
     * We have to go to these extremes because:
     * 1. We need to be able to stop an entire import (including all of the worker threads)
     * 2. Java's ExecutorService framework only offers one way to do this: shutting down the entire ExecutorService
     * 3. Once shutdown, a Java ExecutorService can't be restarted / reused
     * 
     * Ergo this stuff...  *sigh*
     * 
     * @return A new ThreadPoolExecutor instance <i>(will not be null, assuming Spring is configured correctly)</i>.
     */
    protected abstract BlockingPausableExecutorService createThreadPool();
    
    
    /**
     * Validates that the given NodeRef exists and is a writeable space.
     * 
     * @param target The nodeRef to validate <i>(must not be null)</i>.
     */
    private final void validateTarget(final NodeRef target)
    {
        // PRECONDITIONS
        assert target != null : "target must not be null.";
        
        // Body
        if (!nodeService.exists(target))
        {
            throw new IllegalArgumentException("Target '" + String.valueOf(target) + "' doesn't exist.");
        }
        
        if (AccessStatus.DENIED.equals(permissionService.hasPermission(target, PermissionService.ADD_CHILDREN)))
        {
            throw new IllegalArgumentException("Target '" + String.valueOf(target) + "' is not writeable.");
        }
        
        if (!dictionaryService.isSubClass(nodeService.getType(target), ContentModel.TYPE_FOLDER))
        {
            throw new IllegalArgumentException("Target '" + String.valueOf(target) + "' is not a space.");
        }
    }

    
}
