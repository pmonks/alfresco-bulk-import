/*
 * Copyright (C) 2007-2013 Peter Monks.
 *               2010-2011 Ryan McVeigh Fixed issues #18 and #62.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.extension.bulkimport.impl.AbstractBulkImporter;
import org.alfresco.extension.bulkimport.impl.BulkImportStatusImpl;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportItem.NodeType;


/**
 * This class provides the implementation for directory analysis, the process by
 * which a directory listing of files is broken up into ImportableItems.
 * 
 * @author Peter Monks (pmonks@alfresco.com)
 */
public final class DirectoryAnalyserImpl
    implements DirectoryAnalyser
{
    private final static Log log = LogFactory.getLog(DirectoryAnalyserImpl.class);
    
    private final static Pattern VERSION_SUFFIX_PATTERN = Pattern.compile(".+" + VERSION_SUFFIX_REGEX);

    private final MetadataLoader       metadataLoader;
    private final BulkImportStatusImpl importStatus;
    
    
    
    public DirectoryAnalyserImpl(final MetadataLoader       metadataLoader,
                                 final BulkImportStatusImpl importStatus)
    {
        this.metadataLoader = metadataLoader;
        this.importStatus   = importStatus;
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.fs.DirectoryAnalyser#analyseDirectory(java.io.File)
     */
    public AnalysedDirectory analyseDirectory(final File directory)
        throws InterruptedException
    {
        final AnalysedDirectory        result          = new AnalysedDirectory();
        final Map<File,BulkImportItem> importableItems = new HashMap<File,BulkImportItem>();
        long                           start;
        long                           end;
        
        if (log.isDebugEnabled()) log.debug("Analysing directory " + AbstractBulkImporter.getFileName(directory) + "...");

        start = System.nanoTime();
        result.originalListing = Arrays.asList(directory.listFiles());
        end = System.nanoTime();
        if (log.isTraceEnabled()) log.trace("List directory took: " + (float)(end - start) / (1000 * 1000 * 1000 )+ "s");

        // Build up the list of ImportableItems from the directory listing
        start = System.nanoTime();
        for (final File file : result.originalListing)
        {
            if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted.  Terminating early.");
            
            if (file.canRead())
            {
                if (isVersionFile(file))
                {
                    addVersionFile(importableItems, file);
                    importStatus.incrementNumberOfFilesScanned();
                }
                else if (isMetadataFile(file))
                {
                    addMetadataFile(importableItems, file);
                    importStatus.incrementNumberOfFilesScanned();
                }
                else
                {
                    boolean isDirectory = addParentFile(importableItems, file);
                    
                    if (isDirectory)
                    {
                        importStatus.incrementNumberOfFoldersScanned();
                    }
                    else
                    {
                        importStatus.incrementNumberOfFilesScanned();
                    }
                }
            }
            else
            {
                if (log.isWarnEnabled()) log.warn("Skipping unreadable file/directory '" + AbstractBulkImporter.getFileName(file) + "'.");
                
                importStatus.incrementNumberOfUnreadableEntries();
            }
        }
        end = System.nanoTime();
        if (log.isTraceEnabled()) log.trace("Build list of importable items took: " + (float)(end - start) / (1000 * 1000 * 1000 )+ "s");

        result.importableItems = new ArrayList<BulkImportItem>(importableItems.values());

        // Finally, remove any items from the list that aren't valid
        start = System.nanoTime();
        Iterator<BulkImportItem> iter = result.importableItems.iterator();

        while (iter.hasNext())
        {
            BulkImportItem importableItem = iter.next();

            if (!importableItem.isValid())
            {
                iter.remove();
            }
        }
        
        end = System.nanoTime();
        if (log.isTraceEnabled()) log.trace("Filter invalid importable items took: " + (float)(end - start) / (1000 * 1000 * 1000 )+ "s");

        if (log.isDebugEnabled()) log.debug("Finished analysing directory " + AbstractBulkImporter.getFileName(directory) + ".");

        return(result);
    }


    private boolean isVersionFile(final File file)
    {
        Matcher matcher = VERSION_SUFFIX_PATTERN.matcher(file.getName());

        return(matcher.matches());
    }


    private boolean isMetadataFile(final File file)
    {
        boolean result = false;
        
        if (metadataLoader != null)
        {
            result = file.getName().endsWith(MetadataLoader.METADATA_SUFFIX + metadataLoader.getMetadataFileExtension());
        }
        
        return(result);
    }


    private void addVersionFile(final Map<File,BulkImportItem> importableItems, final File versionFile)
    {
        File    parentContentFile = getParentOfVersionFile(versionFile);
        boolean isContentVersion  = false;

        if (isMetadataFile(parentContentFile))
        {
            parentContentFile = getParentOfMetadatafile(parentContentFile);
            isContentVersion  = false;
        }
        else
        {
            isContentVersion = true;
        }

        BulkImportItem                             importableItem = findOrCreateImportableItem(importableItems, parentContentFile);
        String                                     versionLabel   = getVersionLabel(versionFile);
        BulkImportItem.VersionedContentAndMetadata versionEntry   = findOrCreateVersionEntry(importableItem, versionLabel);

        if (isContentVersion)
        {
            versionEntry.setContentFile(versionFile);
        }
        else
        {
            versionEntry.setMetadataFile(versionFile);
        }
    }


    private void addMetadataFile(final Map<File,BulkImportItem> importableItems, final File metadataFile)
    {
        final File parentContentfile = getParentOfMetadatafile(metadataFile);

        BulkImportItem importableItem = findOrCreateImportableItem(importableItems, parentContentfile);

        importableItem.getHeadRevision().setMetadataFile(metadataFile);
    }


    private boolean addParentFile(final Map<File,BulkImportItem> importableItems, final File contentFile)
    {
        BulkImportItem importableItem = findOrCreateImportableItem(importableItems, contentFile);

        importableItem.getHeadRevision().setContentFile(contentFile);
        
        return(importableItem.getHeadRevision().getContentFileType() == NodeType.DIRECTORY);
    }


    private BulkImportItem findOrCreateImportableItem(final Map<File,BulkImportItem> importableItems,
                                                      final File                     contentFile)
    {
        BulkImportItem result = importableItems.get(contentFile);

        // We didn't find it, so create it
        if (result == null)
        {
            result = new BulkImportItem(contentFile.getName());
            result.getHeadRevision().setContentFile(contentFile);
            importableItems.put(contentFile, result);
        }

        return(result);
    }


    private BulkImportItem.VersionedContentAndMetadata findOrCreateVersionEntry(final BulkImportItem importableItem, final String versionLabel)
    {
        BulkImportItem.VersionedContentAndMetadata result = importableItem.getVersionEntry(versionLabel);

        if (result == null)
        {
            result = importableItem.new VersionedContentAndMetadata(versionLabel);
            
            importableItem.addVersionEntry(result);
        }

        return (result);
    }


    private String getVersionLabel(final File versionFile)
    {
        String result = null;

        if (!isVersionFile(versionFile))
        {
            throw new IllegalStateException(AbstractBulkImporter.getFileName(versionFile) + " is not a version file.");
        }

        Matcher matcher = VERSION_SUFFIX_PATTERN.matcher(versionFile.getName());

        if (matcher.matches())
        {
            result = matcher.group(1);
        }
        else
        {
            throw new IllegalStateException(AbstractBulkImporter.getFileName(versionFile) + " has a malformed version label."); 
        }

        return(result);
    }


    private File getParentOfVersionFile(final File versionFile)
    {
        File result = null;

        if (!isVersionFile(versionFile))
        {
            throw new IllegalStateException(AbstractBulkImporter.getFileName(versionFile) + " is not a version file.");
        }

        String parentFilename = versionFile.getName().replaceFirst(VERSION_SUFFIX_REGEX, "");

        result = new File(versionFile.getParent(), parentFilename);
        
        return(result);
    }


    private File getParentOfMetadatafile(final File metadataFile)
    {
        File result = null;

        if (!isMetadataFile(metadataFile))
        {
            throw new IllegalStateException(AbstractBulkImporter.getFileName(metadataFile) + " is not a metadata file.");
        }

        String name = metadataFile.getName();
        String contentName = name.substring(0, name.length() - (MetadataLoader.METADATA_SUFFIX + metadataLoader.getMetadataFileExtension()).length());

        result = new File(metadataFile.getParent(), contentName);

        return(result);
    }
}
