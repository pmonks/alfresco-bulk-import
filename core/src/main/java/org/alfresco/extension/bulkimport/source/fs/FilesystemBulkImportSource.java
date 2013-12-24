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
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.repo.tenant.AbstractTenantRoutingContentStore;

import org.alfresco.extension.bulkimport.impl.BulkImportCallback;
import org.alfresco.extension.bulkimport.impl.BulkImportSourceStatus;
import org.alfresco.extension.bulkimport.source.BulkImportSource;
import org.alfresco.extension.bulkimport.source.fs.DirectoryAnalyser.AnalysedDirectory;


/**
 * This class is a Filesystem specific version of a <code>BulkImportSource</code>.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class FilesystemBulkImportSource
    implements BulkImportSource
{
    private final static String IMPORT_SOURCE_NAME          = "Filesystem";
    private final static String IMPORT_SOURCE_CONFIG_UI_URI = "/bulk/import/fs/config";
    
    private final static String PARAMETER_SOURCE_DIRECTORY = "sourceDirectory";
    
    private final DirectoryAnalyser directoryAnalyser;
    private final ContentStore      configuredContentStore;
    
    public FilesystemBulkImportSource(final DirectoryAnalyser directoryAnalyser,
                                      final ContentStore      configuredContentStore)
    {
        // PRECONDITIONS
        assert directoryAnalyser      != null : "directoryAnalyser must not be null.";
        assert configuredContentStore != null : "configuredContentStore must not be null.";
        
        // Body
        this.directoryAnalyser      = directoryAnalyser;
        this.configuredContentStore = configuredContentStore;
    }
    
    
    
    /*--------------------------------------------------------------------------*
     * Implemented methods
     *--------------------------------------------------------------------------*/

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#getName()
     */
    @Override
    public String getName()
    {
        return(IMPORT_SOURCE_NAME);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#getConfigWebScriptURI()
     */
    @Override
    public String getConfigWebScriptURI()
    {
        return(IMPORT_SOURCE_CONFIG_UI_URI);
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#inPlaceImportPossible(java.util.Map)
     */
    @Override
    public boolean inPlaceImportPossible(final Map<String, List<String>> parameters)
    {
        File sourceDirectory = null;
        
        try
        {
            sourceDirectory = getSourceDirectoryFromParameters(parameters);
        }
        catch (final FileNotFoundException fnfe)
        {
            // Checked exceptions == #fail
            throw new RuntimeException(fnfe);
        }
        
        return(isInContentStore(sourceDirectory));
    }


    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportSource#scan(java.util.Map, org.alfresco.extension.bulkimport.impl.BulkImportSourceStatus, org.alfresco.extension.bulkimport.impl.BulkImportCallback)
     */
    @Override
    public void scan(final Map<String, List<String>> parameters, final BulkImportSourceStatus status, final BulkImportCallback callback)
        throws InterruptedException
    {
        File sourceDirectory = null;
        
        try
        {
            sourceDirectory = getSourceDirectoryFromParameters(parameters);
        }
        catch (final FileNotFoundException fnfe)
        {
            // Checked exceptions == #fail
            throw new RuntimeException(fnfe);
        }
        
        directoryAnalyser.init();
        scanDirectory(status, callback, sourceDirectory, sourceDirectory);
    }
    
    
    /*--------------------------------------------------------------------------*
     * Private methods
     *--------------------------------------------------------------------------*/
    
    private void scanDirectory(final BulkImportSourceStatus status, final BulkImportCallback callback, final File sourceDirectory, final File directory)
        throws InterruptedException
    {
        AnalysedDirectory analysedDirectory = directoryAnalyser.analyseDirectory(sourceDirectory, directory);
        
        if (analysedDirectory != null)
        {
            // Enqueue directories first
            if (analysedDirectory.directoryItems != null)
            {
                for (final FilesystemBulkImportItem directoryItem : analysedDirectory.directoryItems)
                {
                    callback.enqueue(directoryItem);
                }
            }

            // Then files
            if (analysedDirectory.fileItems != null)
            {
                for (final FilesystemBulkImportItem fileItem : analysedDirectory.fileItems)
                {
                    callback.enqueue(fileItem);
                }
            }
            
            // Finally, recurse into subdirectories and scan them too
            if (analysedDirectory.directoryItems != null)
            {
                for (final FilesystemBulkImportItem directoryItem : analysedDirectory.directoryItems)
                {
                    scanDirectory(status, callback, sourceDirectory, ((FilesystemBulkImportItem.FilesystemVersion)(directoryItem.getVersions().first())).getContentFile());
                }
            }
        }
    }
    
    
    /**
     * Retrieve the source directory from the parameter map.
     * 
     * @param parameters The parameters <i>(may be null)
     * @return The source directory <i>(won't be null)</i>.
     * @throws IllegalArgumentException when the parameters are missing, null or blank.
     * @throws FileNotFoundException when the file doesn't exist.
     * @throws SecurityException when the file is not readable.
     */
    private File getSourceDirectoryFromParameters(final Map<String, List<String>> parameters)
        throws IllegalArgumentException,
               FileNotFoundException,
               SecurityException
    {
        File               result                         = null;
        final List<String> sourceDirectoryParameterValues = parameters.get(PARAMETER_SOURCE_DIRECTORY);
        String             sourceDirectoryName            = null;
        
        if (sourceDirectoryParameterValues == null ||
            sourceDirectoryParameterValues.size() != 1)
        {
            throw new IllegalArgumentException("Mandatory parameter '" + PARAMETER_SOURCE_DIRECTORY + "' was missing.");
        }
        
        sourceDirectoryName = sourceDirectoryParameterValues.get(0);
        
        if (sourceDirectoryName == null ||
            sourceDirectoryName.trim().length() == 0)
        {
            throw new IllegalArgumentException("Source directory was provided, but is empty.");
        }
        
        result = new File(sourceDirectoryName);
        
        if (!result.exists())
        {
            throw new FileNotFoundException("Source directory '" + sourceDirectoryName + "' doesn't exist.");
        }
        
        if (!result.canRead())
        {
            throw new SecurityException("No read access to source directory '" + sourceDirectoryName + "'.");
        }
        
        return(result);
    }

    /**
     * Determines whether the given file / directory is located in the given file content store.
     * @param fileContentStore The file content store to check <i>(must not be null)</i>.
     * @param source           The file to check <i>(must not be null)</i>.
     * @return True if the given file is in an Alfresco managed content store, false otherwise.
     */
    private final boolean isInContentStore(final FileContentStore fileContentStore, final File source)
    {
        boolean result            = false;
        String  storeRootLocation = fileContentStore.getRootLocation();
        String  sourcePath        = source.getAbsolutePath();   // Note: we don't use getCanonicalPath here because it dereferences symlinks (which we don't want)
        
        result = sourcePath.startsWith(storeRootLocation);
        
        return(result);
    }


    /**
     * Determines whether the given file is already located in an Alfresco managed content store.  Used to determine
     * whether to perform a streaming or in-place import.
     * 
     * @param source The file to test.  Typically this would be the source directory for the import <i>(must not be null)</i>.
     * @return True if the given file is in an Alfresco managed content store, false otherwise.
     */
    private final boolean isInContentStore(final File source)
    {
        boolean result = false;

        if (configuredContentStore instanceof FileContentStore)
        {
            result = isInContentStore((FileContentStore)configuredContentStore, source);
        }
        // It's a shame org.alfresco.repo.content.AbstractRoutingContentStore.getAllStores() is protected - that limits the applicability of this solution 
        else if (configuredContentStore instanceof AbstractTenantRoutingContentStore)
        {
            final List<ContentStore> backingStores = ((AbstractTenantRoutingContentStore)configuredContentStore).getAllStores();
            
            if (backingStores != null)
            {
                for (final ContentStore store : backingStores)
                {
                    if (store instanceof FileContentStore)
                    {
                        if (isInContentStore((FileContentStore)store, source))
                        {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }

        return(result);
    }

}
