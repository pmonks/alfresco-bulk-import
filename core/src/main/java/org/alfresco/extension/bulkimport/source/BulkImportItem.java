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

package org.alfresco.extension.bulkimport.source;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.alfresco.service.cmr.repository.ContentWriter;


/**
 * This interface represents a "bulk import item" - a series of versions that
 * will be imported as a single node (content OR space) into the repository.
 * 
 * Notes:
 * <ol>
 * <li>Implementations of this interface must be as lightweight as possible, as
 * there may be many jinstances of these in memory at once.  For this reason
 * it is recommended that implementations only maintain references to the underlying
 * content and metadata sources (e.g. <code>java.io.File</code> objects, rather
 * than <code>InputStreams</code> or a <code>Map</code> containing metadata).</li>
 * <li>The import tool will call the putContent and getMetadata methods at most once,
 * allowing implementations to convert those references to actual values exactly once,
 * without retaining those values.</li>
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * </ol>
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public interface BulkImportItem
{
    /**
     * @return The parent path of this node as a list of individual path elements <i>(may be null or empty)</i>.
     */
    public List<String> getParentPath();
    
    /**
     * @return The parent association type to use for this node <i>(may be null)</i>.
     */
    public String getParentAssoc();
    
    /**
     * @return The namespace of the node <i>(may be null)</i>.
     */
    public String getNamespace();
    
    /**
     * @return The name of the node as it is / should be in the repository <i>(must not be null, empty or blank, and must meet Alfresco's naming rules for nodes)</i>.
     */
    public String getName();
    
    /**
     * @return True if this item is a directory (cm:folder or descendent type), false otherwise.
     */
    public boolean isDirectory();
    
    /**
     * @return The set of versions comprising this item <i>(must not be null or empty, and should contain only one entry if this item is a directory)</i>.
     */
    public SortedSet<Version> getVersions();
    
    
    /**
     * This interface identifies a single version within an importable item.
     * 
     * Invariants:
     * hasContent() || hasMetadata() == true
     *
     * @author Peter Monks (pmonks@gmail.com)
     *
     */
    public interface Version
    {
        /**
         * @return True if this is a major version increment, false for minor version increment.
         */
        public boolean isMajor();

        /**
         * @return The type for this version <i>(may be null, but must be a valid Alfresco type if not null)</i>.
         */
        public String getType();
        
        /**
         * @return The aspect(s) for this version <i>(may be null or empty)</i>.
         */
        public Set<String> getAspects();
        
        /**
         * @return True if this version has content, false if not.
         */
        public boolean hasContent();
        
        /**
         * @return A reference to the source of the content (used in error messages) <i>(may be null if hasContent() = false)</i>.
         */
        public String getContentSource();
        
        /**
         * @return True if the content is already in-place (in which case the content url property must be returned from the <code>getMetadata</code> call).
         */
        public boolean contentIsInPlace();
        
        /**
         * Called when the content of this version is ready to be streamed into the repository.
         * 
         * Notes:
         * <ol>
         * <li>This method is not called if contentIsInPlace() returns true.</li>
         * <li>It is the implementer's responsibility to set the MIME type, encoding and/or locale of the content being written.
         * Neither the import tool nor Alfresco will "guess" these values.</li>
         * </ol>
         * 
         * @param writer The ContentWriter to use for this version <i>(will not be null)</i>.
         */
        public void putContent(ContentWriter writer);
        
        /**
         * @return True if this version has metadata, false if not.
         */
        public boolean hasMetadata();

        /**
         * @return A reference to the source of the metadata (used in error messages) <i>(may be null if hasMetadata() = false)</i>.
         */
        public String getMetadataSource();
        
        /**
         * @return The metadata of this version, if any <i>(may return null or empty)</i>.
         */
        public Map<String,Serializable> getMetadata();
    }
}    



