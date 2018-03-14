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


package org.alfresco.extension.bulkimport.source.sample;

import java.math.BigDecimal;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.extension.bulkimport.source.AbstractBulkImportItemVersion;


/**
 * This class is a simple sample source version.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class SampleSourceImportItemVersion
    extends AbstractBulkImportItemVersion<String, Object>
{
    public SampleSourceImportItemVersion(final String     name,
                                         final boolean    isDirectory,
                                         final BigDecimal versionNumber)
    {
        super(isDirectory ? ContentModel.TYPE_FOLDER.toString() : ContentModel.TYPE_CONTENT.toString(), versionNumber);

        this.contentReference  = isDirectory ? null : "This is the content of version " + String.valueOf(versionNumber) + " of " + name + ".";
        this.metadataReference = null;  // Sample source doesn't support metadata
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#getContentSource()
     */
    @Override
    public String getContentSource()
    {
        return("Synthetic Content");
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#sizeInBytes()
     */
    @Override
    public long sizeInBytes()
    {
        return(contentReference == null ? 0 : contentReference.length());
    }

    /**
     * @see org.alfresco.extension.bulkimport.source.BulkImportItemVersion#putContent(org.alfresco.service.cmr.repository.ContentWriter)
     */
    @Override
    public void putContent(final ContentWriter writer)
    {
        // Provide some hardcoded content
        writer.setMimetype("text/plain");
        writer.setEncoding("UTF8");
        writer.putContent(contentReference);
    }

}
