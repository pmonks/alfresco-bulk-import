[#ftl]
[#macro stateToHtmlColour state="Never run"]
  [@compress single_line=true]
    [#if     state="Scanning"]   darkcyan
    [#elseif state="Importing"]  darkblue
    [#elseif state="Paused"]     darkgray
    [#elseif state="Stopping"]   orange
    [#elseif state="Never run"]  black
    [#elseif state="Successful"] green
    [#elseif state="Failed"]     red
    [#elseif state="Stopped"]    orange
    [#else]                      black
    [/#if]
  [/@compress]
[/#macro]
<!DOCTYPE HTML>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js"> <!--<![endif]-->
<html>
<head>
    <meta charset="utf-8">
    <link href='//fonts.googleapis.com/css?family=Open+Sans:400italic,600italic,400,600' rel='stylesheet' type='text/css'>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Bulk Import Tool - Status</title>
    <meta name="description" content="Bulk Import Tool - Status">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    [#-- 3rd Party Stuff --]
    <link rel="stylesheet" href="//code.jquery.com/ui/1.12.0/themes/smoothness/jquery-ui.css">
    <script src="//code.jquery.com/jquery-2.2.4.js"></script>
    <script src="//code.jquery.com/ui/1.12.0/jquery-ui.js"></script>
    <script src="//cdnjs.cloudflare.com/ajax/libs/loglevel/1.4.0/loglevel.min.js"></script>
    <script src="${url.context}/scripts/bulkimport/modernizr-3.3.1.min.js"></script>
    <script src="${url.context}/scripts/bulkimport/favicon.min.js"></script>
    <script src="${url.context}/scripts/bulkimport/smoothie.js"></script>
    [#-- Bulk import --]
    <link rel="stylesheet" href="${url.context}/css/bulkimport/normalize.css">
    <link rel="stylesheet" href="${url.context}/css/bulkimport/main.css">
    <link rel="stylesheet" href="${url.context}/css/bulkimport/bulkimport.css">
    <script src="${url.context}/scripts/bulkimport/bulkimport.js"></script>
</head>
<body>
    <!--[if lt IE 9]>
        <p class="browsehappy">You are using an <strong>outdated</strong> browser. Please <a href="http://browsehappy.com/">upgrade your browser</a> to improve your experience.</p>
    <![endif]-->
    <div class="container">
      <div class="block">
        <img style="margin:15px;vertical-align:middle" src="${url.context}/images/bulkimport/logo.png" alt="Alfresco Bulk Import Tool" />
      </div>
      <div class="block">
        <h1><strong>Bulk Import Status</strong></h1>
      </div>
    </div>

[#if importStatus.inProgress()]
    <div style="display:inline-block;height:50px;font-size:16pt">
      <div id="currentStatus" style="display:inline-block;color:black;bold">In progress ${(importStatus.duration!"")?html}</div><div id="estimatedDuration" style="display:inline-block;">, estimated completion in &lt;unknown&gt;.</div>
    </div>
    <p>
  [#if importStatus.isPaused()]
      <button id="pauseImportButton" style="display:none" class="button orange" type="button">&#10074;&#10074; Pause import</button>
      <button id="resumeImportButton" style="display:inline" class="button green" type="button">&#9658; Resume import</button>
  [#else]
      <button id="pauseImportButton" style="display:inline" class="button orange" type="button">&#10074;&#10074; Pause import</button>
      <button id="resumeImportButton" style="display:none" class="button green" type="button">&#9658; Resume import</button>
  [/#if]
      <button id="stopImportButton" style="display:inline" class="button red" type="button">&#9724; Stop import</button>
      <a id="initiateAnotherImport" style="display:none" href="${url.serviceContext}/bulk/import">Initiate another import</a>
    </p>
[#else]
    <div style="display:inline-block;height:50px;font-size:16pt">
      <div id="currentStatus" style="display:inline-block;color:[@stateToHtmlColour importStatus.processingState/];bold">${(importStatus.processingState!"")?html}</div><div id="estimatedDuration" style="display:inline-block;"></div>
    </div>
    <p>
      <button id="pauseImportButton" style="display:none" class="button orange" type="button">&#10074;&#10074; Pause import</button>
      <button id="resumeImportButton" style="display:none" class="button green" type="button">&#9658; Resume import</button>
      <button id="stopImportButton" style="display:none" class="button red" type="button">&#9724; Stop import</button>
      <a id="initiateAnotherImport" href="${url.serviceContext}/bulk/import">Initiate another import</a>
    </p>
[/#if]

    <div id="accordion">
      <h3>Graphs</h3>
      <div>
        <p><strong>Nodes Imported Per Second</strong></p>
        <p><canvas id="nodesPerSecondChart" width="1000" height="200"></canvas></p>

        <p><strong>Bytes Imported Per Second</strong></p>
        <p><canvas id="bytesPerSecondChart" width="1000" height="200"></canvas></p>
        <p>
          <span style="color:green;font-weight:bold">&#x2588; Moving average (committed content)</span>
          <br/>
          <span style="color:blue;font-weight:bold">&#x2588; Instantaneous rate (committed content)</span>
        </p>
      </div>

      <h3>Details</h3>
      <div>
        <table border="1" cellspacing="0" cellpadding="1" width="80%">
          <thead>
            <tr>
              <th colspan="2">General Statistics</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td width="25%">Status:</td>
              <td width="75%" id="detailsStatus" style="color:[@stateToHtmlColour importStatus.processingState/]">${(importStatus.processingState!"")?html}</td>
            </tr>
            <tr>
              <td>Initiating User:</td>
              <td>${(importStatus.initiatingUserId!"n/a")?html}</td>
            </tr>
            <tr>
              <td>Source Name:</td>
              <td>${(importStatus.sourceName!"n/a")?html}</td>
            </tr>
[#if importStatus.sourceParameters??]
  [#list importStatus.sourceParameters?keys as sourceParameterKey]
    [#assign sourceParameterValue = importStatus.sourceParameters[sourceParameterKey]]
            <tr>
              <td>${(sourceParameterKey!"n/a")?html}</td>
              <td>${(sourceParameterValue!"n/a")?string?html}</td>
            </tr>
  [/#list]
[/#if]
            <tr>
              <td>Target Space:</td>
              <td>${(importStatus.targetPath!"n/a")?html}</td>
            </tr>
            <tr>
              <td>Import Type:</td>
              <td>[#if importStatus.neverRun()]n/a[#elseif importStatus.inPlaceImportPossible()]In place[#else]Streaming[/#if]</td>
            </tr>
            <tr>
              <td>Dry Run:</td>
              <td>[#if importStatus.neverRun()]n/a[#elseif importStatus.dryRun]Yes[#else]No[/#if]</td>
            </tr>
            <tr>
              <td>Batch Weight:</td>
              <td>[#if importStatus.neverRun()]n/a[#else]${importStatus.batchWeight}[/#if]</td>
            </tr>
            <tr>
              <td>Queued Batches:</td>
              <td><span id="detailsQueueSize">[#if importStatus.neverRun()]0[#else]${importStatus.queueSize}[/#if]</span>
                  of a maximum of ${importStatus.queueCapacity}</td>
            </tr>
            <tr>
              <td>Threads:</td>
              <td><span id="detailsActiveThreads">[#if importStatus.neverRun()]0[#else]${importStatus.numberOfActiveThreads}[/#if]</span>
                  active of <span id="detailsTotalThreads">[#if importStatus.neverRun()]0[#else]${importStatus.totalNumberOfThreads}[/#if]</span> total</td>
            </tr>
            <tr>
              <td>Start Date:</td>
              <td>[#if importStatus.startDate??]${importStatus.startDate?datetime?iso_utc}[#else]n/a[/#if]</td>
            </tr>
            <tr>
              <td>Scan End Date:</td>
              <td id="detailsScanEndDate">[#if importStatus.scanEndDate??]${importStatus.scanEndDate?datetime?iso_utc}[#else]n/a[/#if]</td>
            </tr>
            <tr>
              <td>End Date:</td>
              <td id="detailsEndDate">[#if importStatus.endDate??]${importStatus.endDate?datetime?iso_utc}[#else]n/a[/#if]</td>
            </tr>
            <tr>
              <td>Scan Duration:</td>
              <td id="detailsScanDuration">${(importStatus.scanDuration!"n/a")?html}</td>
            </tr>
            <tr>
              <td>Duration:</td>
              <td id="detailsDuration">${(importStatus.duration!"n/a")?html}</td>
            </tr>
            <tr>
              <td>Currently Importing:</td>
              <td id="detailsCurrentlyImporting">${(importStatus.currentlyImporting!"n/a")?html}</td>
            </tr>
          </tbody>
        </table>

        <br/>

[#-- SOURCE COUNTERS --]
        <table id="sourceCounterTable" border="1" cellspacing="0" cellpadding="1" width="80%">
          <thead>
            <tr>
              <th colspan="3">Source (read) Statistics</th>
            </tr>
          </thead>
          <tbody id="sourceCounterTableBody">
[#if importStatus.neverRun() || !importStatus.sourceCounters??]
            <tr>
              <td colspan="3">n/a</td>
            </tr>
[#else]
  [#list importStatus.sourceCounters?keys as counterKey]
    [#assign count = importStatus.sourceCounters[counterKey].Count]
    [#assign rate  = importStatus.sourceCounters[counterKey].Rate]
            <tr>
              <td width="25%">${counterKey?html}</td>
              <td width="35%">${count}</td>
              <td width="40%">${rate} / sec</td>
            </tr>
  [/#list]
[/#if]
          </tbody>
        </table>

        <br/>

[#-- TARGET COUNTERS --]
        <table id="targetCounterTable" border="1" cellspacing="0" cellpadding="1" width="80%">
          <thead>
            <tr>
              <th colspan="3">Target (write) Statistics</th>
            </tr>
          </thead>
          <tbody id="targetCounterTableBody">
[#if importStatus.neverRun() || !importStatus.targetCounters??]
            <tr>
              <td colspan="3">n/a</td>
            </tr>
[#else]
  [#list importStatus.targetCounters?keys as counterKey]
    [#assign count = importStatus.targetCounters[counterKey].Count]
    [#assign rate  = importStatus.targetCounters[counterKey].Rate]
            <tr>
              <td width="25%">${counterKey}</td>
              <td width="35%">${count}</td>
              <td width="40%">${rate} / sec</td>
            </tr>
  [/#list]
[/#if]
          </tbody>
        </table>

[#-- ERROR INFORMATION --]
[#if importStatus.lastExceptionAsString??]
        <div id="detailsErrorInformation" style="display:block">
[#else]
        <div id="detailsErrorInformation" style="display:none">
[/#if]
          <br/>
          <table border="1" cellspacing="0" cellpadding="1" width="80%">
            <thead>
              <tr>
                <th colspan="2">Error Information</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Exception:</td>
                <td><pre style="font-size:8pt" id="detailsLastException">${importStatus.lastExceptionAsString!"n/a"}</pre></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>  [#-- End of accordion --]

    <p>Note: you may close this page at any time - any active imports will continue running.</p>

    <p>Please see the <a target="_blank" href="https://github.com/pmonks/alfresco-bulk-import">project site</a> for documentation, known issues, updated versions, etc.</p>
    <hr/>
    <p class="footnote">Bulk Import Tool v2.1.1-SNAPSHOT, Alfresco ${server.edition} v${server.version}</p>

<script>
  $(document).ready(function() {
    initStatus('${url.context?js_string}', '${url.serviceContext?js_string}');

    $("#accordion").accordion({
      active: 0,
      heightStyle: "content"
    });

    $("#pauseImportButton").button().click(function() {
      pauseImport();
    });

    $("#resumeImportButton").button().click(function() {
      resumeImport();
    });

    $("#stopImportButton").button().click(function() {
      stopImport();
    });
  });
</script>

</body>
</html>
