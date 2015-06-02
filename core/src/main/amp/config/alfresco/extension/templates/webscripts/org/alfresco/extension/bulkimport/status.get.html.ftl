[#ftl]
[#macro formatDuration durationInNs=0]
  [@compress single_line=true]
    [#assign days         = (durationInNs / (1000 * 1000 * 1000 * 60 * 60 * 24))?floor]
    [#assign hours        = (durationInNs / (1000 * 1000 * 1000 * 60 * 60))?floor % 24]
    [#assign minutes      = (durationInNs / (1000 * 1000 * 1000 * 60))?floor % 60]
    [#assign seconds      = (durationInNs / (1000 * 1000 * 1000))?floor % 60]
    [#assign milliseconds = (durationInNs / (1000 * 1000)) % 1000]
    [#assign microseconds = (durationInNs / (1000)) % 1000]
    ${days}d ${hours}h ${minutes}m ${seconds}s ${milliseconds}.${microseconds}ms
  [/@compress]
[/#macro]
[#macro formatBytes bytes=0]
  [@compress single_line=true]
    [#if     bytes > (1024 * 1024 * 1024 * 1024 * 1024)]${(bytes / (1024 * 1024 * 1024 * 1024 * 1024))?string("#,##0.00")}PB
    [#elseif bytes > (1024 * 1024 * 1024 * 1024)]${(bytes / (1024 * 1024 * 1024 * 1024))?string("#,##0.00")}TB
    [#elseif bytes > (1024 * 1024 * 1024)]${(bytes / (1024 * 1024 * 1024))?string("#,##0.00")}GB
    [#elseif bytes > (1024 * 1024)]${(bytes / (1024 * 1024))?string("#,##0.00")}MB
    [#elseif bytes > 1024]${(bytes / 1024)?string("#,##0.00")}kB
    [#else]${bytes?string("#,##0")}B
    [/#if]
  [/@compress]
[/#macro]
[#macro stateToHtmlColour state="Never run"]
  [@compress single_line=true]
    [#if     state="Scanning"]   darkcyan
    [#elseif state="Importing"]  darkblue
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
    <link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
    <script src="//code.jquery.com/jquery-2.1.4.js"></script>
    <script src="//code.jquery.com/ui/1.11.4/jquery-ui.js"></script>
    <script src="//cdnjs.cloudflare.com/ajax/libs/loglevel/1.2.0/loglevel.min.js"></script>
    <script src="${url.context}/scripts/bulkimport/modernizr-2.8.3.min.js"></script>
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
    <p><button id="stopImportButton" class="button red" type="button">Stop import</button></p>
    <p><a id="initiateAnotherImport" style="display:none" href="${url.serviceContext}/bulk/import">Initiate another import</a></p>
[#else]
    <div style="display:inline-block;height:50px;font-size:16pt">
      <div id="currentStatus" style="display:inline-block;color:green;bold">Idle</div><div id="estimatedDuration" style="display:inline-block;"></div>
    </div>
    <p><button id="stopImportButton" style="display:none" class="button red" type="button">Stop import</button></p>
    <p><a id="initiateAnotherImport" href="${url.serviceContext}/bulk/import">Initiate another import</a></p>
[/#if]

    <div id="accordion">
      <h3>Graphs</h3>
      <div>
        <p><strong>Nodes Imported Per Second</strong></p>
        <table border="0" cellspacing="10" cellpadding="0">
          <tr>
            <td align="left" valign="top" width="75%">
              <canvas id="nodesPerSecondChart" width="1000" height="200"></canvas>
            </td>
            <td align="left" valign="top" width="25%">
              <span style="color:green;font-weight:bold"> Green = moving average</span><br/>
              <span style="color:blue;font-weight:bold"> Blue = instantaneous rate</span><br/>
            </td>
          </tr>
        </table>
    
        <p><strong>Bytes Imported Per Second</strong></p>
        <table border="0" cellspacing="10" cellpadding="0">
          <tr>
            <td align="left" valign="top" width="75%">
              <canvas id="bytesPerSecondChart" width="1000" height="200"></canvas>
            </td>
            <td align="left" valign="top" width="25%">
              <span style="color:green;font-weight:bold"> Green = moving average</span><br/>
              <span style="color:blue;font-weight:bold"> Blue = instantaneous rate</span><br/>
            </td>
          </tr>
        </table>
      </div>

      <h3>Details</h3>
      <div>
        <p>Refreshes every 5 seconds.</p>
        <table border="1" cellspacing="0" cellpadding="1" width="80%">
          <tr>
            <td colspan="2"><strong>General Statistics</strong></td>
          </tr>
          <tr>
            <td width="25%">Status:</td>
            <td width="75%" id="detailsStatus" style="color:[@stateToHtmlColour importStatus.processingState/]">${(importStatus.processingState!"")?html}</td>
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
            <td>Dry run:</td>
            <td>[#if importStatus.neverRun()]n/a[#elseif importStatus.dryRun]Yes[#else]No[/#if]</td>
          </tr>
          <tr>
            <td>Batch Weight:</td>
            <td>[#if importStatus.neverRun()]n/a[#else]${importStatus.batchWeight}[/#if]</td>
          </tr>
          <tr>
            <td>Threads:</td>
            <td><span id="detailsActiveThreads">[#if importStatus.neverRun()]0[#else]${importStatus.numberOfActiveThreads}[/#if]</span>
                (of <span id="detailsTotalThreads">[#if importStatus.neverRun()]0[#else]${importStatus.totalNumberOfThreads}[/#if]</span> total)</td>
          </tr>
          <tr>
            <td>Start Date:</td>
            <td>[#if importStatus.startDate??]${importStatus.startDate?datetime?iso_utc}[#else]n/a[/#if]</td>
          </tr>
          <tr>
            <td>End Date:</td>
            <td id="detailsEndDate">[#if importStatus.endDate??]${importStatus.endDate?datetime?iso_utc}[#else]n/a[/#if]</td>
          </tr>
          <tr>
            <td>Duration:</td>
            <td id="detailsDuration">${(importStatus.duration!"n/a")?html}</td>
          </tr>
        </table>
        
        <br/>
                  
[#-- SOURCE COUNTERS --]
        <table id="sourceCounterTable" border="1" cellspacing="0" cellpadding="1" width="80%">
          <tr>
            <td colspan="2"><strong>Source (read) Statistics</strong></td>
          </tr>
[#if importStatus.neverRun() || !importStatus.sourceCounters??]
          <tr>
            <td colspan="2">n/a</td>
          </tr>
[#else]
  [#list importStatus.sourceCounters?keys as counterKey]
    [#assign count = importStatus.sourceCounters[counterKey].Count]
    [#assign rate  = importStatus.sourceCounters[counterKey].Rate]
          <tr>
            <td>${counterKey?html}</td>
            <td>${count} (${rate} / second)</td>
          </tr>
  [/#list]
[/#if]
        </table>
        
        <br/>

[#-- TARGET COUNTERS --]
        <table id="targetCounterTable" border="1" cellspacing="0" cellpadding="1" width="80%">
          <tr>
            <td colspan="2"><strong>Target (write) Statistics</strong></td>
          </tr>
[#if importStatus.neverRun() || !importStatus.targetCounters??]
          <tr>
            <td colspan="2">n/a</td>
          </tr>
[#else]
  [#list importStatus.targetCounters?keys as counterKey]
    [#assign count = importStatus.targetCounters[counterKey].Count]
    [#assign rate  = importStatus.targetCounters[counterKey].Rate]
          <tr>
            <td>${counterKey}</td>
            <td>${count} (${rate} / second)</td>
          </tr>
  [/#list]
[/#if]
        </table>

[#-- ERROR INFORMATION --]
        <div id="detailsErrorInformation" style="display:none">
          <br/>
          <table border="1" cellspacing="0" cellpadding="1" width="80%">
            <tr>
              <td colspan="2"><strong>Error Information From Last Run</strong></td>
            </tr>
            <tr>
              <td style="vertical-align:top">Exception:</td>
              <td><pre id="detailsLastException">${importStatus.lastExceptionAsString!"n/a"}</pre></td>
            </tr>
          </table>
        </div>
      </div>
    </div>  [#-- End of accordion --]

    <hr/>
    <p class="footnote">Bulk Import Tool v2.0-SNAPSHOT, Alfresco ${server.edition} v${server.version}</p>

<script>
  $(document).ready(function() {
    $("#accordion").accordion({
      active: 0,
      heightStyle: "content"
    });

    $("#stopImportButton").button().click(function() {
      stopImport();
    });

    initStatus('${url.context?js_string}', '${url.serviceContext?js_string}');
  });
</script>

</body>
</html>