/*    
    
    
    
    
    
    private final String   parentFilename;
    private final NodeType nodeType;
    
    private SortedSet<VersionedContentAndMetadata> versionEntries = null;
    
    
    public ImportableItem(final String   parentFilename,
                          final NodeType nodeType)
    {
        // PRECONDITIONS
        assert parentFilename != null             : "parentFilename must not be null.";
        assert parentFilename.trim().length() > 0 : "parentFilename must not be blank or empty.";
        
        // Body
        this.parentFilename = parentFilename;
        this.nodeType       = isDirectory;
    }

    
    public String getParentFilename()
    {
        return(parentFilename);
    }
    
    public 
    
    
    
    public NodeType getFileType()
    {
        NodeType result = NodeType.UNKNOWN;
        
        if (headRevision.contentFileExists())
        {
            result = headRevision.getContentFileType();
        }
        else if (hasVersionEntries())
        {
            for (final VersionedContentAndMetadata versionEntry : versionEntries)
            {
                if (versionEntry.contentFileExists())
                {
                    result = versionEntry.getContentFileType();
                }
            }
        }
        
        return(result);
    }
    
    
    public boolean isValid()
    {
        return(headRevision.contentFileExists() || headRevision.metadataFileExists() || hasVersionEntries());
    }
    
    public ContentAndMetadata getHeadRevision()
    {
        return(headRevision);
    }
    
    
    public int weight()
    {
        int totalWeightOfVersions = 0;
        
        if (hasVersionEntries())
        {
            for (final VersionedContentAndMetadata versionEntry : versionEntries)
            {
                totalWeightOfVersions += versionEntry.weight();
            }
        }
        
        return(headRevision.weight() + totalWeightOfVersions);
    }

    public boolean hasVersionEntries()
    {
        return(versionEntries != null && versionEntries.size() > 0);
    }
    
    public VersionedContentAndMetadata getVersionEntry(final String versionLabel)
    {
        VersionedContentAndMetadata result = null;
    
        if (hasVersionEntries())
        {
            for (final ImportableItem.VersionedContentAndMetadata versionEntry : versionEntries)
            {
                if (versionEntry.getVersionLabel().equals(versionLabel))
                {
                    result = versionEntry;
                    break;
                }
            }
        }
    
        return(result);
    }
    
    
    public Set<VersionedContentAndMetadata> getVersionEntries()
    {
        return(Collections.unmodifiableSet(versionEntries));
    }
    
    
    public void addVersionEntry(final VersionedContentAndMetadata versionEntry)
    {
        if (versionEntry != null)
        {
            if (versionEntries == null)
            {
                versionEntries = new TreeSet<VersionedContentAndMetadata>();
            }
                
            versionEntries.add(versionEntry);
        }
    }
    
    @Override
    public String toString()
    {
        return(new ToStringBuilder(this)
               .append("headRevision", headRevision)
               .append("versions",     versionEntries)
               .toString());
    }
    
    public class ContentAndMetadata
    {
        private File     contentFile           = null;
        private boolean  contentFileExists     = false;
        private boolean  contentFileIsReadable = false;
        private NodeType contentFileType       = NodeType.UNKNOWN;
        private long     contentFileSize       = -1;
        private Date     contentFileCreated    = null;
        private Date     contentFileModified   = null;
        private File     metadataFile          = null;
        private long     metadataFileSize      = -1;

        
        
        public final String getParentFileName()
        {
            return(parentFilename);
        }
        
        public final File getContentFile()
        {
            return(contentFile);
        }
        
        public final void setContentFile(final File contentFile)
        {
            this.contentFile = contentFile;
            
            if (contentFile != null)
            {
                // stat the file, to find out a few key details
                contentFileExists = contentFile.exists();
                
                if (contentFileExists)
                {
                    contentFileIsReadable = contentFile.canRead();
                    contentFileSize       = contentFile.length();
                    contentFileModified   = new Date(contentFile.lastModified());
                    contentFileCreated    = contentFileModified;    // TODO: determine proper file creation time (awaiting JDK 1.7 NIO2 library)
                    
                    if (contentFile.isFile())
                    {
                        contentFileType = NodeType.FILE;
                    }
                    else if (contentFile.isDirectory())
                    {
                        contentFileType = NodeType.DIRECTORY;
                    }
                    else
                    {
                        contentFileType = NodeType.OTHER;
                    }
                }
            }
        }
        
        public final boolean contentFileExists()
        {
            return(contentFileExists);
        }
        
        public final boolean isContentFileReadable()
        {
            return(contentFileIsReadable);
        }
        
        public final NodeType getContentFileType()
        {
            return(contentFileType);
        }
        
        public final long getContentFileSize()
        {
            if (!contentFileExists())
            {
                throw new IllegalStateException("Cannot determine content file size if content file doesn't exist.");
            }
            
            return(contentFileSize);
        }
        
        public final Date getContentFileCreatedDate()
        {
            if (!contentFileExists())
            {
                throw new IllegalStateException("Cannot determine content file creation date if content file doesn't exist.");
            }
            
            return(contentFileCreated);
        }
        
        public final Date getContentFileModifiedDate()
        {
            if (!contentFileExists())
            {
                throw new IllegalStateException("Cannot determine content file modification date if content file doesn't exist.");
            }
            
            return(contentFileModified);
        }
        
        public final boolean metadataFileExists()
        {
            return(metadataFile != null);
        }
        
        public final File getMetadataFile()
        {
            return(metadataFile);
        }
        
        public final void setMetadataFile(final File metadataFile)
        {
            if (metadataFile != null && metadataFile.exists())
            {
                this.metadataFile     = metadataFile;
                this.metadataFileSize = metadataFile.length();
            }
        }
        
        public final long getMetadataFileSize()
        {
            if (!metadataFileExists())
            {
                throw new IllegalStateException("Cannot determine metadata file size if metadata file doesn't exist.");
            }
            
            return(metadataFileSize);
        }
        public final int weight()
        
        {
            return((contentFile  == null || !contentFileExists ? 0 : 1) +
                   (metadataFile == null ? 0 : 1));
        }

        @Override
        public String toString()
        {
            return(new ToStringBuilder(this)
                   .append("contentFile",  (contentFileExists    ? contentFile.getAbsolutePath()  : null))
                   .append("metadatafile", (metadataFile != null ? metadataFile.getAbsolutePath() : null))
                   .toString());
        }
    }
    
    
    public class VersionedContentAndMetadata
        extends ContentAndMetadata
        implements Comparable<VersionedContentAndMetadata>
    {
        private final Pattern VERSION_NUMBER_PATTERN = Pattern.compile(DirectoryAnalyser.VERSION_LABEL_REGEX);
        
        private final int majorVersion;
        private final int minorVersion;


        public VersionedContentAndMetadata(final String versionLabel)
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
        }
        
        public VersionedContentAndMetadata(final int majorVersion,
                                           final int minorVersion)
        {
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
        }
        
        public final int getMajorVersion()
        {
            return(majorVersion);
        }
        
        public final int getMinorVersion()
        {
            return(minorVersion);
        }
        
        public final String getVersionLabel()
        {
            return(majorVersion + "." + minorVersion);
        }
        
        @Override
        public String toString()
        {
            return(new ToStringBuilder(this)
                   .append("version", getVersionLabel())
                   .appendSuper("")
                   .toString());
        }

        public int compareTo(final VersionedContentAndMetadata other)
        {
            return(this.majorVersion < other.majorVersion ? -1 :
                   this.majorVersion > other.majorVersion ?  1 :
                   this.minorVersion < other.minorVersion ? -1 :
                   this.minorVersion > other.minorVersion ?  1 : 0);
        }

        @Override
        public boolean equals(final Object other)
        {
            if (this == other)
            {
                return(true);
            }

            if (!(other instanceof VersionedContentAndMetadata))
            {
                return(false);
            }

            VersionedContentAndMetadata otherVCAM = (VersionedContentAndMetadata)other;

            return(this.majorVersion == otherVCAM.majorVersion &&
                   this.minorVersion == otherVCAM.minorVersion);
        }

        @Override
        public int hashCode()
        {
            return(majorVersion * 17 + minorVersion);
        }
    }

    
    public enum NodeType
    {
        FILE,
        DIRECTORY,
        OTHER,
        UNKNOWN
    }
    
}
*/