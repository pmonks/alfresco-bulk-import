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


/**
 * Definition of a filesystem source filter - a class that filters out import items identified from the source
 * directory from the import.
 * 
 * Note that source filters can be "chained", in which case each source filter effectively has
 * "veto" power - if any single filter requests that a given importable item be filtered, it
 * <strong>will</strong> be filtered.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public interface ImportFilter
{
    
    /**
     * Method that checks whether the given <code>FilesystemBulkImportItem</code> should be filtered.
     * 
     * @param item The import item to check for filtering <i>(will not be null)</i>.
     * @return True if the given import item should be filtered, false otherwise. 
     */
    boolean shouldFilter(FilesystemBulkImportItem item);
    
}
