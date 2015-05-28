/*
 * Copyright (C) 2012-2015 Peter Monks.
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

/*
 * This file contains the browser functionality used by the HTML status page.
 */

//####TODO: CHANGE THIS PRIOR TO RELEASE!
var logLevel = log.levels.DEBUG;  // See http://pimterry.github.io/loglevel/ for details

// Global variables
var statusURI;
var stopURI;
var webAppContext;
var previousData;
var currentData;
var nodesPerSecondChart;
var nodesPerSecondChartTimer;
var bytesPerSecondChart;
var bytesPerSecondChartTimer;
var getImportStatusTimer;
var refreshTextTimer;

log.setLevel(logLevel);

/*
 * Initialise the status UI
 */
function initStatus(alfrescoWebAppContext, alfrescoWebScriptContext)
{
  statusURI     = alfrescoWebScriptContext + "/bulk/import/status.json";
  stopURI       = alfrescoWebScriptContext + "/bulk/import/stop";
  webAppContext = alfrescoWebAppContext;

  currentData = getStatusInfo();  // Pull down an initial set of status info

  if (currentData != null && currentData.inProgress === false)
  {
    // If the import completed before the page even loaded, update the text area then bomb out
    favicon.change(webAppContext + "/images/bulkimport/favicon.gif");
    refreshTextElements(currentDate);
  }
  else
  {
    log.debug('Import in progress, starting UI.');
    startSpinner();
    startImportStatusTimer();
//####TODO: GET THIS WORKING!!!!
//    startRefreshTextTimer();
    startNodesPerSecondChart();
    startBytesPerSecondChart();
  }
}


/*
 * Get status information via an AJAX call
 */
function getStatusInfo()
{
  log.debug('Retrieving import status information...');

  $.getJSON(statusURI, function(data) {
      try
      {
        previousData = currentData;
        currentData  = data;
      }
      catch (e)
      {
        log.error('Exception while retrieving status information: ' + e);
      }
      
      log.debug('Status information: ' + currentData);

      if (currentData != null)
      {
        // If we're idle, stop the world
        if (currentData.inProgress === false)
        {
          log.debug('Import complete, shutting down UI.');

          // Kill all the spinners, charts and timers
          favicon.stopAnimate();
          favicon.change(webAppContext + "/images/bulkimport/favicon.gif");

          if (nodesPerSecondChart      != null) nodesPerSecondChart.stop();
          if (nodesPerSecondChartTimer != null) { clearInterval(nodesPerSecondChartTimer); nodesPerSecondChartTimer = null; }
          if (bytesPerSecondChart      != null) bytesPerSecondChart.stop();
          if (bytesPerSecondChartTimer != null) { clearInterval(bytesPerSecondChartTimer); bytesPerSecondChartTimer = null; }
          if (getImportStatusTimer     != null) getImportStatusTimer.stop();
          if (refreshTextTimer         != null) refreshTextTimer.stop();

          // Update the status
          document.getElementById("currentStatus").textContent = "Idle";
          document.getElementById("currentStatus").style.color = "green";
          document.getElementById("estimatedDuration").textContent = "";
          
          toggleDivs(document.getElementById("stopImportButton"), document.getElementById("initiateAnotherImport"));
          
          // Update the text one last time
          refreshTextElements(currentData);
        }
        else  // We're not idle, so update the duration in the current status
        {
          document.getElementById("currentStatus").textContent = currentData.processingState + " " + currentData.duration;

          if (currentData.estimatedRemainingDuration !== undefined)
          {
            document.getElementById("estimatedDuration").textContent = ", estimated completion in " + currentData.estimatedRemainingDuration;
          }
          else
          {
            document.getElementById("estimatedDuration").textContent = ", estimated completion in <unknown>";
          }
        }
      }
      else
      {
        log.warn('No data received from server.');
      }
  });
}


