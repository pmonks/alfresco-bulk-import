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
  "completedBatches" : ${importStatus.numberOfBatchesCompleted?c},
  "currentFileOrFolder" : "${importStatus.currentFileBeingProcessed!"n/a"}",
  "sourceStatistics" : {
    "lastFileOrFolderProcessed" : "${importStatus.currentFileBeingProcessed!"n/a"?js_string?replace("\\'", "'")}",
    "filesScanned" : ${importStatus.numberOfFilesScanned?c},
    "foldersScanned" : ${importStatus.numberOfFoldersScanned?c},
    "unreadableEntries" : ${importStatus.numberOfUnreadableEntries?c},
    "contentFilesRead" : ${importStatus.numberOfContentFilesRead?c},
    "contentBytesRead" : ${importStatus.numberOfContentBytesRead?c},
    "metadataFilesRead" : ${importStatus.numberOfMetadataFilesRead?c},
    "metadataBytesRead" : ${importStatus.numberOfMetadataBytesRead?c},
    "contentVersionFilesRead" : ${importStatus.numberOfContentVersionFilesRead?c},
    "contentVersionBytesRead" : ${importStatus.numberOfContentVersionBytesRead?c},
    "metadataVersionFilesRead" : ${importStatus.numberOfMetadataVersionFilesRead?c},
    "metadataVersionBytesRead" : ${importStatus.numberOfMetadataVersionBytesRead?c}
  },
  "targetStatistics" : {
    "spaceNodesCreated" : ${importStatus.numberOfSpaceNodesCreated?c},
    "spaceNodesReplaced" : ${importStatus.numberOfSpaceNodesReplaced?c},
    "spaceNodesSkipped" : ${importStatus.numberOfSpaceNodesSkipped?c},
    "spacePropertiesWritten" : ${importStatus.numberOfSpacePropertiesWritten?c},
    "contentNodesCreated" : ${importStatus.numberOfContentNodesCreated?c},
    "contentNodesReplaced" : ${importStatus.numberOfContentNodesReplaced?c},
    "contentNodesSkipped" : ${importStatus.numberOfContentNodesSkipped?c},
    "contentBytesWritten" : ${importStatus.numberOfContentBytesWritten?c},
    "contentPropertiesWritten" : ${importStatus.numberOfContentPropertiesWritten?c},
    "contentVersionsCreated" : ${importStatus.numberOfContentVersionsCreated?c},
    "contentVersionsBytesWritten" : ${importStatus.numberOfContentVersionBytesWritten?c},
    "contentVersionsPropertiesWritten" : ${importStatus.numberOfContentVersionPropertiesWritten?c}
  }
[#if importStatus.lastExceptionAsString??]
  ,
  "errorInformation" : {
    "fileThatFailed" : "${importStatus.currentFileBeingProcessed!"n/a"}",
    "exception" : "${importStatus.lastExceptionAsString?js_string?replace("\\'", "'")}"
  }
[/#if]
}