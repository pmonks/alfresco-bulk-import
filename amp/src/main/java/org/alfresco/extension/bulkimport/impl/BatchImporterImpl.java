/*
 * Copyright (C) 2007-2016 Peter Monks.
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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.version.Version;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

import org.alfresco.extension.bulkimport.BulkImportStatus;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportItemVersion;

import static org.alfresco.extension.bulkimport.util.Utils.*;
import static org.alfresco.extension.bulkimport.util.LogUtils.*;


/**
 * This class implements the logic for importing a batch into Alfresco.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class BatchImporterImpl
    implements BatchImporter
{
    private final static Log log = LogFactory.getLog(BatchImporterImpl.class);

    private final static String REGEX_SPLIT_PATH_ELEMENTS = "[\\\\/]+";

    private final ServiceRegistry serviceRegistry;
    private final BehaviourFilter behaviourFilter;
    private final NodeService     nodeService;
    private final VersionService  versionService;
    private final ContentService  contentService;
    
    
    private final WritableBulkImportStatus importStatus;
    
    
    public BatchImporterImpl(final ServiceRegistry          serviceRegistry,
                             final BehaviourFilter          behaviourFilter,
                             final WritableBulkImportStatus importStatus)
    {
        // PRECONDITIONS
        assert serviceRegistry != null : "serviceRegistry must not be null.";
        assert behaviourFilter != null : "behaviourFilter must not be null.";
        assert importStatus    != null : "importStatus must not be null.";
        
        // Body
        this.serviceRegistry = serviceRegistry;
        this.behaviourFilter = behaviourFilter;
        this.importStatus    = importStatus;
        
        this.nodeService    = serviceRegistry.getNodeService();
        this.versionService = serviceRegistry.getVersionService();
        this.contentService = serviceRegistry.getContentService();
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.impl.BatchImporter#importBatch(String, NodeRef, Batch, boolean, boolean)
     */
    @Override
    public final void importBatch(final String  userId,
                                  final NodeRef target,
                                  final Batch   batch,
                                  final boolean replaceExisting,
                                  final boolean dryRun)
        throws InterruptedException,
               OutOfOrderBatchException
    {
        long start = System.nanoTime();
        
        final String batchName = "Batch #" + batch.getNumber() + ", " + batch.size() + " items, " + batch.sizeInBytes() + " bytes.";
        if (debug(log)) debug(log, "Importing " + batchName);
        importStatus.setCurrentlyImporting(batchName);
        
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            @Override
            public Object doWork()
                throws Exception
            {
                importBatchInTxn(target, batch, replaceExisting, dryRun);
                return(null);
            }
        }, userId);
        
        if (debug(log))
        {
            long end = System.nanoTime();
            debug(log, "Batch #" + batch.getNumber() + " (containing " + batch.size() + " nodes) processed in " + getDurationInSeconds(end - start) + ".");
        }
    }

    
    private final void importBatchInTxn(final NodeRef target,
                                        final Batch   batch,
                                        final boolean replaceExisting,
                                        final boolean dryRun)
        throws InterruptedException,
               OutOfOrderBatchException
    {
        RetryingTransactionHelper txnHelper = serviceRegistry.getRetryingTransactionHelper();

        txnHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {
            @Override
            public Object execute()
                throws Exception
            {
                // Disable the auditable aspect's behaviours for this transaction, to allow creation & modification dates to be set
                behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
                
                importBatchImpl(target, batch, replaceExisting, dryRun);
                return(null);
            }
        },
        false,   // read only flag, false=R/W txn
        false);  // requires new txn flag, false=does not require a new txn if one is already in progress (which should never be the case here)

        importStatus.batchCompleted(batch);
    }
    
    
    private final void importBatchImpl(final NodeRef target,
                                       final Batch   batch,
                                       final boolean replaceExisting,
                                       final boolean dryRun)
        throws InterruptedException
    {
        if (batch != null)
        {
            for (final BulkImportItem<BulkImportItemVersion> item : batch)
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
                
                importItem(target, item, replaceExisting, dryRun);
            }
        }
    }
    
    
    private final void importItem(final NodeRef                               target,
                                  final BulkImportItem<BulkImportItemVersion> item,
                                  final boolean                               replaceExisting,
                                  final boolean                               dryRun)
        throws InterruptedException
    {
        try
        {
            if (trace(log)) trace(log, "Importing " + (item.isDirectory() ? "directory " : "file ") + String.valueOf(item) + ".");
            
            NodeRef nodeRef     = findOrCreateNode(target, item, replaceExisting, dryRun);
            boolean isDirectory = item.isDirectory();
            
            if (nodeRef != null)
            {
                // We're creating or replacing the item, so import it
                if (isDirectory)
                {
                    importDirectory(nodeRef, item, dryRun);
                }
                else
                {
                    importFile(nodeRef, item, dryRun);
                }
            }
            
            if (trace(log)) trace(log, "Finished importing " + String.valueOf(item));
        }
        catch (final InterruptedException ie)
        {
            Thread.currentThread().interrupt();            
            throw ie;
        }
        catch (final OutOfOrderBatchException oobe)
        {
            throw oobe;
        }
        catch (final Exception e)
        {
            // Capture the item that failed, along with the exception
            throw new ItemImportException(item, e);
        }
    }
    
    
    private final NodeRef findOrCreateNode(final NodeRef                               target,
                                           final BulkImportItem<BulkImportItemVersion> item,
                                           final boolean                               replaceExisting,
                                           final boolean                               dryRun)
    {
        NodeRef result           = null;
        String  nodeName         = item.getName();
        String  nodeNamespace    = item.getNamespace();
        QName   nodeQName        = QName.createQName(nodeNamespace == null ? NamespaceService.CONTENT_MODEL_1_0_URI : nodeNamespace,
                                                     QName.createValidLocalName(nodeName));
        boolean isDirectory      = item.isDirectory();
        String  parentAssoc      = item.getParentAssoc();
        QName   parentAssocQName = parentAssoc == null ? ContentModel.ASSOC_CONTAINS : createQName(serviceRegistry, parentAssoc);
        NodeRef parentNodeRef    = null;
        
        try
        {
            parentNodeRef = getParent(target, item);
        
            if (parentNodeRef == null)
            {
                parentNodeRef = target;
            }
            
            // Find the node
            if (trace(log)) trace(log, "Searching for node with name '" + nodeName + "' within node '" + String.valueOf(parentNodeRef) + "' with parent association '" + String.valueOf(parentAssocQName) + "'.");
            result = nodeService.getChildByName(parentNodeRef, parentAssocQName, nodeName);
        }
        catch (final OutOfOrderBatchException oobe)
        {
            if (dryRun)
            {
                parentNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "dry-run-fake-parent-node-ref");
            }
            else
            {
                throw oobe;
            }
        }
        
        if (result == null)    // We didn't find it, so create a new node in the repo. 
        {
            String itemType      = item.getVersions().first().getType();
            QName  itemTypeQName = itemType == null ? (isDirectory ? ContentModel.TYPE_FOLDER : ContentModel.TYPE_CONTENT) : createQName(serviceRegistry, itemType);

            if (dryRun)
            {
                if (info(log)) info(log, "[DRY RUN] Would have created new node of type '" + String.valueOf(itemTypeQName) + "' with qname '" + String.valueOf(nodeQName) + "' within node '" + String.valueOf(parentNodeRef) + "' with parent association '" + String.valueOf(parentAssocQName) + "'.");
                result = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "dry-run-fake-created-node-ref");
            }
            else
            {
                if (trace(log)) trace(log, "Creating new node of type '" + String.valueOf(itemTypeQName) + "' with qname '" + String.valueOf(nodeQName) + "' within node '" + String.valueOf(parentNodeRef) + "' with parent association '" + String.valueOf(parentAssocQName) + "'.");
                Map<QName, Serializable> props = new HashMap<>();
                props.put(ContentModel.PROP_NAME, nodeName);
                result = nodeService.createNode(parentNodeRef, parentAssocQName, nodeQName, itemTypeQName, props).getChildRef();
            }
        }
        else if (replaceExisting)
        {
            if (trace(log)) trace(log, "Found content node '" + String.valueOf(result) + "', replacing it.");
        }
        else
        {
            if (info(log)) info(log, "Skipping '" + item.getName() + "' as it already exists in the repository and 'replace existing' is false.");
            result = null;
            importStatus.incrementTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_SKIPPED);
        }
        
        return(result);
    }
    
    
    private NodeRef getParent(final NodeRef target, final BulkImportItem<BulkImportItemVersion> item)
    {
        NodeRef result = null;
        
        final String itemParentPath         = item.getRelativePathOfParent();
        List<String> itemParentPathElements = (itemParentPath == null || itemParentPath.length() == 0) ? null : Arrays.asList(itemParentPath.split(REGEX_SPLIT_PATH_ELEMENTS));
        
        if (debug(log)) debug(log, "Finding parent folder '" + itemParentPath + "'.");
        
        if (itemParentPathElements != null && itemParentPathElements.size() > 0)
        {
            FileInfo fileInfo = null;
                
            try
            {
                //####TODO: I THINK THIS WILL FAIL IN THE PRESENCE OF CUSTOM NAMESPACES / PARENT ASSOC QNAMES!!!!
                fileInfo = serviceRegistry.getFileFolderService().resolveNamePath(target, itemParentPathElements, false);
            }
            catch (final FileNotFoundException fnfe)  // This should never be triggered due to the last parameter in the resolveNamePath call, but just in case
            {
                throw new OutOfOrderBatchException(itemParentPath, fnfe);
            }
            
            // Out of order batch submission (child arrived before parent)
            if (fileInfo == null)
            {
                throw new OutOfOrderBatchException(itemParentPath);
            }
            
            result = fileInfo.getNodeRef();
        }
        
        return(result);
    }
    
    

    private final void importDirectory(final NodeRef                               nodeRef,
                                       final BulkImportItem<BulkImportItemVersion> item,
                                       final boolean                               dryRun)
        throws InterruptedException
    {
        if (item.getVersions() != null &&
            item.getVersions().size() > 0)
        {
            if (item.getVersions().size() > 1)
            {
                warn(log, "Skipping versions for directory '" + item.getName() + "' - Alfresco does not support versioned spaces.");
            }
            
            final BulkImportItemVersion lastVersion = item.getVersions().last();

            if (lastVersion.hasContent())
            {
                warn(log, "Skipping content for directory '" + item.getName() + "' - Alfresco doesn't support content in spaces.");
            }
            
            // Import the last version's metadata only
            importVersionMetadata(nodeRef, lastVersion, dryRun);
        }
        else
        {
            if (trace(log)) trace(log, "No metadata to import for directory '" + item.getName() + "'.");
        }

        if (trace(log)) trace(log, "Finished importing metadata for directory " + item.getName() + ".");
    }


    private final void importFile(final NodeRef                               nodeRef,
                                  final BulkImportItem<BulkImportItemVersion> item,
                                  final boolean                               dryRun)
        throws InterruptedException
    {
        final int numberOfVersions = item.getVersions().size();
        
        if (numberOfVersions == 0)
        {
            throw new IllegalStateException(item.getName() + " (being imported into " + String.valueOf(nodeRef) + ") has no versions.");
        }
        else if (numberOfVersions == 1)
        {
            importVersion(nodeRef, null, item.getVersions().first(), dryRun, true);
        }
        else
        {
            final BulkImportItemVersion firstVersion = item.getVersions().first();
            BulkImportItemVersion previousVersion = null;
            
            // Add the cm:versionable aspect if it isn't already there
            if (firstVersion.getAspects() == null ||
                firstVersion.getAspects().isEmpty() ||
                (!firstVersion.getAspects().contains(ContentModel.ASPECT_VERSIONABLE.toString()) &&
                 !firstVersion.getAspects().contains(ContentModel.ASPECT_VERSIONABLE.toPrefixString())))
            {
                if (debug(log)) debug(log, item.getName() + " has versions but is missing the cm:versionable aspect. Adding it.");
                nodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, null);
            }
        
            for (final BulkImportItemVersion version : item.getVersions())
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
                
                importVersion(nodeRef, previousVersion, version, dryRun, false);
                previousVersion = version;
            }
        }
        
        if (trace(log)) trace(log, "Finished importing " + numberOfVersions + " version" + (numberOfVersions == 1 ? "" : "s") + " of file " + item.getName() + ".");
    }
    
    
    private final void importVersion(final NodeRef               nodeRef,
                                     final BulkImportItemVersion previousVersion,
                                     final BulkImportItemVersion version,
                                     final boolean               dryRun,
                                     final boolean               onlyOneVersion)
        throws InterruptedException
    {
        Map<String, Serializable> versionProperties = new HashMap<>();
        boolean                   isMajor           = true;
        
        if (version == null)
        {
            throw new IllegalStateException("version was null. This is indicative of a bug in the chosen import source.");
        }
        
        importVersionContentAndMetadata(nodeRef, version, dryRun);
        
        if (previousVersion != null && version.getVersionNumber() != null)
        {
            final BigDecimal difference = version.getVersionNumber().subtract(previousVersion.getVersionNumber());
            
            isMajor = difference.compareTo(BigDecimal.ONE) >= 0;
        }

        // Note: PROP_VERSION_LABEL is a "reserved" property, and cannot be modified by custom code.
        // In other words, we can't use the source's version label as the version label in Alfresco.  :-(
        // See: https://github.com/pmonks/alfresco-bulk-import/issues/13
//        versionProperties.put(ContentModel.PROP_VERSION_LABEL.toString(), String.valueOf(version.getVersionNumber().toString()));

        versionProperties.put(VersionModel.PROP_VERSION_TYPE, isMajor ? VersionType.MAJOR : VersionType.MINOR);

        if (version.getVersionComment() != null)
        {
            versionProperties.put(Version.PROP_DESCRIPTION, version.getVersionComment());
        }
        
        if (dryRun)
        {
            if (info(log)) info(log, "[DRY RUN] Would have created " + (isMajor ? "major" : "minor") + " version of node '" + String.valueOf(nodeRef) + "'.");
        }
        else
        {
            // Only create versions if we have to - this is an exceptionally expensive operation in Alfresco
            if (onlyOneVersion)
            {
                if (trace(log)) trace(log, "Skipping creation of a version for node '" + String.valueOf(nodeRef) + "' as it only has one version.");
            }
            else
            {
                if (trace(log)) trace(log, "Creating " + (isMajor ? "major" : "minor") + " version of node '" + String.valueOf(nodeRef) + "'.");
                versionService.createVersion(nodeRef, versionProperties);
            }
        }
    }
    
    
    private final void importVersionContentAndMetadata(final NodeRef               nodeRef,
                                                       final BulkImportItemVersion version,
                                                       final boolean               dryRun)
        throws InterruptedException
    {
        if (version.hasMetadata())
        {
            importVersionMetadata(nodeRef, version, dryRun);
        }
        
        if (version.hasContent())
        {
            importVersionContent(nodeRef, version, dryRun);
        }
    }
    
    
    private final void importVersionMetadata(final NodeRef               nodeRef,
                                             final BulkImportItemVersion version,
                                             final boolean               dryRun)
        throws InterruptedException
    {
        String                    type     = version.getType();
        Set<String>               aspects  = version.getAspects();
        Map<String, Serializable> metadata = version.getMetadata();
        
        if (type != null)
        {
            if (dryRun)
            {
                if (info(log)) info(log, "[DRY RUN] Would have set type of '" + String.valueOf(nodeRef) + "' to '" + String.valueOf(type) + "'.");
            }
            else
            {
                if (trace(log)) trace(log, "Setting type of '" + String.valueOf(nodeRef) + "' to '" + String.valueOf(type) + "'.");
                nodeService.setType(nodeRef, createQName(serviceRegistry, type));
            }
        }
        
        if (aspects != null)
        {
            for (final String aspect : aspects)
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");

                if (dryRun)
                {
                    if (info(log)) info(log, "[DRY RUN] Would have added aspect '" + aspect + "' to '" + String.valueOf(nodeRef) + "'.");
                }
                else
                {
                    if (trace(log)) trace(log, "Adding aspect '" + aspect + "' to '" + String.valueOf(nodeRef) + "'.");
                    nodeService.addAspect(nodeRef, createQName(serviceRegistry, aspect), null);
                }
            }
        }
        
        if (version.hasMetadata())
        {
            if (metadata == null) throw new IllegalStateException("The import source has logic errors - it says it has metadata, but the metadata is null.");

            
            // QName all the keys.  It's baffling that NodeService doesn't have a method that accepts a Map<String, Serializable>, when things like VersionService do...
            Map<QName, Serializable> qNamedMetadata = new HashMap<>(metadata.size());
            
            for (final String key : metadata.keySet())
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
                
                QName        keyQName = createQName(serviceRegistry, key);
                Serializable value    = metadata.get(key);
                
                qNamedMetadata.put(keyQName, value);
            }

            if (dryRun)
            {
                if (info(log)) info(log, "[DRY RUN] Would have added the following properties to '" + String.valueOf(nodeRef) +
                                         "':\n" + Arrays.toString(qNamedMetadata.entrySet().toArray()));
            }
            else
            {
                try
                {
                    if (trace(log)) trace(log, "Adding the following properties to '" + String.valueOf(nodeRef) +
                                               "':\n" + Arrays.toString(qNamedMetadata.entrySet().toArray()));
                    nodeService.addProperties(nodeRef, qNamedMetadata);
                }
                catch (final InvalidNodeRefException inre)
                {
                    if (!nodeRef.equals(inre.getNodeRef()))
                    {
                        // Caused by an invalid NodeRef in the metadata (e.g. in an association)
                        throw new IllegalStateException("Invalid nodeRef found in metadata file '" + version.getMetadataSource() + "'.  " +
                                                        "Probable cause: an association is being populated via metadata, but the " +
                                                        "NodeRef for the target of that association ('" + inre.getNodeRef() + "') is invalid.  " +
                                                        "Please double check your metadata file and try again.", inre);
                    }
                    else
                    {
                        // Logic bug in the BFSIT.  :-(
                        throw inre;
                    }
                }
            }
        }
    }
    

    private final void importVersionContent(final NodeRef               nodeRef,
                                            final BulkImportItemVersion version,
                                            final boolean               dryRun)
        throws InterruptedException
    {
        if (version.hasContent())
        {
            if (version.contentIsInPlace())
            {
                if (dryRun)
                {
                    if (info(log)) info(log, "[DRY RUN] Content for node '" + String.valueOf(nodeRef) + "' is in-place.");
                }
                else
                {
                    if (trace(log)) trace(log, "Content for node '" + String.valueOf(nodeRef) + "' is in-place.");
                }
                
                if (!version.hasMetadata() ||
                    version.getMetadata() == null ||
                    (!version.getMetadata().containsKey(ContentModel.PROP_CONTENT.toPrefixString()) &&
                     !version.getMetadata().containsKey(ContentModel.PROP_CONTENT.toString())))
                {
                    throw new IllegalStateException("The source system you selected is incorrectly implemented - it is reporting" +
                                                    " that content is in place for '" + version.getContentSource() +
                                                    "', but the metadata doesn't contain the '" + String.valueOf(ContentModel.PROP_CONTENT) +
                                                    "' property.");
                }
                
                importStatus.incrementTargetCounter(BulkImportStatus.TARGET_COUNTER_IN_PLACE_CONTENT_LINKED);
            }
            else  // Content needs to be streamed into the repository
            {
                if (dryRun)
                {
                    if (info(log)) info(log, "[DRY RUN] Would have streamed content from '" + version.getContentSource() + "' into node '" + String.valueOf(nodeRef) + "'.");
                }
                else
                {
                    if (trace(log)) trace(log, "Streaming content from '" + version.getContentSource() + "' into node '" + String.valueOf(nodeRef) + "'.");
                    
                    ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
                    version.putContent(writer);

                    if (trace(log)) trace(log, "Finished streaming content from '" + version.getContentSource() + "' into node '" + String.valueOf(nodeRef) + "'.");
                }
                
                importStatus.incrementTargetCounter(BulkImportStatus.TARGET_COUNTER_CONTENT_STREAMED);
            }
        }
    }
    
}