function startSpinner()
{
  favicon.animate([
    webAppContext + "/images/bulkimport/spinner-frame-01.gif",
    webAppContext + "/images/bulkimport/spinner-frame-02.gif",
    webAppContext + "/images/bulkimport/spinner-frame-03.gif",
    webAppContext + "/images/bulkimport/spinner-frame-04.gif",
    webAppContext + "/images/bulkimport/spinner-frame-05.gif",
    webAppContext + "/images/bulkimport/spinner-frame-06.gif",
    webAppContext + "/images/bulkimport/spinner-frame-07.gif",
    webAppContext + "/images/bulkimport/spinner-frame-08.gif"
  ], 250);
}


/*
 * Start the timer that periodically pulls the import status info down
 */
function startImportStatusTimer()
{
  log.debug('Starting import status timer...');

  var getImportStatus = function()
  {
    getStatusInfo();
  };

  setInterval(getImportStatus, 1000)
}


/*
 * Start the timer that refreshes the details section of the page
 */
function startRefreshTextTimer()
{
  log.debug('Starting refresh text timer...');

  var refreshText = function()
  {
    refreshTextElements(currentData);
  };

  setInterval(refreshText, 5000)
}


/*
 * Start the "nodes per second" chart
 */
function startNodesPerSecondChart()
{
  log.debug('Starting nodes per second chart...');
  
  var canvasElement = document.getElementById('nodesPerSecondChart');

  // Initialise the nodes per second chart
  nodesPerSecondChart = new SmoothieChart({
    grid: { strokeStyle      : 'rgb(127, 127, 127)',
            fillStyle        : 'rgb(0, 0, 0)',
            lineWidth        : 1,
            millisPerLine    : 500,
            verticalSections : 10 },
    labels: { fillStyle :'rgb(255, 255, 255)' },
    minValue: 0
  });

  nodesPerSecondChart.streamTo(canvasElement, 1000);  // 1 second delay in rendering (for extra smoothiness!)

  // Times series'
  var movingAverageTimeSeries = new TimeSeries();
  var instantaneousTimeSeries = new TimeSeries();

  // Update the graph every second
  nodesPerSecondChartTimer = setInterval(function()
  {
    log.debug('Updating nodes per second chart...');

    var now           = new Date().getTime();
    var pd            = previousData;
    var cd            = currentData;
    var movingAverage = 0;
    var instantaneous = 0;

    if (cd != null)
    {
      movingAverage = cd.targetCounters["Nodes imported per second"];
      
      if (pd != null)
      {
        var previousNodesImported = pd.targetCounters["Nodes imported"];
        var currentNodesImported  = cd.targetCounters["Nodes imported"];
        
        instantaneous = Math.max(0, currentNodesImported - previousNodesImported);
      }
    }
    else
    {
      log.debug('No status data available for nodes per second chart.');
    }

    movingAverageTimeSeries.append(now, movingAverage);
    instantaneousTimeSeries.append(now, instantaneous);
  }, 1000);  // Update every second

  // Add the time series' to the chart
  nodesPerSecondChart.addTimeSeries(movingAverageTimeSeries, { strokeStyle : 'rgb(0, 255, 0)',   fillStyle : 'rgba(0, 255, 0, 0.0)', lineWidth : 3 } );
  nodesPerSecondChart.addTimeSeries(instantaneousTimeSeries, { strokeStyle : 'rgb(0, 0, 255)',   fillStyle : 'rgba(0, 0, 255, 0.0)', lineWidth : 3 } );
}


/*
 * Start the "bytes per second" chart
 */
