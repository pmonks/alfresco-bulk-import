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

import org.alfresco.extension.bulkimport.source.BulkImportItem;


/**
 * Definition of a source filter - a class that filters out importable items idenfitied from the source
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
     * Method that checks whether the given file or folder should be filtered.
     * 
     * @param importableItem The source importable item to check for filtering <i>(will not be null)</i>.
     * @return True if the given importable item should be filtered, false otherwise. 
     */
    boolean shouldFilter(final BulkImportItem importableItem);
    
}
