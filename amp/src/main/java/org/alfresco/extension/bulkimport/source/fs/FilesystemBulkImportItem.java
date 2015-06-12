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
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.util.Pair;

import org.alfresco.extension.bulkimport.source.AbstractBulkImportItem;


/**
 * This class represents a <code>BulkImportItem</code> sourced from the
 * server's local filesystem.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class FilesystemBulkImportItem
    extends AbstractBulkImportItem<FilesystemVersion>
{
    final static Log log = LogFactory.getLog(FilesystemBulkImportItem.class);
    
    public FilesystemBulkImportItem(final MimetypeService                   mimeTypeService,
                                    final ContentStore                      configuredContentStore,
                                    final MetadataLoader                    metadataLoader,
                                    final String                            relativePathOfParent,
                                    final SortedMap<String,Pair<File,File>> itemVersions)
    {
        super(relativePathOfParent,
              buildVersions(mimeTypeService, configuredContentStore, metadataLoader, itemVersions));
    }
    
    
    private final static NavigableSet<FilesystemVersion> buildVersions(final MimetypeService                   mimeTypeService,
                                                                       final ContentStore                      configuredContentStore,
                                                                       final MetadataLoader                    metadataLoader,
                                                                       final SortedMap<String,Pair<File,File>> itemVersions)
    {
        // PRECONDITIONS
        if (mimeTypeService        == null) throw new IllegalArgumentException("mimeTypeService cannot be null.");
        if (configuredContentStore == null) throw new IllegalArgumentException("configuredContentStore cannot be null.");
        if (metadataLoader         == null) throw new IllegalArgumentException("metadataLoader cannot be null.");
        if (itemVersions           == null) throw new IllegalArgumentException("itemVersions cannot be null.");
        if (itemVersions.size()    <= 0)    throw new IllegalArgumentException("itemVersions cannot be empty.");
        
        // Body
        final NavigableSet<FilesystemVersion> result = new TreeSet<FilesystemVersion>();
        
        for (final String versionLabel : itemVersions.keySet())
        {
            final Pair<File,File>   contentAndMetadataFiles = itemVersions.get(versionLabel);
            final FilesystemVersion version                 = new FilesystemVersion(mimeTypeService, 
                                                                                    configuredContentStore,
                                                                                    metadataLoader,
                                                                                    versionLabel,
                                                                                    contentAndMetadataFiles.getFirst(),
                                                                                    contentAndMetadataFiles.getSecond());
            
            result.add(version);
        }
        
        return(result);
    }
    
}
