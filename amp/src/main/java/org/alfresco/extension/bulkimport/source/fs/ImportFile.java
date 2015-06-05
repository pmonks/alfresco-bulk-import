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

/**
 * This class encapsulates the data needed to load a single node from the
 * filesystem <code>BulkImportSource</code>.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public final class ImportFile
{
    private final File    file;
    private final boolean isVersion;
    private final boolean isMetadata;
    private final String  versionLabel;
    
    public ImportFile(final File    file,
                      final boolean isVersion,
                      final boolean isMetadata,
                      final String  versionLabel)
    {
        // PRECONDITIONS
        assert file != null : "file must not be null.";
        
        // Body
        this.file         = file;
        this.isVersion    = isVersion;
        this.isMetadata   = isMetadata;
        this.versionLabel = versionLabel;
    }
    
    public File    getFile()         { return(file); }
    public boolean isVersion()       { return(isVersion); }
    public boolean isMetadata()      { return(isMetadata); }
    public String  getVersionLabel() { return(versionLabel); }
}
