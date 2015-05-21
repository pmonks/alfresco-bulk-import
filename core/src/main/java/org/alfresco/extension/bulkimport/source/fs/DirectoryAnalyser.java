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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.extension.bulkimport.source.BulkImportSourceStatus;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;
import static org.alfresco.extension.bulkimport.source.fs.FilesystemUtils.*;


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
    
    // Regexes for matching version files
    public  final static String  VERSION_LABEL_REGEX      = "([\\d]+)(\\.([\\d]+))?"; // Group 0 = version label, Group 1 = major version #, group 3 (if not null) = minor version #
    private final static String  VERSION_SUFFIX_REGEX     = "\\.v(" + VERSION_LABEL_REGEX + ")\\z"; // Note: group numbers are one greater than shown above
    private final static String  VERSION_FILENAME_REGEX   = ".+" + VERSION_SUFFIX_REGEX;
    private final static Pattern VERSION_FILENAME_PATTERN = Pattern.compile(VERSION_FILENAME_REGEX);
    
    // Status counters    
    private final static String COUNTER_NAME_DIRECTORIES_SCANNED = "Number of directores scanned";
    private final static String COUNTER_NAME_FILES_SCANNED       = "Number of files scanned";
    private final static String COUNTER_NAME_FOLDERS_SCANNED     = "Number of folders scanned";
    private final static String COUNTER_NAME_UNREADABLE_ENTRIES  = "Number of unreadable entries";
        
    private final static String[] COUNTER_NAMES = { COUNTER_NAME_DIRECTORIES_SCANNED,
                                                    COUNTER_NAME_FILES_SCANNED,
                                                    COUNTER_NAME_FOLDERS_SCANNED,
                                                    COUNTER_NAME_UNREADABLE_ENTRIES
                                                  };

    private final ServiceRegistry        serviceRegistry;
    private final ContentStore           configuredContentStore;
    private final MetadataLoader         metadataLoader;
    private final BulkImportSourceStatus importStatus;
    
    
    
    public DirectoryAnalyser(final ServiceRegistry        serviceRegistry,
                             final ContentStore           configuredContentStore,
                             final MetadataLoader         metadataLoader,
                             final BulkImportSourceStatus importStatus)
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
        this.importStatus           = importStatus;
    }
    
    
    public void init()
    {
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
    public AnalysedDirectory analyseDirectory(final File sourceDirectory, final File directory)
        throws InterruptedException
    {
        AnalysedDirectory result                        = null;
        File[]            directoryListing              = null;
        long              analysisStart                 = 0L;
        long              analysisEnd                   = 0L;
        long              start                         = 0L;
        long              end                           = 0L;
        String            sourceRelativeParentDirectory = sourceDirectory.toPath().relativize(directory.toPath()).toString();  // Note: JDK 1.7 specific
        
        if (debug(log)) debug(log, "Analysing directory " + getFileName(directory) + "...");

        // List the directory
        start         = System.nanoTime();
        analysisStart = start;
        directoryListing = directory.listFiles();
        end = System.nanoTime();
        if (trace(log)) trace(log, "List directory (" + directoryListing.length + " entries) took: " + (float)(end - start) / (1000 * 1000 * 1000) + "s.");
        importStatus.incrementSourceCounter(COUNTER_NAME_DIRECTORIES_SCANNED);

        // Build up the list of items from the directory listing
        start = System.nanoTime();
        result = analyseDirectory(sourceRelativeParentDirectory, directoryListing);
        end = System.nanoTime();
        if (trace(log)) trace(log, "Convert directory listing to set of filesystem import items took: " + (float)(end - start) / (1000 * 1000 * 1000) + "s.");
        
        analysisEnd = end;
        if (debug(log)) debug(log, "Finished analysing directory " + getFileName(directory) + ", in " + (float)(analysisEnd - analysisStart) / (1000 * 1000 * 1000) + "s.");

        return(result);
    }
    
    
    private AnalysedDirectory analyseDirectory(final String sourceRelativeParentDirectory, final File[] directoryListing)
    {
        AnalysedDirectory result = null;
        
        if (directoryListing != null)
        {
            Map<String, List<ImportFile>> groupedFiles = groupFilesByParent(directoryListing);
            result                                     = constructImportItems(sourceRelativeParentDirectory, groupedFiles);
        }
        
        return(result);
    }
    
    
    private Map<String, List<ImportFile>> groupFilesByParent(final File[] directoryListing)
    {
        Map<String, List<ImportFile>> result = null;
        
        if (directoryListing != null)
        {
            result = new HashMap<String, List<ImportFile>>();
            
            for (final File file : directoryListing)
            {
                findOrCreateGroupedFile(result, file);
            }
        }
        
        return(result);
    }
    
    
    private void findOrCreateGroupedFile(Map<String, List<ImportFile>> groupedFiles, final File file)
    {
        if (file != null)
        {
            if (file.canRead())
            {
                final String     fileName     = file.getName();
                final boolean    isVersion    = isVersionFile(fileName);
                final boolean    isMetadata   = isMetadataFile(fileName);
                final String     parentName   = getParentName(fileName, isVersion, isMetadata);
                final String     versionLabel = isVersion ? getVersionLabel(fileName) : null;
                final ImportFile importFile   = new ImportFile(file, isVersion, isMetadata, versionLabel);
                
                if (trace(log)) trace(log, getFileName(file) + ": " + (isVersion ? "[version] " : "") + (isMetadata ? "[metadata]" : ""));
                
                if (groupedFiles.containsKey(parentName))
                {
                    groupedFiles.get(parentName).add(importFile);
                }
                else
                {
                    final List<ImportFile> entry = new ArrayList<ImportFile>(1);
                    entry.add(importFile);
                    groupedFiles.put(parentName, entry);
                }
            }
            else
            {
                if (warn(log)) warn(log, "Skipping '" + getFileName(file) + "' as Alfresco does not have permission to read it.");
            }
        }
    }
    

    private String getParentName(final String fileName, final boolean isVersion, final boolean isMetadata)
    {
        String result = fileName;
        
        if (isVersion)
        {
            result = result.replaceFirst(VERSION_SUFFIX_REGEX, "");
        }
        
        if (isMetadata)
        {
            result = result.substring(0, result.length() - (MetadataLoader.METADATA_SUFFIX + metadataLoader.getMetadataFileExtension()).length());
        }
        
        return(result);
    }
    
    
    boolean isVersionFile(final String fileName)
    {
        Matcher matcher = VERSION_FILENAME_PATTERN.matcher(fileName);

        return(matcher.matches());
    }
    

    boolean isMetadataFile(final String fileName)
    {
        boolean result = false;
        
        if (metadataLoader != null)
        {
            result = fileName.endsWith(MetadataLoader.METADATA_SUFFIX + metadataLoader.getMetadataFileExtension());
        }
        
        return(result);
    }
    
    
    private String getVersionLabel(final String fileName)
    {
        String result = null;
        
        if (fileName != null)
        {
            Matcher m = VERSION_FILENAME_PATTERN.matcher(fileName);
            
            if (m.matches())
            {
                result = m.group(1);  // Group 1 = version label, including full stop separator for decimal version numbers
            }
            else
            {
                throw new IllegalStateException("File " + fileName + " is not a version file.");
            }
        }
        
        return(result);
    }
    
    
    private AnalysedDirectory constructImportItems(final String sourceRelativeParentDirectory, final Map<String, List<ImportFile>> groupedFiles)
    {
        AnalysedDirectory result = null;
        
        if (groupedFiles != null)
        {
            result                = new AnalysedDirectory();
            result.directoryItems = new ArrayList<FilesystemBulkImportItem>();
            result.fileItems      = new ArrayList<FilesystemBulkImportItem>();
            
            for (final String fileName : groupedFiles.keySet())
            {
                FilesystemBulkImportItem item = new FilesystemBulkImportItem(serviceRegistry, configuredContentStore, metadataLoader, sourceRelativeParentDirectory, fileName, groupedFiles.get(fileName));
                
                if (item.isDirectory())
                {
                    result.directoryItems.add(item);
                }
                else
                {
                    result.fileItems.add(item);
                }
            }
        }
        
        return(result);
    }

    
    /**
     * This class represents an analysed directory.
     */
    public class AnalysedDirectory
    {
        public List<FilesystemBulkImportItem> directoryItems = null;
        public List<FilesystemBulkImportItem> fileItems      = null;
    }
        
}