function startBytesPerSecondChart()
{
  log.debug('Starting bytes per second chart...');

  var canvasElement = document.getElementById('bytesPerSecondChart');
  
  // Initialise the bytes per second chart
  bytesPerSecondChart = new SmoothieChart({
    grid: { strokeStyle      : 'rgb(127, 127, 127)',
            fillStyle        : 'rgb(0, 0, 0)',
            lineWidth        : 1,
            millisPerLine    : 500,
            verticalSections : 10 },
    labels: { fillStyle : 'rgb(255, 255, 255)' },
    minValue: 0
  });

  bytesPerSecondChart.streamTo(canvasElement, 1000);  // 1 second delay in rendering (for extra smoothiness!)

  // Data
  var movingAverageTimeSeries = new TimeSeries();
  var instantaneousTimeSeries = new TimeSeries();

  // Update the graph every second
  bytesPerSecondChartTimer = setInterval(function()
  {
    log.debug('Updating bytes per second chart...');

    var now           = new Date().getTime();
    var pd            = previousData;
    var cd            = currentData;
    var movingAverage = 0;
    var instantaneous = 0;

    if (cd != null)
    {
      movingAverage = cd.targetCounters["Bytes imported per second"];

      if (pd != null)
      {
        var previousBytesImported = pd.targetCounters["Bytes imported"];
        var currentBytesImported  = cd.targetCounters["Bytes imported"];
        
        instantaneous = Math.max(0, currentBytesImported - previousBytesImported);
      }
    }
    else
    {
      log.debug('No status data available for bytes per second chart.');
    }

    movingAverageTimeSeries.append(now, movingAverage);
    instantaneousTimeSeries.append(now, instantaneous);
  }, 1000);  // Update every second

  // Add the time series' to the chart
  bytesPerSecondChart.addTimeSeries(movingAverageTimeSeries, { strokeStyle : 'rgb(0, 255, 0)',   fillStyle : 'rgba(0, 255, 0, 0.0)', lineWidth : 3 } );
  bytesPerSecondChart.addTimeSeries(instantaneousTimeSeries, { strokeStyle : 'rgb(0, 0, 255)',   fillStyle : 'rgba(0, 0, 255, 0.0)', lineWidth : 3 } );
}


/*
 * Refresh all of the text elements on the page.
 */
