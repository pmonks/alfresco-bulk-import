[#ftl]
{
  "processingState"                : "${importStatus.processingState?js_string}",
  "inProgress"                     : ${importStatus.inProgress()?c},
  "neverRun"                       : ${importStatus.neverRun()?c}
[#if !importStatus.neverRun()]
  ,
  [#if importStatus.sourceName??]
  "sourceName"                     : "${importStatus.sourceName?js_string}",
  [/#if]
  "sourceParameters" : {
  [#if importStatus.sourceParameters??]
    [#list importStatus.sourceParameters?keys as parameterName]
      [#assign parameterValue = importStatus.sourceParameters[parameterName]]
    "${parameterName?js_string}" : "${parameterValue?js_string}"[#if parameterName != importStatus.sourceParameters?keys?last],[/#if]
    [/#list]
  [/#if]
  },
  [#if importStatus.targetPath??]
  "targetPath"                     : "${importStatus.targetPath?js_string}",
  [/#if]
  "stopping"                       : ${importStatus.isStopping()?c},
  "scanning"                       : ${importStatus.isScanning()?c},
  "succeeded"                      : ${importStatus.succeeded()?c},
  "failed"                         : ${importStatus.failed()?c},
  "stopped"                        : ${importStatus.stopped()?c},
  "inPlaceImportPossible"          : ${importStatus.inPlaceImportPossible()?c},
  "dryRun"                         : ${importStatus.dryRun?c},
  [#if importStatus.startDate??]
  "startDate"                      : "${importStatus.startDate?datetime?iso_utc}",
  [/#if]
  [#if importStatus.endDate??]
  "endDate"                        : "${importStatus.endDate?datetime?iso_utc}",
  [/#if]
  [#if importStatus.durationInNs??]
  "durationInNs"                   : ${importStatus.durationInNs?c},
    [#if importStatus.duration??]
  "duration"                       : "${importStatus.duration?js_string}",
    [/#if]
  [/#if]
  [#if importStatus.estimatedRemainingDurationInNs??]
  "estimatedRemainingDurationInNs" : ${importStatus.estimatedRemainingDurationInNs?c},
    [#if importStatus.estimatedRemainingDuration??]
  "estimatedRemainingDuration"     : "${importStatus.estimatedRemainingDuration?js_string}",
    [/#if]
  [/#if]
  [#if importStatus.lastExceptionAsString??]
  "lastExceptionAsString"          : "${importStatus.lastExceptionAsString?js_string}",
  [/#if]
  "batchWeight"                    : ${importStatus.batchWeight?c},
  "numberOfActiveThreads"          : ${importStatus.numberOfActiveThreads?c},
  "totalNumberOfThreads"           : ${importStatus.totalNumberOfThreads?c},
  [#if importStatus.currentlyScanning??]
  "currentlyScanning"              : "${importStatus.currentlyScanning?js_string}",
  [/#if]
  [#if importStatus.currentlyImporting??]
  "currentlyImporting"             : "${importStatus.currentlyImporting?js_string}",
  [/#if]
  "sourceCounters" : {
  [#if importStatus.sourceCounterNames??]
    [#list importStatus.sourceCounterNames as counterName]
      [#assign counterValue     = importStatus.getSourceCounter(counterName)!0]
      [#assign counterValueRate = importStatus.getSourceCounterRate(counterName)!0]
    "${counterName?js_string}" : {
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
    "${counterName?js_string}" : {
      "Count" : ${counterValue?c},
      "Rate"  : ${counterValueRate?c}
    }[#if counterName != importStatus.targetCounterNames?last],[/#if]
    [/#list]
  [/#if]
  }
[/#if]
}
