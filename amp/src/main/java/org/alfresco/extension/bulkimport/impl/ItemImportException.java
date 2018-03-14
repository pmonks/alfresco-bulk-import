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


package org.alfresco.extension.bulkimport.impl;

import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportItemVersion;

/**
 * This simple exception adds the failed item to the stack.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public class ItemImportException
    extends RuntimeException
{
    private static final long serialVersionUID = -8885681046650475606L;
    
    final BulkImportItem<BulkImportItemVersion> failedItem;

    
    public ItemImportException(final BulkImportItem<BulkImportItemVersion> failedItem, final Throwable rootCause)
    {
        super("Unexpected exception:\n " + (rootCause == null ? "<unknown>" : String.valueOf(rootCause.getClass()) + ": " + rootCause.getMessage()) +
              "\nWhile importing item: " + String.valueOf(failedItem), rootCause);
        
        this.failedItem = failedItem;
    }
    

    /**
     * @return The <code>BulkImportItem</code> that failed <i>(may be null)</i>.
     */
    public BulkImportItem<BulkImportItemVersion> getFailedItem()
    {
        return(failedItem);
    }

}
