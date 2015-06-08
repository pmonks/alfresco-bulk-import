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


package org.alfresco.extension.bulkimport.source.sample;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.service.cmr.repository.ContentWriter;

import org.alfresco.extension.bulkimport.source.AbstractBulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportItem;



/**
 * This class represents the <code>BulkImportSource</code> portion of the
 * sample custom bulk import source.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class SampleSourceImportItem
    extends AbstractBulkImportItem
{
    private final static Log log = LogFactory.getLog(SampleSourceImportItem.class);
    
    private final String                            parentPath;
    private final String                            name;
    private final boolean                           isDirectory;
    private final NavigableSet<SampleSourceVersion> versions;
    
    
    public SampleSourceImportItem(final String  parentPath,
                                  final String  name,
                                  final boolean isDirectory,
                                  final int     numVersions)
    {
        this.parentPath  = parentPath;
        this.name        = name;
        this.isDirectory = isDirectory;
        this.versions    = new TreeSet<SampleSourceVersion>();

        // Add at least one version
        this.versions.add(new SampleSourceVersion(isDirectory, BigDecimal.ONE));
        
        if (!isDirectory && numVersions > 1)
        {
            for (int i = 1; i < numVersions; i++)
            {
                this.versions.add(new SampleSourceVersion(isDirectory, BigDecimal.valueOf(i + 1)));
            }
        }
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getRelativePath()
     */
    @Override
    public String getRelativePathOfParent()
    {
        return(parentPath);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getParentAssoc()
     */
    @Override
    public String getParentAssoc()
    {
        // Sample Source only supports the default cm:contains assoc.
        return(null);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItem#getNamespace()
     */
    @Override
    public String getNamespace()
    {
        // Sample Source only supports the default "cm" namespace.
        return(null);
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
        return(isDirectory);
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


    private final class SampleSourceVersion
        implements BulkImportItem.Version,
                   Comparable<SampleSourceVersion>
    {
        private final BigDecimal versionNumber;
        private final String     content;
        private final String     type;
        
        public SampleSourceVersion(final boolean    isDirectory,
                                   final BigDecimal versionNumber)
        {
            this.type          = isDirectory ? "cm:folder" : "cm:content";
            this.versionNumber = versionNumber;
            
            // Synthesise some faux content
            // IMPORTANT NOTE: in a typical bulk import source, content should not be obtained until putContent is called - that ensures optimal performance
            this.content = isDirectory ? null : "This is the content of version " + String.valueOf(versionNumber) + " of " + getName() + ".";
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
            return(type);
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getAspects()
         */
        @Override
        public Set<String> getAspects()
        {
            // The Sample Source doesn't support aspects
            return(null);
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#hasContent()
         */
        @Override
        public boolean hasContent()
        {
            return(!isDirectory);
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getContentSource()
         */
        @Override
        public String getContentSource()
        {
            return("Synthetic Content");
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#sizeInBytes()
         */
        @Override
        public long sizeInBytes()
        {
            return(content == null ? 0 : content.length());
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#contentIsInPlace()
         */
        @Override
        public boolean contentIsInPlace()
        {
            // The Sample Source doesn't support in-place content
            return(false);
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#putContent(org.alfresco.service.cmr.repository.ContentWriter)
         */
        @Override
        public void putContent(final ContentWriter writer)
        {
            // Provide some hardcoded content
            writer.setMimetype("text/plain");
            writer.setEncoding("UTF8");
            writer.putContent(content);
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#hasMetadata()
         */
        @Override
        public boolean hasMetadata()
        {
            // The Sample Source doesn't support metadata
            return(false);
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getMetadataSource()
         */
        @Override
        public String getMetadataSource()
        {
            // The Sample Source doesn't support metadata
            return(null);
        }

        /**
         * @see org.alfresco.extension.bulkimport.source.BulkImportItem.Version#getMetadata()
         */
        @Override
        public Map<String, Serializable> getMetadata()
        {
            // The Sample Source doesn't support metadata
            return(null);
        }

        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(final SampleSourceVersion other)
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
            if (!(other instanceof SampleSourceVersion)) return(false);

            SampleSourceVersion otherSampleSourceVersion = (SampleSourceVersion)other;
            return(compareTo(otherSampleSourceVersion) == 0);
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            return(versionNumber.hashCode());
        }
    }
    
}
