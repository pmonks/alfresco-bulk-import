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

package org.alfresco.extension.bulkimport.source.fs;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.extension.bulkimport.source.AbstractBulkImportItemVersion;
import org.alfresco.extension.bulkimport.source.fs.MetadataLoader.Metadata;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;
import static org.alfresco.extension.bulkimport.source.fs.FilesystemSourceUtils.*;


/**
 * This class represents a single version of a filesystem bulk import item.
 */
public final class FilesystemVersion
    extends AbstractBulkImportItemVersion<File, File>
{
    private final MimetypeService mimeTypeService;
    private final ContentStore    configuredContentStore;
    private final MetadataLoader  metadataLoader;
    
    // Cached file info (to avoid repeated calls to stat syscall on the same file)
    private Metadata cachedMetadata    = null;
    private long     cachedSizeInBytes = 0L;
    private boolean  contentIsInPlace  = false;

    
    public FilesystemVersion(final MimetypeService mimeTypeService,
                             final ContentStore    configuredContentStore,
                             final MetadataLoader  metadataLoader,
                             final String          versionLabel,
                             final File            contentFile,
                             final File            metadataFile)
    {
        super(calculateName(metadataLoader, contentFile, metadataFile),
              contentFile == null ? false : contentFile.isDirectory(),  // Note: if we can't tell if this is a directory or not, default to not a directory
              versionLabel);
        
        this.mimeTypeService        = mimeTypeService;
        this.configuredContentStore = configuredContentStore;
        this.metadataLoader         = metadataLoader;
        this.contentReference       = contentFile;
        this.metadataReference      = metadataFile;
        
        // "stat" the content file then cache the results
        if (contentFile == null || contentFile.isDirectory())
        {
            cachedSizeInBytes = 0L;
        }
        else
        {
            cachedSizeInBytes = contentFile.length();
        }
    }
    
    public File getContentFile()
    {
        return(contentReference);
    }
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getType()
     */
    @Override
    public String getType()
    {
        loadMetadataIfNecessary();
        return(cachedMetadata.getType());
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getParentAssoc()
     */
    @Override
    public String getParentAssoc()
    {
        loadMetadataIfNecessary();
        return(cachedMetadata.getParentAssoc());
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getNamespace()
     */
    @Override
    public String getNamespace()
    {
        loadMetadataIfNecessary();
        return(cachedMetadata.getNamespace());
    }
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getAspects()
     */
    @Override
    public Set<String> getAspects()
    {
        loadMetadataIfNecessary();
        return(cachedMetadata.getAspects());
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#hasMetadata()
     */
    @Override
    public boolean hasMetadata()
    {
        loadMetadataIfNecessary();
        return(cachedMetadata.getProperties() != null &&
               cachedMetadata.getProperties().size() > 0);
    }
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getMetadata()
     */
    @Override
    public Map<String, Serializable> getMetadata()
    {
        loadMetadataIfNecessary();
        return(cachedMetadata.getProperties());
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getMetadataSource()
     */
    @Override
    public String getMetadataSource()
    {
        return(metadataReference.getAbsolutePath());
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#hasContent()
     */
    @Override
    public boolean hasContent()
    {
        return(contentReference != null && !contentReference.isDirectory());
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getContentSource()
     */
    @Override
    public String getContentSource()
    {
        return(contentReference.getAbsolutePath());
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#sizeInBytes()
     */
    @Override
    public long sizeInBytes()
    {
        return(cachedSizeInBytes);
    }
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#contentIsInPlace()
     */
    @Override
    public boolean contentIsInPlace()
    {
        loadMetadataIfNecessary();
        return(contentIsInPlace);
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#putContent(org.alfresco.service.cmr.repository.ContentWriter)
     */
    @Override
    public void putContent(final ContentWriter writer)
    {
        writer.guessMimetype(contentReference.getName());
        writer.putContent(contentReference);
        writer.guessEncoding();
    }
    
    
    private final static String calculateName(final MetadataLoader metadataLoader,
                                              final File           contentFile,
                                              final File           metadataFile)
    {
        String result = null;
        
        final Metadata metadata = metadataLoader.loadMetadata(metadataFile);
        
        if (metadata.getProperties().containsKey(ContentModel.PROP_NAME.toString()))
        {
            result = (String)metadata.getProperties().get(ContentModel.PROP_NAME.toString());
        }
        else if (metadata.getProperties().containsKey(ContentModel.PROP_NAME.toPrefixString()))
        {
            result = (String)metadata.getProperties().get(ContentModel.PROP_NAME.toPrefixString());
        }
        else
        {
            if (contentFile != null)
            {
                result = contentFile.getName();
            }
            else if (metadataFile != null)
            {
                result = getParentName(metadataLoader, metadataFile.getName());
            }
        }
    
        return(result);
    }
    
    private final synchronized void loadMetadataIfNecessary()
    {
        if (cachedMetadata == null)
        {
            cachedMetadata   = metadataLoader.loadMetadata(metadataReference);
            contentIsInPlace = false;
            
            if (contentReference != null)
            {
                try
                {
                    final Path                path       = contentReference.toPath();
                    final BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                    
                    // If not set in the metadata file, set the creation timestamp to what's on disk
                    if (!cachedMetadata.getProperties().containsKey(ContentModel.PROP_CREATED.toString()) &&
                        !cachedMetadata.getProperties().containsKey(ContentModel.PROP_CREATED.toPrefixString()) &&
                        attributes.creationTime() != null)
                    {
                        final Date created = new Date(attributes.creationTime().toMillis());
                        cachedMetadata.addProperty(ContentModel.PROP_CREATED.toString(), created);
                    }
                    
                    // If not set in the metadata file, set the modification timestamp to what's on disk
                    if (!cachedMetadata.getProperties().containsKey(ContentModel.PROP_MODIFIED.toString()) &&
                        !cachedMetadata.getProperties().containsKey(ContentModel.PROP_MODIFIED.toPrefixString()) &&
                        attributes.lastModifiedTime() != null)
                    {
                        final Date modified = new Date(attributes.lastModifiedTime().toMillis());
                        cachedMetadata.addProperty(ContentModel.PROP_MODIFIED.toString(), modified);
                    }
                    
                    // If an in-place import is possible, attempt to construct a content URL
                    if (!contentReference.isDirectory() && isInContentStore(configuredContentStore, contentReference))
                    {
                        final ContentData contentData = buildContentProperty(mimeTypeService, configuredContentStore, contentReference);
                        
                        if (contentData != null)
                        {
                            // We have valid in-place content
                            contentIsInPlace = true;
                            cachedMetadata.addProperty(ContentModel.PROP_CONTENT.toString(), contentData);
                        }
                        else
                        {
                            if (warn(FilesystemBulkImportItem.log)) warn (FilesystemBulkImportItem.log, "Unable to in-place import '" + getFileName(contentReference) + "'. Will stream it instead.");
                        }
                    }
                }
                catch (final IOException ioe)
                {
                    // Not much we can do in this case - log it and keep on truckin'
                    if (warn(FilesystemBulkImportItem.log)) warn(FilesystemBulkImportItem.log, "Unable to read file attributes for " + contentReference.getAbsolutePath() + ". Creation and modification timestamps will be system generated.", ioe);
                }
            }
        }
    }

}