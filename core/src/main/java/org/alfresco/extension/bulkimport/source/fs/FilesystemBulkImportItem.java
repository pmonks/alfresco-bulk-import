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


package org.alfresco.extension.bulkimport.source.fs;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.fs.MetadataLoader.Metadata;

/**
 * This class TODO
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public class FilesystemBulkImportItem
    implements BulkImportItem
{
    private final ServiceRegistry serviceRegistry;
    private final MetadataLoader  metadataLoader;
    
    private final List<String>       importRelativePathElements;
    private final String             name;
    private final SortedSet<Version> versions;
    
    
    public FilesystemBulkImportItem(final ServiceRegistry  serviceRegistry,
                                    final MetadataLoader   metadataLoader,
                                    final String           importRelativePath,
                                    final String           name,
                                    final List<ImportFile> constituentFiles)
    {
        // PRECONDITIONS
        assert serviceRegistry         != null : "serviceRegistry must not be null.";
        assert name                    != null : "name must not be null.";
        assert name.trim().length()    > 0     : "name must not be empty or blank.";
        assert constituentFiles        != null : "constituentFiles must not be null.";
        assert constituentFiles.size() > 0     : "constituentFiles must not be empty.";
        
        // Body
        this.serviceRegistry            = serviceRegistry;
        this.metadataLoader             = metadataLoader;
        this.importRelativePathElements = importRelativePath == null ? null : Arrays.asList(importRelativePath.split("\\" + File.pathSeparatorChar));
        this.name                       = name;
        this.versions                   = new TreeSet<Version>();
        
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
                                            importFile.isMetadata() ? null : importFile.getFile(), importFile.isMetadata() ? importFile.getFile() : null);
            
            versions.add(version);
            versionsIndexedByVersionLabel.put(importFile.getVersionLabel(), version);
        }
        else
        {
            if (importFile.isMetadata())
            {
                version.setMetadataFile(importFile.getFile());
            }
            else
            {
                version.setContentFile(importFile.getFile());
            }
        }
    }
    
    
    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getParent(org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    public NodeRef getParent(final NodeRef target)
    {
        NodeRef result = null;
        
        if (importRelativePathElements != null && importRelativePathElements.size() > 0)
        {
            FileInfo fileInfo = null;
                
            try
            {
                fileInfo = serviceRegistry.getFileFolderService().resolveNamePath(target, importRelativePathElements, false);
            }
            catch (final FileNotFoundException fnfe)  // This should never be triggered due to the last parameter in the resolveNamePath call
            {
                // Bloody Java and its bloody stupid checked exceptions!!
                throw new IllegalStateException("Could not find path '" + String.valueOf(importRelativePathElements) + "' underneath node '" + String.valueOf(target) + "'.", fnfe);
            }
            
            //#################################
            //#### VERY IMPORTANT TODO!!!! ####
            //#################################
            //####TODO: consider re-queuing the batch in this case?
            if (fileInfo == null) throw new IllegalStateException("Could not find path '" + String.valueOf(importRelativePathElements) + "' underneath node '" + String.valueOf(target) + "'.  Out-of-order batch submission?");
            
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
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getNamespace()
     */
    @Override
    public String getNamespace()
    {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return false;
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getVersions()
     */
    @Override
    public SortedSet<Version> getVersions()
    {
        return(versions);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#weight()
     */
    @Override
    public int weight()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    
    public class FilesystemVersion
        implements BulkImportItem.Version,
                   Comparable<FilesystemVersion>
    {
        private final Pattern VERSION_NUMBER_PATTERN = Pattern.compile(DirectoryAnalyser.VERSION_LABEL_REGEX);
        
        private final int  majorVersion;
        private final int  minorVersion;
        
        private File contentFile;
        private File metadataFile;
        
        private Metadata cachedMetadata;

        
        public FilesystemVersion(final int majorVersion,
                                 final int minorVersion)
        {
            this(majorVersion, minorVersion, null, null);
        }
        
        public FilesystemVersion(final int majorVersion,
                                 final int minorVersion,
                                 final File contentFile,
                                 final File metadataFile)
        {
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.contentFile  = contentFile;
            this.metadataFile = metadataFile;
        }
        
        public FilesystemVersion(final String versionLabel)
        {
            this(versionLabel, null, null);
        }
        
        public FilesystemVersion(final String versionLabel,
                                 final File   contentFile,
                                 final File   metadataFile)
        {
            Matcher m = VERSION_NUMBER_PATTERN.matcher(versionLabel);
            
            if (!m.matches())
            {
                throw new IllegalArgumentException(versionLabel + " is not a valid version label.");
            }
            
            String majorVersionStr = m.group(1);
            String minorVersionStr = m.group(3);
            
            majorVersion = Integer.parseInt(majorVersionStr);
            
            if (minorVersionStr != null)
            {
                minorVersion = Integer.parseInt(minorVersionStr);
            }
            else
            {
                minorVersion = 0;
            }
            
            this.contentFile  = contentFile;
            this.metadataFile = metadataFile;
        }
        
        public File getContentFile()
        {
            return(contentFile);
        }
        
        public void setContentFile(final File contentFile)
        {
            this.contentFile = contentFile;
        }
        
        public File getMetadataFile()
        {
            return(metadataFile);
        }
        
        public void setMetadataFile(final File metadataFile)
        {
            this.metadataFile = metadataFile;
        }
        
        
        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getVersionNumber()
         */
        @Override
        public BigDecimal getVersionNumber()
        {
            return(new BigDecimal(majorVersion + "." + minorVersion));
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
            return(metadataFile != null);
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
            return(contentFile != null);
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
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#contentIsInPlace()
         */
        @Override
        public boolean contentIsInPlace()
        {
            // TODO Auto-generated method stub
            return(false);
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#putContent(org.alfresco.service.cmr.repository.ContentWriter)
         */
        @Override
        public void putContent(final ContentWriter writer)
        {
            writer.guessMimetype(contentFile.getName());
            writer.putContent(contentFile);
            writer.guessEncoding();  //TODO: double check that this is necessary, and if so that it's in the correct sequence (i.e. after streaming in the bytes).
        }

        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(final FilesystemVersion other)
        {
            return(this.majorVersion < other.majorVersion ? -1 :
                   this.majorVersion > other.majorVersion ?  1 :
                   this.minorVersion < other.minorVersion ? -1 :
                   this.minorVersion > other.minorVersion ?  1 : 0);
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object other)
        {
            if (this == other)
            {
                return(true);
            }

            if (!(other instanceof FilesystemVersion))
            {
                return(false);
            }

            FilesystemVersion otherFilesystemVersion = (FilesystemVersion)other;

            return(this.majorVersion == otherFilesystemVersion.majorVersion &&
                   this.minorVersion == otherFilesystemVersion.minorVersion);
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            return(majorVersion * 17 + minorVersion);
        }
        
        
        private final void loadMetadataIfNecessary()
        {
            if (cachedMetadata == null)
            {
                cachedMetadata = metadataLoader.loadMetadata(metadataFile);
            }
        }
    }
}
