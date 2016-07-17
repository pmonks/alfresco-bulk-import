[#ftl]
{
  "processingState"                : "${importStatus.processingState?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  "inProgress"                     : ${importStatus.inProgress()?c},
  "neverRun"                       : ${importStatus.neverRun()?c}
[#if !importStatus.neverRun()]
  ,
  [#if importStatus.initiatingUserId??]
  "initiatingUserId"               : "${importStatus.initiatingUserId?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  [/#if]
  [#if importStatus.sourceName??]
  "sourceName"                     : "${importStatus.sourceName?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  [/#if]
  "sourceParameters" : {
  [#if importStatus.sourceParameters??]
    [#list importStatus.sourceParameters?keys as parameterName]
      [#assign parameterValue = importStatus.sourceParameters[parameterName]]
    "${parameterName?js_string?replace("\\'", "'")?replace("\\>", ">")}" : "${parameterValue?js_string?replace("\\'", "'")?replace("\\>", ">")}"[#if parameterName != importStatus.sourceParameters?keys?last],[/#if]
    [/#list]
  [/#if]
  },
  [#if importStatus.targetPath??]
  "targetPath"                     : "${importStatus.targetPath?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  [/#if]
  "stopping"                       : ${importStatus.isStopping()?c},
  "scanning"                       : ${importStatus.isScanning()?c},
  "paused"                         : ${importStatus.isPaused()?c},
  "succeeded"                      : ${importStatus.succeeded()?c},
  "failed"                         : ${importStatus.failed()?c},
  "stopped"                        : ${importStatus.stopped()?c},
  "inPlaceImportPossible"          : ${importStatus.inPlaceImportPossible()?c},
  "dryRun"                         : ${importStatus.dryRun?c},
  [#if importStatus.startDate??]
  "startDate"                      : "${importStatus.startDate?datetime?iso_utc}",
  [/#if]
  [#if importStatus.scanEndDate??]
  "scanEndDate"                    : "${importStatus.scanEndDate?datetime?iso_utc}",
  [/#if]
  [#if importStatus.endDate??]
  "endDate"                        : "${importStatus.endDate?datetime?iso_utc}",
  [/#if]
  [#if importStatus.scanDurationInNs??]
  "scanDurationInNs"               : ${importStatus.scanDurationInNs?c},
    [#if importStatus.scanDuration??]
  "scanDuration"                   : "${importStatus.scanDuration?js_string?replace("\\'", "'")?replace("\\>", ">")}",
    [/#if]
  [/#if]
  [#if importStatus.durationInNs??]
  "durationInNs"                   : ${importStatus.durationInNs?c},
    [#if importStatus.duration??]
  "duration"                       : "${importStatus.duration?js_string?replace("\\'", "'")?replace("\\>", ">")}",
    [/#if]
  [/#if]
  [#if importStatus.estimatedRemainingDurationInNs??]
  "estimatedRemainingDurationInNs" : ${importStatus.estimatedRemainingDurationInNs?c},
    [#if importStatus.estimatedRemainingDuration??]
  "estimatedRemainingDuration"     : "${importStatus.estimatedRemainingDuration?js_string?replace("\\'", "'")?replace("\\>", ">")}",
    [/#if]
  [/#if]
  [#if importStatus.lastExceptionAsString??]
  "lastException"                  : "${importStatus.lastExceptionAsString?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  [/#if]
  "batchWeight"                    : ${importStatus.batchWeight?c},
  "queuedBatches"                  : ${importStatus.queueSize?c},
  "maxQueuedBatches"               : ${importStatus.queueCapacity?c},
  "numberOfActiveThreads"          : ${importStatus.numberOfActiveThreads?c},
  "totalNumberOfThreads"           : ${importStatus.totalNumberOfThreads?c},
  [#if importStatus.currentlyScanning??]
  "currentlyScanning"              : "${importStatus.currentlyScanning?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  [/#if]
  [#if importStatus.currentlyImporting??]
  "currentlyImporting"             : "${importStatus.currentlyImporting?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  [/#if]
  "sourceCounters" : {
  [#if importStatus.sourceCounterNames??]
    [#list importStatus.sourceCounterNames as counterName]
      [#assign counterValue     = importStatus.getSourceCounter(counterName)!0]
      [#assign counterValueRate = importStatus.getSourceCounterRate(counterName)!0]
    "${counterName?js_string?replace("\\'", "'")?replace("\\>", ">")}" : {
      "Count" : ${counterValue?c},
      "Rate"  : ${counterValueRate?c}
    }[#if counterName != importStatus.sourceCounterNames?last],[/#if]
    [/#list]
  [/#if]
  },
  "targetCounters" : {
  [#if importStatus.targetCounterNames??]
    [#list importStatus.targetCounterNames as counterName]
      [#assign counterValue     = importStatus.getTargetCounter(counterName)!0]
      [#assign counterValueRate = importStatus.getTargetCounterRate(counterName)!0]
    "${counterName?js_string?replace("\\'", "'")?replace("\\>", ">")}" : {
      "Count" : ${counterValue?c},
      "Rate"  : ${counterValueRate?c}
    }[#if counterName != importStatus.targetCounterNames?last],[/#if]
    [/#list]
  [/#if]
  }
[/#if]
}
