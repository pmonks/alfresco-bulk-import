[#ftl]
<?xml version='1.0' encoding='UTF-8'?>
<BulkFilesystemImportStatus>
  <InProgress>[#compress]
[#if importStatus.inProgress()]
    true
[#else]
    false
[/#if]
[/#compress]</InProgress>
  <Status>${importStatus.processingState}</Status>
[#if importStatus.sourceDirectory??]
  <SourceDirectory>${importStatus.sourceDirectory}</SourceDirectory>
[/#if]
[#if importStatus.targetSpace??]
  <TargetSpace>${importStatus.targetSpace}</TargetSpace>
[/#if]
[#if importStatus.importType??]
  <ImportType>${importStatus.importType}</ImportType>
[/#if]
  <BatchWeight>${importStatus.batchWeight?c}</BatchWeight>
[#if importStatus.inProgress()]
  <TotalThreads>${importStatus.totalNumberOfThreads?c}</TotalThreads>
  <ActiveThreads>${importStatus.numberOfActiveThreads?c}</ActiveThreads>
[/#if]
[#if importStatus.startDate??]
  <StartDate>${importStatus.startDate?datetime?iso_utc}</StartDate>
[/#if]
[#if importStatus.endDate??]
  <EndDate>${importStatus.endDate?datetime?iso_utc}</EndDate>
[/#if]
[#if importStatus.durationInNs??]
  <DurationInNS>${importStatus.durationInNs?c}</DurationInNS>
[/#if]
  <CompletedBatches>${importStatus.numberOfBatchesCompleted?c}</CompletedBatches>
  <CurrentFileOrFolder>${importStatus.currentFileBeingProcessed!"n/a"}</CurrentFileOrFolder>
  <SourceStatistics>
    <LastFileOrFolderProcessed>${importStatus.currentFileBeingProcessed!"n/a"}</LastFileOrFolderProcessed>
    <FilesScanned>${importStatus.numberOfFilesScanned?c}</FilesScanned>
    <FoldersScanned>${importStatus.numberOfFoldersScanned?c}</FoldersScanned>
    <UnreadableEntries>${importStatus.numberOfUnreadableEntries?c}</UnreadableEntries>
    <ContentFilesRead>${importStatus.numberOfContentFilesRead?c}</ContentFilesRead>
    <ContentBytesRead>${importStatus.numberOfContentBytesRead?c}</ContentBytesRead>
    <MetadataFilesRead>${importStatus.numberOfMetadataFilesRead?c}</MetadataFilesRead>
    <MetadataBytesRead>${importStatus.numberOfMetadataBytesRead?c}</MetadataBytesRead>
    <ContentVersionFilesRead>${importStatus.numberOfContentVersionFilesRead?c}</ContentVersionFilesRead>
    <ContentVersionBytesRead>${importStatus.numberOfContentVersionBytesRead?c}</ContentVersionBytesRead>
    <MetadataVersionFilesRead>${importStatus.numberOfMetadataVersionFilesRead?c}</MetadataVersionFilesRead>
    <MetadataVersionBytesRead>${importStatus.numberOfMetadataVersionBytesRead?c}</MetadataVersionBytesRead>
  </SourceStatistics>
  <TargetStatistics>
    <SpaceNodesCreated>${importStatus.numberOfSpaceNodesCreated?c}</SpaceNodesCreated>
    <SpaceNodesReplaced>${importStatus.numberOfSpaceNodesReplaced?c}</SpaceNodesReplaced>
    <SpaceNodesSkipped>${importStatus.numberOfSpaceNodesSkipped?c}</SpaceNodesSkipped>
    <SpacePropertiesWritten>${importStatus.numberOfSpacePropertiesWritten?c}</SpacePropertiesWritten>
    <ContentNodesCreated>${importStatus.numberOfContentNodesCreated?c}</ContentNodesCreated>
    <ContentNodesReplaced>${importStatus.numberOfContentNodesReplaced?c}</ContentNodesReplaced>
    <ContentNodesSkipped>${importStatus.numberOfContentNodesSkipped?c}</ContentNodesSkipped>
    <ContentBytesWritten>${importStatus.numberOfContentBytesWritten?c}</ContentBytesWritten>
    <ContentPropertiesWritten>${importStatus.numberOfContentPropertiesWritten?c}</ContentPropertiesWritten>
    <ContentVersionsCreated>${importStatus.numberOfContentVersionsCreated?c}</ContentVersionsCreated>
    <ContentVersionsBytesWritten>${importStatus.numberOfContentVersionBytesWritten?c}</ContentVersionsBytesWritten>
    <ContentVersionsPropertiesWritten>${importStatus.numberOfContentVersionPropertiesWritten?c}</ContentVersionsPropertiesWritten>
  </TargetStatistics>
[#if importStatus.lastExceptionAsString??]
  <ErrorInformation>
    <FileThatFailed>${importStatus.currentFileBeingProcessed!"n/a"}</FileThatFailed>
    <Exception>${importStatus.lastExceptionAsString}</Exception>
  </ErrorInformation>
[/#if]
</BulkFilesystemImportStatus>
