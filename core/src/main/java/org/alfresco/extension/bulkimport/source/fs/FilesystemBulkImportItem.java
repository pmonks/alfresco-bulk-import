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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.extension.bulkimport.OutOfOrderBatchException;
import org.alfresco.extension.bulkimport.source.AbstractBulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.fs.MetadataLoader.Metadata;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;
import static org.alfresco.extension.bulkimport.source.fs.FilesystemUtils.*;


/**
 * This class represents a <code>BulkImportItem</code> sourced from the
 * server's local filesystem.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class FilesystemBulkImportItem
    extends AbstractBulkImportItem
{
    private final static Log log = LogFactory.getLog(FilesystemBulkImportItem.class);
    
    private final static String REGEX_SPLIT_PATH_ELEMENTS = "[\\\\/]+";
    
    private final ServiceRegistry serviceRegistry;
    private final MimetypeService mimeTypeService;
    private final ContentStore    configuredContentStore;
    private final MetadataLoader  metadataLoader;
    
    private final String                          importRelativePath;
    private final List<String>                    importRelativePathElements;
    private final String                          name;
    public final NavigableSet<FilesystemVersion> versions;
    
    
    public FilesystemBulkImportItem(final ServiceRegistry  serviceRegistry,
                                    final ContentStore     configuredContentStore,
                                    final MetadataLoader   metadataLoader,
                                    final String           importRelativePath,
                                    final String           name,
                                    final List<ImportFile> constituentFiles)
    {
        // PRECONDITIONS
        assert serviceRegistry         != null : "serviceRegistry must not be null.";
        assert configuredContentStore  != null : "configuredContentStore must not be null.";
        assert name                    != null : "name must not be null.";
        assert name.trim().length()    > 0     : "name must not be empty or blank.";
        assert constituentFiles        != null : "constituentFiles must not be null.";
        assert constituentFiles.size() > 0     : "constituentFiles must not be empty.";
        
        // Body
        this.serviceRegistry            = serviceRegistry;
        this.mimeTypeService            = serviceRegistry.getMimetypeService();
        this.configuredContentStore     = configuredContentStore;
        this.metadataLoader             = metadataLoader;
        this.importRelativePath         = importRelativePath;
        this.importRelativePathElements = (importRelativePath == null || importRelativePath.length() == 0) ? null : Arrays.asList(importRelativePath.split(REGEX_SPLIT_PATH_ELEMENTS));
        this.name                       = name;
        this.versions                   = new TreeSet<FilesystemVersion>();
        
        Map<String, FilesystemVersion> versionsIndexedByVersionLabel = new HashMap<String, FilesystemVersion>();
        
        for (final ImportFile importFile : constituentFiles)
        {
            updateOrCreateVersion(versionsIndexedByVersionLabel, importFile);
        }
    }
    
    
    private final void updateOrCreateVersion(final Map<String, FilesystemVersion> versionsIndexedByVersionLabel, final ImportFile importFile)
    {
        FilesystemVersion version = versionsIndexedByVersionLabel.get(importFile.getVersionLabel());
        
        if (version == null)
        {
            version = new FilesystemVersion(importFile.getVersionLabel(),
                                            importFile.isMetadata() ? null : importFile.getFile(),
                                            importFile.isMetadata() ? importFile.getFile() : null);
            
            versions.add(version);
            versionsIndexedByVersionLabel.put(importFile.getVersionLabel(), version);
        }

        if (importFile.isMetadata())
        {
            version.setMetadataFile(importFile.getFile());
        }
        else
        {
            version.setContentFile(importFile.getFile());
        }
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getParent(org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    public NodeRef getParent(final NodeRef target)
    {
        NodeRef result = null;
        
        if (debug(log)) debug(log, "Looking up parent in target-relative location '" + importRelativePath + "'.");
        
        if (importRelativePathElements != null && importRelativePathElements.size() > 0)
        {
            FileInfo fileInfo = null;
                
            try
            {
                fileInfo = serviceRegistry.getFileFolderService().resolveNamePath(target, importRelativePathElements, false);
            }
            catch (final FileNotFoundException fnfe)  // This should never be triggered due to the last parameter in the resolveNamePath call, but just in case
            {
                throw new OutOfOrderBatchException(importRelativePath, fnfe);
            }
            
            // Out of order batch submission (child arrived before parent)
            if (fileInfo == null)
            {
                throw new OutOfOrderBatchException(importRelativePath);
            }
            
            result = fileInfo.getNodeRef();
        }
        
        return(result);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getParentAssoc()
     */
    @Override
    public String getParentAssoc()
    {
        String                      result = null;
        Iterator<FilesystemVersion> iter   = versions.descendingIterator();
        
        while (iter.hasNext())
        {
            FilesystemVersion version = iter.next();
            
            if (version.getRawMetadata() != null &&
                version.getRawMetadata().getParentAssoc() != null)
            {
                result = version.getRawMetadata().getParentAssoc();
            }
        }
        
        return(result);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getNamespace()
     */
    @Override
    public String getNamespace()
    {
        String                      result = null;
        Iterator<FilesystemVersion> iter   = versions.descendingIterator();
        
        while (iter.hasNext())
        {
            final FilesystemVersion version = iter.next();
            
            if (version.getRawMetadata() != null &&
                version.getRawMetadata().getNamespace() != null)
            {
                result = version.getRawMetadata().getNamespace();
                break;
            }
        }
        
        return(result);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getName()
     */
    @Override
    public String getName()
    {
        return(name);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#isDirectory()
     */
    @Override
    public boolean isDirectory()
    {
        boolean                     result = false;
        Iterator<FilesystemVersion> iter   = versions.descendingIterator();
        
        while (iter.hasNext())
        {
            FilesystemVersion version = iter.next();
            
            if (version.isDirectory() != null)
            {
                result = version.isDirectory();
                break;
            }
        }
        
        return(result);
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getVersions()
     */
    @Override
    public SortedSet<Version> getVersions()
    {
        @SuppressWarnings({"unchecked", "rawtypes"})
        SortedSet<Version> result = Collections.unmodifiableSortedSet((SortedSet)versions);
        return(result);
    }


    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return(name + " (" + versions.size() + " version" + (versions.size() > 1 ? "s)" : ")"));
    }

    
    /* package */ final class FilesystemVersion
        implements BulkImportItem.Version,
                   Comparable<FilesystemVersion>
    {
        private final BigDecimal versionNumber;

        // Stateful, because versions are built up incrementally from the directory listing
        private File contentFile  = null;
        private File metadataFile = null;
        
        // Cached file info (to avoid repeated calls to stat syscall on the same file)
        private Metadata cachedMetadata    = null;
        private Boolean  cachedIsDirectory = null;
        private long     cachedSizeInBytes = 0L;
        private boolean  contentIsInPlace  = false;

        
        public FilesystemVersion(final String versionLabel,
                                 final File   contentFile,
                                 final File   metadataFile)
        {
            if (versionLabel != null)
            {
                versionNumber = new BigDecimal(versionLabel);
            }
            else
            {
                versionNumber = null;
            }
            
            setContentFile(contentFile);
            setMetadataFile(metadataFile);
        }
        
        /* package */ File getContentFile()
        {
            return(contentFile);
        }
        
        private void setContentFile(final File contentFile)
        {
            if (contentFile != null)
            {
                this.contentFile       = contentFile;
                this.cachedIsDirectory = contentFile.isDirectory();
                
                if (cachedIsDirectory)
                {
                    cachedSizeInBytes = 0L;
                }
                else
                {
                    cachedSizeInBytes = contentFile.length();
                }
            }
        }
        
        private void setMetadataFile(final File metadataFile)
        {
            this.metadataFile = metadataFile;
        }
        
        private Metadata getRawMetadata()
        {
            loadMetadataIfNecessary();
            return(cachedMetadata);
        }
        
        private Boolean isDirectory()
        {
            return(cachedIsDirectory);
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getVersionNumber()
         */
        @Override
        public BigDecimal getVersionNumber()
        {
            return(versionNumber);
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
            return(metadataFile.getAbsolutePath());
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#hasContent()
         */
        @Override
        public boolean hasContent()
        {
            return(contentFile != null && !contentFile.isDirectory());
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getContentSource()
         */
        @Override
        public String getContentSource()
        {
            return(contentFile.getAbsolutePath());
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
            writer.guessMimetype(contentFile.getName());
            writer.putContent(contentFile);
            writer.guessEncoding();
        }

        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(final FilesystemVersion other)
        {
            if (this.versionNumber == null && other.versionNumber == null) return(0);
            if (this.versionNumber == null) return(1);
            if (other.versionNumber == null) return(-1);
            return(this.versionNumber.compareTo(other.versionNumber));
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object other)
        {
            if (this == other) return(true);
            if (!(other instanceof FilesystemVersion)) return(false);

            FilesystemVersion otherFilesystemVersion = (FilesystemVersion)other;
            return(compareTo(otherFilesystemVersion) == 0);
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            return(versionNumber.hashCode());
        }
        
        
        private final synchronized void loadMetadataIfNecessary()
        {
            if (cachedMetadata == null)
            {
                cachedMetadata   = metadataLoader.loadMetadata(metadataFile);
                contentIsInPlace = false;
                
                if (contentFile != null)
                {
                    try
                    {
                        final Path                path       = contentFile.toPath();
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
                        if (!contentFile.isDirectory() && isInContentStore(configuredContentStore, contentFile))
                        {
                            final ContentData contentData = buildContentProperty(mimeTypeService, configuredContentStore, contentFile);
                            
                            if (contentData != null)
                            {
                                // We have valid in-place content
                                contentIsInPlace = true;
                                cachedMetadata.addProperty(ContentModel.PROP_CONTENT.toString(), contentData);
                            }
                            else
                            {
                                if (warn(log)) warn (log, "Unable to in-place import '" + getFileName(contentFile) + "'. Will stream it instead.");
                            }
                        }
                    }
                    catch (final IOException ioe)
                    {
                        // Not much we can do in this case - log it and keep on truckin'
                        if (warn(log)) warn(log, "Unable to read file attributes for " + contentFile.getAbsolutePath() + ". Creation and modification timestamps will be system generated.", ioe);
                    }
                }
            }
        }
    }
}
