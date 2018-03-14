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

/**
 * This exception represents the case where a batch has been received
 * out-of-order i.e. it contains a node whose parent hasn't been imported yet.
 * 
 * Custom <code>BulkImportSource</code> implementations may throw this
 * exception to indicate to the core bulk import logic that the batch should be
 * rolled back and requeued.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public class OutOfOrderBatchException
    extends RuntimeException
{
    private static final long serialVersionUID = 5644065156780862L;
    
    private final String missingParentPath;
    
    
    /**
     * Constructs an out-of-order batch exception.
     * 
     * @param missingParentPath The parent path (as a String) that's missing. Should be in the source-root-relative format "foo/bar".
     */
    public OutOfOrderBatchException(final String missingParentPath)
    {
        super();
        
        this.missingParentPath = missingParentPath;
    }
    
    public OutOfOrderBatchException(final String missingParentPath, final Throwable t)
    {
        super(t);
        
        this.missingParentPath = missingParentPath;
    }
    
    public String getMissingParentPath()
    {
        return(missingParentPath);
    }
    
}
