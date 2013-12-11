/*
 * Copyright (C) 2007-2013 Peter Monks.
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
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.extension.bulkimport.BulkImportStatus;
import org.alfresco.extension.bulkimport.BulkImporter;
import org.alfresco.extension.bulkimport.source.BulkImportSource;
import org.java.util.concurrent.NotifyingBlockingThreadPoolExecutor;

/**
 * This class implements multi-threaded Bulk Importer logic.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public abstract class BulkImporterImpl
    implements BulkImporter
{
    private final static Log log = LogFactory.getLog(BulkImporterImpl.class);
    
    private final static int    DEFAULT_BATCH_WEIGHT = 100;
    private final static String SCANNER_THREAD_NAME  = "BulkImport-ScannerThread";

    private final BatchImporter        batchImporter;
    private final ServiceRegistry      serviceRegistry;
    private final NodeService          nodeService;
    private final DictionaryService    dictionaryService;
    private final PermissionService    permissionService;
    
    private final BulkImportStatusImpl importStatus;
    private final int                  batchWeight;
    
    
    // Transient state while an import is in progress
    private Thread                              scannerThread;
    private NotifyingBlockingThreadPoolExecutor importThreadPool;
    
    
    public BulkImporterImpl(final BatchImporter        batchImporter,
                            final ServiceRegistry      serviceRegistry,
                            final BulkImportStatusImpl importStatus,
                            final int                  batchWeight)
    {
        // PRECONDITIONS
        assert batchImporter   != null : "batchImporter must not be null.";
        assert serviceRegistry != null : "serviceRegistry must not be null.";
        assert importStatus    != null : "importStatus must not be null.";

        // Body
        this.batchImporter     = batchImporter;
        this.serviceRegistry   = serviceRegistry;
        this.nodeService       = serviceRegistry.getNodeService();
        this.dictionaryService = serviceRegistry.getDictionaryService();
        this.permissionService = serviceRegistry.getPermissionService();
        
        this.importStatus = importStatus;
        this.batchWeight  = batchWeight <= 0 ? DEFAULT_BATCH_WEIGHT : batchWeight;
    }


    /*--------------------------------------------------------------------------*
     * Implemented methods
     *--------------------------------------------------------------------------*/

    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#bulkImport(org.alfresco.service.cmr.repository.NodeRef, java.io.File, boolean)
     */
    @Override
    public void start(final NodeRef target, final BulkImportSource source, final Map<String,String> parameters)
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
            
        validateNodeRefIsWritableSpace(target);
            
        if (log.isInfoEnabled()) log.info("Bulk import started from '" + source.getName() + "' with parameters '" + String.valueOf(parameters) + "'...");
//        if (log.isDebugEnabled()) log.debug("---- Data Dictionary:\n" + dataDictionaryBuilder.toString());

        // Create the threads used by the bulk import tool
        importThreadPool = createThreadPool();
        scannerThread    = new Thread(new Scanner(AuthenticationUtil.getRunAsUser(), batchWeight, importStatus, source, parameters, target, importThreadPool, batchImporter));
        scannerThread.setName(SCANNER_THREAD_NAME);
        scannerThread.setDaemon(true);
        
//        importStatus.startImport(source,
//                                 parameters,
//                                 getRepositoryPath(target),
//                                 source.inPlaceImportPossible(parameters) ? BulkImportStatus.ImportType.IN_PLACE : BulkImportStatus.ImportType.STREAMING,
//                                 batchWeight,
//                                 importThreadPool);
        
        scannerThread.start();
    }


    /**
     * @see org.alfresco.extension.bulkimport.BulkImporter#stopImport()
     */
    @Override
    public void stop()
    {
        if (scannerThread != null &&
            scannerThread.isAlive())
        {
            scannerThread.interrupt();
        }
        
        if (importThreadPool != null &&
            !importThreadPool.isShutdown() &&
            !importThreadPool.isTerminated() &&
            !importThreadPool.isTerminating())
        {
            importThreadPool.shutdownNow();
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
     * Miscellaneous private utility methods
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
    protected abstract NotifyingBlockingThreadPoolExecutor createThreadPool();
    
    
    /**
     * Validates that the given NodeRef exists and is a writeable space.
     * 
     * @param nodeRef The nodeRef to validate <i>(must not be null)</i>.
     */
    private final void validateNodeRefIsWritableSpace(final NodeRef nodeRef)
    {
        if (nodeRef == null)
        {
            throw new IllegalArgumentException("nodeRef must not be null.");
        }
        
        if (!nodeService.exists(nodeRef))
        {
            throw new RuntimeException("Target '" + String.valueOf(nodeRef) + "' doesn't exist.");
        }
        
        if (AccessStatus.DENIED.equals(permissionService.hasPermission(nodeRef, PermissionService.ADD_CHILDREN)))
        {
            throw new RuntimeException("Target '" + String.valueOf(nodeRef) + "' is not writeable.");
        }
        
        if (!dictionaryService.isSubClass(nodeService.getType(nodeRef), ContentModel.TYPE_FOLDER))
        {
            throw new RuntimeException("Target '" + String.valueOf(nodeRef) + "' is not a space.");
        }
    }

    
    /**
     * Returns a human-readable rendition of the repository path of the given NodeRef.
     * 
     * @param nodeRef The nodeRef from which to dervice a path <i>(may be null)</i>.
     * @return The human-readable path <i>(will be null if the nodeRef is null or the nodeRef doesn't exist)</i>.
     */
    private final String getRepositoryPath(final NodeRef nodeRef)
    {
        String result = null;
        
        if (nodeRef != null)
        {
            List<FileInfo> pathElements = null;
            
            try
            {
                pathElements = serviceRegistry.getFileFolderService().getNamePath(null, nodeRef);   // Note: violates issue #132, but allowable in this case since this is a R/O method without an obvious alternative

                if (pathElements != null && pathElements.size() > 0)
                {
                    StringBuilder temp = new StringBuilder();
                    
                    for (FileInfo pathElement : pathElements)
                    {
                        temp.append("/");
                        temp.append(pathElement.getName());
                    }
                    
                    result = temp.toString();
                }
            }
            catch (final FileNotFoundException fnfe)
            {
                // Do nothing
            }
        }
        
        return(result);
    }
    
}
