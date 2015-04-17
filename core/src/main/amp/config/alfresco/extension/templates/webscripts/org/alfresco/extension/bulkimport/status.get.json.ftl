[#ftl]
{
  "inProgress" : [#compress]
[#if importStatus.inProgress()]
    true
[#else]
    false
[/#if]
[/#compress],
  "status" : "${importStatus.processingState}",
[#if importStatus.sourceDirectory??]
  "sourceDirectory" : "${importStatus.sourceDirectory?js_string?replace("\\'", "'")}",
[/#if]
[#if importStatus.targetSpace??]
  "targetSpace" : "${importStatus.targetSpace?js_string?replace("\\'", "'")}",
[/#if]
[#if importStatus.importType??]
  "importType" : "${importStatus.importType}",
[/#if]
  "batchWeight" : ${importStatus.batchWeight?c},
[#if importStatus.inProgress()]
  "totalThreads" : ${importStatus.totalNumberOfThreads?c},
  "activeThreads" : ${importStatus.numberOfActiveThreads?c},
[/#if]
[#if importStatus.startDate??]
  "startDate" : "${importStatus.startDate?datetime?iso_utc}",
[/#if]
[#if importStatus.endDate??]
  "endDate" : "${importStatus.endDate?datetime?iso_utc}",
[/#if]
[#if importStatus.durationInNs??]
  "durationInNS" : ${importStatus.durationInNs?c},
[/#if]
  "completedBatches" : ${importStatus.numberOfBatchesCompleted!0?c},
  "currentFileOrFolder" : "${importStatus.currentFileBeingProcessed!"n/a"}",
  "sourceStatistics" : {
    "lastFileOrFolderProcessed" : "${importStatus.currentFileBeingProcessed!"n/a"?js_string?replace("\\'", "'")}",
    "filesScanned" : ${importStatus.numberOfFilesScanned!0?c},
    "foldersScanned" : ${importStatus.numberOfFoldersScanned!0?c},
    "unreadableEntries" : ${importStatus.numberOfUnreadableEntries!0?c},
    "contentFilesRead" : ${importStatus.numberOfContentFilesRead!0?c},
    "contentBytesRead" : ${importStatus.numberOfContentBytesRead!0?c},
    "metadataFilesRead" : ${importStatus.numberOfMetadataFilesRead!0?c},
    "metadataBytesRead" : ${importStatus.numberOfMetadataBytesRead!0?c},
    "contentVersionFilesRead" : ${importStatus.numberOfContentVersionFilesRead!0?c},
    "contentVersionBytesRead" : ${importStatus.numberOfContentVersionBytesRead!0?c},
    "metadataVersionFilesRead" : ${importStatus.numberOfMetadataVersionFilesRead!0?c},
    "metadataVersionBytesRead" : ${importStatus.numberOfMetadataVersionBytesRead!0?c}
  },
  "targetStatistics" : {
    "spaceNodesCreated" : ${importStatus.numberOfSpaceNodesCreated!0?c},
    "spaceNodesReplaced" : ${importStatus.numberOfSpaceNodesReplaced!0?c},
    "spaceNodesSkipped" : ${importStatus.numberOfSpaceNodesSkipped!0?c},
    "spacePropertiesWritten" : ${importStatus.numberOfSpacePropertiesWritten!0?c},
    "contentNodesCreated" : ${importStatus.numberOfContentNodesCreated!0?c},
    "contentNodesReplaced" : ${importStatus.numberOfContentNodesReplaced!0?c},
    "contentNodesSkipped" : ${importStatus.numberOfContentNodesSkipped!0?c},
    "contentBytesWritten" : ${importStatus.numberOfContentBytesWritten!0?c},
    "contentPropertiesWritten" : ${importStatus.numberOfContentPropertiesWritten!0?c},
    "contentVersionsCreated" : ${importStatus.numberOfContentVersionsCreated!0?c},
    "contentVersionsBytesWritten" : ${importStatus.numberOfContentVersionBytesWritten!0?c},
    "contentVersionsPropertiesWritten" : ${importStatus.numberOfContentVersionPropertiesWritten!0?c}
  }
[#if importStatus.lastExceptionAsString??]
  ,
  "errorInformation" : {
    "fileThatFailed" : "${importStatus.currentFileBeingProcessed!"n/a"}",
    "exception" : "${importStatus.lastExceptionAsString?js_string?replace("\\'", "'")}"
  }
[/#if]
}