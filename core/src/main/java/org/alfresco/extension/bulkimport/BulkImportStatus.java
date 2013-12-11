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

package org.alfresco.extension.bulkimport;

import java.util.Date;


/**
 * Interface defining which information can be obtained from the Bulk Filesystem Import engine.
 *
 * @author Peter Monks (peter.monks@alfresco.com)
 */
public interface BulkImportStatus
{
    // General information
    boolean         inProgress();
    boolean         isStopping();
    ProcessingState getProcessingState();
    
    String     getSourceDirectory();
    String     getTargetSpace();
    ImportType getImportType();
    
    Date getStartDate();
    Date getEndDate();
    
    long getBatchWeight();
    int  getNumberOfActiveThreads();
    int  getTotalNumberOfThreads();
    
    String getCurrentFileBeingProcessed();
    long   getNumberOfBatchesCompleted();

    Long      getDurationInNs();  // Note: java.lang.Long, _not_ primitive long - may be null
    Throwable getLastException();
    String    getLastExceptionAsString();
    

    // Read-side information
    long getNumberOfFoldersScanned();
    long getNumberOfFilesScanned();
    long getNumberOfUnreadableEntries();

    long getNumberOfContentFilesRead();
    long getNumberOfContentBytesRead();
    
    long getNumberOfMetadataFilesRead();
    long getNumberOfMetadataBytesRead();
    
    long getNumberOfContentVersionFilesRead();
    long getNumberOfContentVersionBytesRead();
    
    long getNumberOfMetadataVersionFilesRead();
    long getNumberOfMetadataVersionBytesRead();
    
    // Write-side information
    long getNumberOfSpaceNodesCreated();
    long getNumberOfSpaceNodesReplaced();
    long getNumberOfSpaceNodesSkipped();
    long getNumberOfSpacePropertiesWritten();
    
    long getNumberOfContentNodesCreated();
    long getNumberOfContentNodesReplaced();
    long getNumberOfContentNodesSkipped();
    long getNumberOfContentBytesWritten();
    long getNumberOfContentPropertiesWritten();
    
    long getNumberOfContentVersionsCreated();
    long getNumberOfContentVersionBytesWritten();
    long getNumberOfContentVersionPropertiesWritten();

    public enum ImportType
    {
        STREAMING("Streaming"),
        IN_PLACE("In Place");
        
        // The following allows us to create human-readable names for this enum.
        // Note that it breaks round-tripping (enum -> String -> enum).
        private final String name;
        
        private ImportType(final String name)
        {
            this.name = name;
            
        }
        
        @Override
        public String toString()
        {
            return(name);
        }
    };
    
    public enum ProcessingState
    {
        NEVER_RUN("Never run"),
        RUNNING("Running"),
        SUCCESSFUL("Successful"),
        STOPPING("Stopping"),
        STOPPED("Stopped"),
        FAILED("Failed");
        
        // The following allows us to create human-readable names for this enum.
        // Note that it breaks round-tripping (enum -> String -> enum).
        private final String name;
        
        private ProcessingState(final String name)
        {
            this.name = name;
            
        }
        
        @Override
        public String toString()
        {
            return(name);
        }
    };
    
}
