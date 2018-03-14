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

import java.util.NavigableSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.extension.bulkimport.source.AbstractBulkImportItem;


/**
 * This class represents a <code>BulkImportItem</code> sourced from the
 * server's local filesystem.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class FilesystemBulkImportItem
    extends AbstractBulkImportItem<FilesystemBulkImportItemVersion>
{
    final static Log log = LogFactory.getLog(FilesystemBulkImportItem.class);
    
    public FilesystemBulkImportItem(final String                          name,
                                    final boolean                         isDirectory,
                                    final String                          relativePathOfParent,
                                    final NavigableSet<FilesystemBulkImportItemVersion> versions)
    {
        super(name,
              isDirectory,
              relativePathOfParent,
              versions);
    }
    
}
