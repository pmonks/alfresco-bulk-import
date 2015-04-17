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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.extension.bulkimport.source.BulkImportSourceStatus;


/**
 * This interface defines a directory analyser. This is the process by which
 * the contents of a source directory are grouped together into a list of
 * <code>ImportableItem</code>s. 
 * 
 * Please note that this interface is not intended to have more than one implementation
 * (<code>DirectoryAnalyserImpl</code>) - it exists solely for dependency injection purposes.
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
    private final MetadataLoader         metadataLoader;
    private final BulkImportSourceStatus importStatus;
    
    
    
    public DirectoryAnalyser(final ServiceRegistry        serviceRegistry,
                             final MetadataLoader         metadataLoader,
                             final BulkImportSourceStatus importStatus)
    {
        // PRECONDITIONS
        assert serviceRegistry != null : "serviceRegistry must not be null.";
        assert metadataLoader  != null : "metadataLoader must not be null.";
        assert importStatus    != null : "importStatus must not be null.";
        
        // Body
        this.serviceRegistry = serviceRegistry;
        this.metadataLoader  = metadataLoader;
        this.importStatus    = importStatus;
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
        long              start                         = 0L;
        long              end                           = 0L;
        String            sourceRelativeParentDirectory = sourceDirectory.toPath().relativize(directory.toPath()).toString();  // Note: JDK 1.7 specific
        
        if (log.isDebugEnabled()) log.debug("Analysing directory " + getFileName(directory) + "...");

        // List the directory
        start = System.nanoTime();
        directoryListing = directory.listFiles();
        end = System.nanoTime();
        if (log.isTraceEnabled()) log.trace("List directory took: " + (float)(end - start) / (1000 * 1000 * 1000) + "s");
        importStatus.incrementSourceCounter(COUNTER_NAME_DIRECTORIES_SCANNED);

        // Build up the list of items from the directory listing
        start = System.nanoTime();
        result = analyseDirectory(sourceRelativeParentDirectory, directoryListing);
        end = System.nanoTime();
        if (log.isTraceEnabled()) log.trace("Convert directory listing to set of filesystem import items took: " + (float)(end - start) / (1000 * 1000 * 1000) + "s");
        
        if (log.isDebugEnabled()) log.debug("Finished analysing directory " + getFileName(directory) + ".");

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
            final String     fileName     = file.getName();
            final boolean    isVersion    = isVersionFile(fileName);
            final boolean    isMetadata   = isMetadataFile(fileName);
            final String     parentName   = getParentName(fileName, isVersion, isMetadata);
            final String     versionLabel = isVersion ? getVersionLabel(fileName) : null;
            final ImportFile importFile   = new ImportFile(file, isVersion, isMetadata, versionLabel);
            
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
            result = m.group(1);  // Group 1 = version label including full stop separator
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
                FilesystemBulkImportItem item = new FilesystemBulkImportItem(serviceRegistry, metadataLoader, sourceRelativeParentDirectory, fileName, groupedFiles.get(fileName));
                
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



    
            
/*
            result.originalListing = Arrays.asList(directoryListing); // Note: Arrays.asList throws an NPE if the input is null
            result.directoryItems  = new ArrayList<FilesystemBulkImportItem>();
            result.fileItems       = new ArrayList<FilesystemBulkImportItem>();
            
            
            
            
            
            for (final File file : directoryListing)
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted.  Terminating early.");
                
                if (file.canRead())
                {
                    if (isVersionFile(file))
                    {
                        addVersionFile(temp, file);
                        importStatus.incrementSourceCounter(COUNTER_NAME_FILES_SCANNED);
                    }
                    else if (isMetadataFile(file))
                    {
                        addMetadataFile(temp, file);
                        importStatus.incrementSourceCounter(COUNTER_NAME_FILES_SCANNED);
                    }
                    else
                    {
                        boolean isDirectory = addParentFile(temp, file);
                        
                        if (isDirectory)
                        {
                            importStatus.incrementSourceCounter(COUNTER_NAME_FOLDERS_SCANNED);
                        }
                        else
                        {
                            importStatus.incrementSourceCounter(COUNTER_NAME_FILES_SCANNED);
                        }
                    }
                }
                else
                {
                    if (log.isWarnEnabled()) log.warn("Skipping unreadable file/directory '" + getFileName(file) + "'.");
                    importStatus.incrementSourceCounter(COUNTER_NAME_UNREADABLE_ENTRIES);
                }
            }
            
            for (final FilesystemBulkImportItem item : temp.values())
            {
                if (item != null)
                {
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
        }

        // Finally, remove any items from the list that aren't valid
        start = System.nanoTime();
        // Filtering logic would go here...
        end = System.nanoTime();
        if (log.isTraceEnabled()) log.trace("Filter invalid importable items took: " + (float)(end - start) / (1000 * 1000 * 1000 )+ "s");
        return(result);
    }


    private void addVersionFile(final Map<String, BulkImportItem> temp, final File versionFile)
    {
        String  parentContentFile = getParentOfVersionFile(versionFile);
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

        BulkImportItem         importableItem = findOrCreateImportableItem(temp, parentContentFile);
        String                 versionLabel   = getVersionLabel(versionFile);
        BulkImportItem.Version versionEntry   = findOrCreateVersionEntry(importableItem, versionLabel);

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


    private BulkImportItem.Version findOrCreateVersionEntry(final BulkImportItem importableItem, final String versionLabel)
    {
        BulkImportItem.Version result = importableItem.getVersionEntry(versionLabel);

        if (result == null)
        {
            result = importableItem.new Version(versionLabel);
            
            importableItem.addVersionEntry(result);
        }

        return (result);
    }


    private String getVersionLabel(final File versionFile)
    {
        String result = null;

        if (!isVersionFile(versionFile))
        {
            throw new IllegalStateException(getFileName(versionFile) + " is not a version file.");
        }

        Matcher matcher = VERSION_SUFFIX_PATTERN.matcher(versionFile.getName());

        if (matcher.matches())
        {
            result = matcher.group(1);
        }
        else
        {
            throw new IllegalStateException(getFileName(versionFile) + " has a malformed version label."); 
        }

        return(result);
    }
    
    
    private String getParentOfVersionFile(final String versionFileNam)
    {
        String result = null;

        if (!isVersionFile(versionFile))
        {
            throw new IllegalStateException(getFileName(versionFile) + " is not a version file.");
        }

        result = 

        return(result);
    }

    private String getParentOfMetadatafile(final File metadataFile)
    {
        String result = null;

        if (!isMetadataFile(metadataFile))
        {
            throw new IllegalStateException(getFileName(metadataFile) + " is not a metadata file.");
        }

        String name = metadataFile.getName();
        result = name.substring(0, name.length() - (MetadataLoader.METADATA_SUFFIX + metadataLoader.getMetadataFileExtension()).length());

        return(result);
    }
*/
    
    
    /**
     * 
     */
    public static String getFileName(final File file)
    {
        String result = null;
        
        if (file != null)
        {
            try
            {
                result = file.getCanonicalPath();
            }
            catch (final IOException ioe)
            {
                result = file.getAbsolutePath();
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