function refreshTextElements(cd)
{
  log.debug('Refreshing text elements...');

  if (cd != null)
  {
    // Status
    document.getElementById("detailsStatus").textContent = cd.status;
    document.getElementById("detailsStatus").style.color = stateToColour(cd.status);

    // Threads
    if (cd.activeThreads === undefined)
    {
      document.getElementById("detailsActiveThreads").textContent = "0";
    }
    else
    {
      document.getElementById("detailsActiveThreads").textContent = cd.activeThreads;
    }

    if (cd.totalThreads === undefined)
    {
      document.getElementById("detailsTotalThreads").textContent = "0";
    }
    else
    {
      document.getElementById("detailsTotalThreads").textContent = cd.totalThreads;
    }

    // End date
    if (cd.endDate) document.getElementById("detailsEndDate").textContent = cd.endDate;

    // Duration
    if (cd.durationInNS) document.getElementById("detailsDuration").textContent = formatDuration(cd.durationInNS, true);

    // Completed batches
    document.getElementById("detailsCompletedBatches").textContent = cd.completedBatches;

    // Current file or folder
    document.getElementById("detailsCurrentFileOrFolder").textContent = cd.currentFileOrFolder;

    // Source (read) statistics
    document.getElementById("detailsFoldersScanned").textContent           = cd.sourceStatistics.foldersScanned;
    document.getElementById("detailsFilesScanned").textContent             = cd.sourceStatistics.filesScanned;
    document.getElementById("detailsUnreadableEntries").textContent        = cd.sourceStatistics.unreadableEntries;
    document.getElementById("detailsContentFilesRead").textContent         = cd.sourceStatistics.contentFilesRead;
    document.getElementById("detailsContentBytesRead").textContent         = formatBytes(cd.sourceStatistics.contentBytesRead);
    document.getElementById("detailsMetadataFilesRead").textContent        = cd.sourceStatistics.metadataFilesRead;
    document.getElementById("detailsMetadataBytesRead").textContent        = formatBytes(cd.sourceStatistics.metadataBytesRead);
    document.getElementById("detailsContentVersionFilesRead").textContent  = cd.sourceStatistics.contentVersionFilesRead;
    document.getElementById("detailsContentVersionBytesRead").textContent  = formatBytes(cd.sourceStatistics.contentVersionBytesRead);
    document.getElementById("detailsMetadataVersionFilesRead").textContent = cd.sourceStatistics.metadataVersionFilesRead;
    document.getElementById("detailsMetadataVersionBytesRead").textContent = formatBytes(cd.sourceStatistics.metadataVersionBytesRead);

    // Throughput (read)
    if (cd.durationInNS)
    {
      var durationInS = cd.durationInNS / (1000 * 1000 * 1000);
      document.getElementById("detailsEntriesScannedPerSecond").textContent = "" +
                                                                              roundToDigits((cd.sourceStatistics.filesScanned +
                                                                                             cd.sourceStatistics.foldersScanned) / durationInS, 2) +
                                                                              " entries scanned / sec";
      document.getElementById("detailsFilesReadPerSecond").textContent      = "" +
                                                                              roundToDigits((cd.sourceStatistics.contentFilesRead +
                                                                                             cd.sourceStatistics.metadataFilesRead +
                                                                                             cd.sourceStatistics.contentVersionFilesRead +
                                                                                             cd.sourceStatistics.metadataVersionFilesRead) / durationInS, 2) +
                                                                              " files read / sec";
      document.getElementById("detailsDataReadPerSecond").textContent       = "" +
                                                                              formatBytes((cd.sourceStatistics.contentBytesRead +
                                                                                           cd.sourceStatistics.metadataBytesRead +
                                                                                           cd.sourceStatistics.contentVersionBytesRead +
                                                                                           cd.sourceStatistics.metadataVersionBytesRead) / durationInS) +
                                                                              " / sec";
    }

    // Target (write) statistics
    document.getElementById("detailsSpaceNodesCreated").textContent               = cd.targetStatistics.spaceNodesCreated;
    document.getElementById("detailsSpaceNodesReplaced").textContent              = cd.targetStatistics.spaceNodesReplaced;
    document.getElementById("detailsSpaceNodesSkipped").textContent               = cd.targetStatistics.spaceNodesSkipped;
    document.getElementById("detailsSpacePropertiesWritten").textContent          = cd.targetStatistics.spacePropertiesWritten;
    document.getElementById("detailsContentNodesCreated").textContent             = cd.targetStatistics.contentNodesCreated;
    document.getElementById("detailsContentNodesReplaced").textContent            = cd.targetStatistics.contentNodesReplaced;
    document.getElementById("detailsContentNodesSkipped").textContent             = cd.targetStatistics.contentNodesSkipped;
    document.getElementById("detailsContentBytesWritten").textContent             = formatBytes(cd.targetStatistics.contentBytesWritten);
    document.getElementById("detailsContentPropertiesWritten").textContent        = cd.targetStatistics.contentPropertiesWritten;
    document.getElementById("detailsContentVersionsCreated").textContent          = cd.targetStatistics.contentVersionsCreated;
    document.getElementById("detailsContentVersionBytesWritten").textContent      = formatBytes(cd.targetStatistics.contentVersionsBytesWritten);
    document.getElementById("detailsContentVersionPropertiesWritten").textContent = cd.targetStatistics.contentVersionsPropertiesWritten;

    // Throughput (write)
    if (cd.durationInNS)
    {
      var durationInS = cd.durationInNS / (1000 * 1000 * 1000);
      document.getElementById("detailsNodesWrittenPerSecond").textContent = "" +
                                                                            roundToDigits((cd.targetStatistics.spaceNodesCreated +
                                                                                           cd.targetStatistics.spaceNodesReplaced +
                                                                                           cd.targetStatistics.contentNodesCreated +
                                                                                           cd.targetStatistics.contentNodesReplaced +
                                                                                           cd.targetStatistics.contentVersionsCreated) / durationInS, 2) +
                                                                            " nodes / sec";
      document.getElementById("detailsDataWrittenPerSecond").textContent  = "" +
                                                                            formatBytes((cd.targetStatistics.contentBytesWritten +
                                                                                         cd.targetStatistics.contentVersionsBytesWritten) / durationInS) +
                                                                            " / sec";
    }

    if (cd.errorInformation)
    {
      document.getElementById("detailsErrorInformation").style.display = "block";
      document.getElementById("detailsFileThatFailed").textContent     = cd.errorInformation.fileThatFailed;
      document.getElementById("detailsLastException").textContent      = cd.errorInformation.exception;
    }
  }
}


