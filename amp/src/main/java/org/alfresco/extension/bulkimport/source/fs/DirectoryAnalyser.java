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

package org.alfresco.extension.bulkimport.source.fs;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.util.Pair;

import org.alfresco.extension.bulkimport.source.BulkImportSourceStatus;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;
import static org.alfresco.extension.bulkimport.source.fs.FilesystemSourceUtils.*;


/**
 * This interface defines a directory analyser. This is the process by which
 * the contents of a source directory are grouped together into a list of
 * <code>FilesystemBulkImportItem</code>s. 
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public final class DirectoryAnalyser
{
    private final static Log log = LogFactory.getLog(DirectoryAnalyser.class);
    
    // Status counters    
    private final static String COUNTER_NAME_FILES_SCANNED       = "Files scanned";
    private final static String COUNTER_NAME_DIRECTORIES_SCANNED = "Directories scanned";
    private final static String COUNTER_NAME_UNREADABLE_ENTRIES  = "Unreadable entries";
        
    private final static String[] COUNTER_NAMES = { COUNTER_NAME_FILES_SCANNED,
                                                    COUNTER_NAME_DIRECTORIES_SCANNED,
                                                    COUNTER_NAME_UNREADABLE_ENTRIES };
    

    private final ServiceRegistry serviceRegistry;
    private final ContentStore    configuredContentStore;
    private final MetadataLoader  metadataLoader;
    
    private BulkImportSourceStatus importStatus;
    
    
    
    public DirectoryAnalyser(final ServiceRegistry serviceRegistry,
                             final ContentStore    configuredContentStore,
                             final MetadataLoader  metadataLoader)
    {
        // PRECONDITIONS
        assert serviceRegistry        != null : "serviceRegistry must not be null.";
        assert configuredContentStore != null : "configuredContentStore must not be null.";
        assert metadataLoader         != null : "metadataLoader must not be null.";
        assert importStatus           != null : "importStatus must not be null.";
        
        // Body
        this.serviceRegistry        = serviceRegistry;
        this.configuredContentStore = configuredContentStore;
        this.metadataLoader         = metadataLoader;
    }
    
    
    public void init(final BulkImportSourceStatus importStatus)
    {
        this.importStatus = importStatus;
        
        importStatus.preregisterSourceCounters(COUNTER_NAMES);
    }
    
    
    /**
     * Analyses the given directory.
     * 
     * @param sourceDirectory The source directory for the entire import (note: <u>must</u> be a directory) <i>(must not be null)</i>.
     * @param directory The directory to analyse (note: <u>must</u> be a directory) <i>(must not be null)</i>.
     * @return An <code>AnalysedDirectory</code> object <i>(will not be null)</i>.
     * @throws InterruptedException If the thread executing the method is interrupted.
     */
    public Pair<List<FilesystemBulkImportItem>, List<FilesystemBulkImportItem>> analyseDirectory(final File sourceDirectory, final File directory)
        throws InterruptedException
    {
        // PRECONDITIONS
        if (sourceDirectory == null) throw new IllegalArgumentException("sourceDirectory cannot be null.");
        if (directory       == null) throw new IllegalArgumentException("directory cannot be null.");
        
        // Body
        if (debug(log)) debug(log, "Analysing directory " + getFileName(directory) + "...");
        
        Pair<List<FilesystemBulkImportItem>, List<FilesystemBulkImportItem>> result                        = null;
        File[]                                                               directoryListing              = null;
        long                                                                 analysisStart                 = 0L;
        long                                                                 analysisEnd                   = 0L;
        long                                                                 start                         = 0L;
        long                                                                 end                           = 0L;
        String                                                               sourceRelativeParentDirectory = sourceDirectory.toPath().relativize(directory.toPath()).toString();  // Note: JDK 1.7 specific
        

        // List the directory
        start         = System.nanoTime();
        analysisStart = start;
        directoryListing = directory.listFiles();
        end = System.nanoTime();
        if (trace(log)) trace(log, "List directory (" + directoryListing.length + " entries) took: " + (float)(end - start) / (1000 * 1000 * 1000) + "s.");

        // Build up the list of items from the directory listing
        start = System.nanoTime();
        result = analyseDirectory(sourceRelativeParentDirectory, directoryListing);
        end = System.nanoTime();
        if (trace(log)) trace(log, "Convert directory listing to set of filesystem import items took: " + (float)(end - start) / (1000 * 1000 * 1000) + "s.");
        
        analysisEnd = end;
        if (debug(log)) debug(log, "Finished analysing directory " + getFileName(directory) + ", in " + (float)(analysisEnd - analysisStart) / (1000 * 1000 * 1000) + "s.");

        return(result);
    }
    
    
    private Pair<List<FilesystemBulkImportItem>, List<FilesystemBulkImportItem>> analyseDirectory(final String sourceRelativeParentDirectory, final File[] directoryListing)
        throws InterruptedException
    {
        Pair<List<FilesystemBulkImportItem>, List<FilesystemBulkImportItem>> result = null;
        
        if (directoryListing != null)
        {
            // This needs some Clojure, desperately...
            Map<String, SortedMap<BigDecimal, Pair<File, File>>> categorisedFiles = categoriseFiles(directoryListing);
            
            if (debug(log)) debug(log, "Categorised files: " + String.valueOf(categorisedFiles));
            
            result = constructImportItems(sourceRelativeParentDirectory, categorisedFiles);
        }
        
        return(result);
    }
    
    
    private Map<String, SortedMap<BigDecimal, Pair<File, File>>> categoriseFiles(final File[] directoryListing)
        throws InterruptedException
    {
        Map<String, SortedMap<BigDecimal, Pair<File, File>>> result = null;
        
        if (directoryListing != null)
        {
            result = new HashMap<>();
            
            for (final File file : directoryListing)
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
                
                categoriseFile(result, file);
            }
        }
        
        return(result);
    }
    

    /*
     * This method does the hard work of figuring out where the file belongs (which parent item, and where in that item's
     * version history).
     */
    private void categoriseFile(final Map<String, SortedMap<BigDecimal, Pair<File, File>>> categorisedFiles, final File file)
    {
        if (file != null)
        {
            if (file.canRead())
            {
                final String     fileName       = file.getName();
                final String     parentName     = getParentName(metadataLoader, fileName);
                final boolean    isMetadata     = isMetadataFile(metadataLoader, fileName);
                final BigDecimal versionNumber  = getVersionNumber(fileName);
                
                SortedMap<BigDecimal, Pair<File, File>> versions = categorisedFiles.get(parentName);
                
                // Find the item
                if (versions == null)
                {
                    versions = new TreeMap<>();
                    categorisedFiles.put(parentName, versions);
                }
                
                // Find the version within the item
                Pair<File, File> version = versions.get(versionNumber);
                
                if (version == null)
                {
                    version = new Pair<>(null, null);
                }
                
                // Categorise the incoming file in that version of the item
                if (isMetadata)
                {
                    version = new Pair<>(version.getFirst(), file);
                }
                else
                {
                    version = new Pair<>(file, version.getSecond());
                }
                
                versions.put(versionNumber, version);
                
                if (file.isDirectory())
                {
                    importStatus.incrementSourceCounter(COUNTER_NAME_DIRECTORIES_SCANNED);
                }
                else
                {
                    importStatus.incrementSourceCounter(COUNTER_NAME_FILES_SCANNED);
                }
            }
            else
            {
                if (warn(log)) warn(log, "Skipping '" + getFileName(file) + "' as Alfresco does not have permission to read it.");
                importStatus.incrementSourceCounter(COUNTER_NAME_UNREADABLE_ENTRIES);
            }
        }
    }
    
    
    private Pair<List<FilesystemBulkImportItem>, List<FilesystemBulkImportItem>> constructImportItems(final String                                             sourceRelativeParentDirectory,
                                                                                                      final Map<String, SortedMap<BigDecimal,Pair<File,File>>> categorisedFiles)
        throws InterruptedException
    {
        Pair<List<FilesystemBulkImportItem>, List<FilesystemBulkImportItem>> result = null;
        
        if (categorisedFiles != null)
        {
            final List<FilesystemBulkImportItem> directoryItems = new ArrayList<>();
            final List<FilesystemBulkImportItem> fileItems      = new ArrayList<>();
            
            result = new Pair<>(directoryItems, fileItems);
            
            for (final String parentName : categorisedFiles.keySet())
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
                
                final SortedMap<BigDecimal,Pair<File,File>>         itemVersions = categorisedFiles.get(parentName);
                final NavigableSet<FilesystemBulkImportItemVersion> versions     = constructImportItemVersions(itemVersions);
                final boolean                                       isDirectory  = versions.last().isDirectory();
                final FilesystemBulkImportItem                      item         = new FilesystemBulkImportItem(parentName,
                                                                                                                isDirectory,
                                                                                                                sourceRelativeParentDirectory,
                                                                                                                versions);
                
                if (isDirectory)
                {
                    directoryItems.add(item);
                }
                else
                {
                    fileItems.add(item);
                }
            }
        }
        
        return(result);
    }
    
    
    private final NavigableSet<FilesystemBulkImportItemVersion> constructImportItemVersions(final SortedMap<BigDecimal,Pair<File,File>> itemVersions)
        throws InterruptedException
    {
        // PRECONDITIONS
        if (itemVersions        == null) throw new IllegalArgumentException("itemVersions cannot be null.");
        if (itemVersions.size() <= 0)    throw new IllegalArgumentException("itemVersions cannot be empty.");
        
        // Body
        final NavigableSet<FilesystemBulkImportItemVersion> result = new TreeSet<>();
        
        for (final BigDecimal versionNumber : itemVersions.keySet())
        {
            if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");
            
            final Pair<File,File>   contentAndMetadataFiles = itemVersions.get(versionNumber);
            final FilesystemBulkImportItemVersion version   = new FilesystemBulkImportItemVersion(serviceRegistry,
                                                                                                  configuredContentStore,
                                                                                                  metadataLoader,
                                                                                                  versionNumber,
                                                                                                  contentAndMetadataFiles.getFirst(),
                                                                                                  contentAndMetadataFiles.getSecond());
            
            result.add(version);
        }
        
        return(result);
    }

}
