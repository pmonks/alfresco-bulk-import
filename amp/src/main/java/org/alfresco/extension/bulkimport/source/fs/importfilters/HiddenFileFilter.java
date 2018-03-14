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

package org.alfresco.extension.bulkimport.source.fs.importfilters;

import java.util.Iterator;

import org.alfresco.extension.bulkimport.source.fs.FilesystemBulkImportItem;
import org.alfresco.extension.bulkimport.source.fs.FilesystemBulkImportItemVersion;
import org.alfresco.extension.bulkimport.source.fs.ImportFilter;


/**
 * This class is an <code>ImportFilter</code> that filters out hidden files.
 * 
 * The exact definition of "hidden" is OS dependent - see http://download.oracle.com/javase/6/docs/api/java/io/File.html#isHidden() for details.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public class HiddenFileFilter
    implements ImportFilter
{
    /**
     * @see org.alfresco.extension.bulkimport.source.fs.ImportFilter#shouldFilter(org.alfresco.extension.bulkimport.source.fs.FilesystemBulkImportItem)
     */
    @Override
    public boolean shouldFilter(final FilesystemBulkImportItem item)
    {
        boolean result = false;
        
        if (item.numberOfVersions() > 0)
        {
            Iterator<FilesystemBulkImportItemVersion> iter = item.getVersions().descendingIterator();
            
            while (iter.hasNext())
            {
                FilesystemBulkImportItemVersion version = iter.next();

                if (version.hasContent() && version.getContentFile().isHidden())
                {
                    result = true;
                    break;
                }
            }
        }

        return(result);
    }

}
