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


package org.alfresco.extension.bulkimport;

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

    public OutOfOrderBatchException()
    {
        super();
    }

    public OutOfOrderBatchException(String message) {
        super(message);
    }

    public OutOfOrderBatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutOfOrderBatchException(Throwable cause) {
        super(cause);
    }
}