function stopImport()
{
  var stopImportButton = $("#stopImportButton");
  
  stopImportButton.prop('disabled', true);
  stopImportButton.text('Stopping...');
  stopImportButton.switchClass('red', 'gray');
  
  $.post(stopURI).fail(function() { log.error("Error when calling " + stopURI + "."); });
}


function stateToColour(state)
{
  var result = "black";

  // Note: these names must line up with the processing state values returned by the server
  if      (state === "Scanning")   result = "darkcyan";
  else if (state === "Importing")  result = "darkblue";
  else if (state === "Stopping")   result = "orange";
  else if (state === "Never run")  result = "black";
  else if (state === "Succeeded")  result = "green";
  else if (state === "Failed")     result = "red";
  else if (state === "Stopped")    result = "orange";
  else                             result = "black";

  return(result);
}


function formatDuration(durationInNs, details)
{
  var days         = Math.floor(durationInNs / (1000 * 1000 * 1000 * 60 * 60 * 24));
  var hours        = Math.floor(durationInNs / (1000 * 1000 * 1000 * 60 * 60)) % 24;
  var minutes      = Math.floor(durationInNs / (1000 * 1000 * 1000 * 60)) % 60;
  var seconds      = Math.floor(durationInNs / (1000 * 1000 * 1000)) % 60;
  var milliseconds = Math.floor(durationInNs / (1000 * 1000)) % 1000;
  var microseconds = Math.floor(durationInNs / (1000)) % 1000;

  if (details === true)
  {
    return("" + days + "d " + hours + "h " + minutes + "m " + seconds + "s " + milliseconds + "." + microseconds + "ms");
  }
  else
  {
    return("" + days + "d " + hours + "h " + minutes + "m " + seconds + "s ");
  }
}


function formatBytes(bytes)
{
  if      (bytes > (1024 * 1024 * 1024 * 1024 * 1024)) return("" + roundToDigits(bytes / (1024 * 1024 * 1024 * 1024 * 1024), 2) + "PB");
  else if (bytes > (1024 * 1024 * 1024 * 1024))        return("" + roundToDigits(bytes / (1024 * 1024 * 1024 * 1024), 2) + "TB");
  else if (bytes > (1024 * 1024 * 1024))               return("" + roundToDigits(bytes / (1024 * 1024 * 1024), 2) + "GB");
  else if (bytes > (1024 * 1024))                      return("" + roundToDigits(bytes / (1024 * 1024), 2) + "MB");
  else if (bytes > 1024)                               return("" + roundToDigits(bytes / 1024, 2) + "kB");
  else                                                 return("" + roundToDigits(bytes, 2) + "B");
}


function roundToDigits(number, numberOfDigits)
{
  var multiplicationFactor = Math.pow(10, numberOfDigits);

  return(Math.round(number * multiplicationFactor) / multiplicationFactor);
}


/*
 * Toggle visibility of two div elements
 */
function toggleDivs(elementToHide, elementToShow)
{
  elementToHide.style.display = "none";
  elementToShow.style.display = "block";
}
