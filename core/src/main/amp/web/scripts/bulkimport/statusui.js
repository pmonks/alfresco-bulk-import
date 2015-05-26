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
var previousData;
var currentData;
var spinner;
var filesPerSecondChart;
var filesPerSecondChartTimer;
var bytesPerSecondChart;
var bytesPerSecondChartTimer;
var getImportStatusTimer;
var refreshTextTimer;

log.setLevel(logLevel);

/*
 * Boot the UI
 */
function onLoad(alfrescoWebScriptContext)
{
  statusURI = alfrescoWebScriptContext + "/bulk/import/status.json";

  currentData = getStatusInfo();  // Pull down an initial set of status info

  if (currentData != null && currentData.inProgress === false)
  {
    // If the import completed before the page even loaded, update the text area then bomb out
    refreshTextElements(currentDate);
  }
  else
  {
    log.debug('Import in progress, starting UI.');
    startSpinner();
    startImportStatusTimer();
    startRefreshTextTimer();
    startFilesPerSecondChart(document.getElementById('filesPerSecondChart'));
    startBytesPerSecondChart(document.getElementById('bytesPerSecondChart'));
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

      if (currentData != null)
      {
        // If we're idle, stop the world
        if (currentData.inProgress === false)
        {
          log.debug('Import complete, shutting down UI.');

          // Update the text one last time
          refreshTextElements(currentData);

          // Kill all the spinners, charts and timers
          if (spinner                  != null) spinner.stop();
          if (filesPerSecondChart      != null) filesPerSecondChart.stop();
          if (filesPerSecondChartTimer != null) { clearInterval(filesPerSecondChartTimer); filesPerSecondChartTimer = null; }
          if (bytesPerSecondChart      != null) bytesPerSecondChart.stop();
          if (bytesPerSecondChartTimer != null) { clearInterval(bytesPerSecondChartTimer); bytesPerSecondChartTimer = null; }
          if (getImportStatusTimer     != null) getImportStatusTimer.stop();
          if (refreshTextTimer         != null) refreshTextTimer.stop();

          // Update the status
          document.getElementById("spinner").style.display               = "none";
          document.getElementById("currentStatus").textContent           = "Idle";
          document.getElementById("currentStatus").style.color           = "green";
          document.getElementById("stopImportButton").style.display      = "none";
          document.getElementById("initiateAnotherImport").style.display = "block";
        }
        else  // We're not idle, so update the duration in the current status
        {
          document.getElementById("currentStatus").textContent = "In progress " + currentData.duration;

          if (currentData.estimatedRemainingDuration !== undefined)
          {
            document.getElementById("estimatedDuration").textContent = "Estimated completion in " + currentData.estimatedRemainingDuration;
          }
        }
      }

  });
}


function startSpinner()
{
  var spinnerOptions = {
    lines     : 13,         // The number of lines to draw
    length    : 7,          // The length of each line
    width     : 4,          // The line thickness
    radius    : 10,         // The radius of the inner circle
    corners   : 1,          // Corner roundness (0..1)
    rotate    : 0,          // The rotation offset
    color     : '#000',     // #rgb or #rrggbb
    speed     : 1,          // Rounds per second
    trail     : 60,         // Afterglow percentage
    shadow    : false,      // Whether to render a shadow
    hwaccel   : true,       // Whether to use hardware acceleration
    className : 'spinner',  // The CSS class to assign to the spinner
    zIndex    : 2e9,        // The z-index (defaults to 2000000000)
    top       : 'auto',     // Top position relative to parent in px
    left      : 'auto'      // Left position relative to parent in px
  };

  var target = document.getElementById('spinner');
  spinner = new Spinner(spinnerOptions).spin(target);
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
 * Start the "files per second" chart
 */
function startFilesPerSecondChart(canvasElement)
{
  log.debug('Starting files per second chart...');

  // Initialise the files per second chart
  filesPerSecondChart = new SmoothieChart({
    grid: { strokeStyle      : 'rgb(127, 127, 127)',
            fillStyle        : 'rgb(0, 0, 0)',
            lineWidth        : 1,
            millisPerLine    : 500,
            verticalSections : 10 },
    labels: { fillStyle :'rgb(255, 255, 255)' }
  });

  filesPerSecondChart.streamTo(canvasElement, 1000);  // 1 second delay in rendering (for extra smoothiness!)

  // Data
  var fileScannedTimeSeries  = new TimeSeries();
  var filesReadTimeSeries    = new TimeSeries();
  var nodesCreatedTimeSeries = new TimeSeries();
  var filesZeroTimeSeries    = new TimeSeries();

  // Update the graph every second
  filesPerSecondChartTimer = setInterval(function()
  {
    log.debug('Updating files per second chart...');

    var now          = new Date().getTime();
    var pd           = previousData;
    var cd           = currentData;
    var filesScanned = 0;
    var filesRead    = 0;
    var nodesCreated = 0;

    if (cd != null)
    {
      filesScanned = cd.sourceStatistics.filesScanned;
      filesRead    = cd.sourceStatistics.contentFilesRead + cd.sourceStatistics.metadataFilesRead + cd.sourceStatistics.contentVersionFilesRead + cd.sourceStatistics.metadataVersionFilesRead;
      nodesCreated = cd.targetStatistics.contentNodesCreated;

      if (pd != null)
      {
        filesScanned = Math.max(0, filesScanned - pd.sourceStatistics.filesScanned);
        filesRead    = Math.max(0, filesRead    - (pd.sourceStatistics.contentFilesRead + pd.sourceStatistics.metadataFilesRead + pd.sourceStatistics.contentVersionFilesRead + pd.sourceStatistics.metadataVersionFilesRead));
        nodesCreated = Math.max(0, nodesCreated - pd.targetStatistics.contentNodesCreated);
      }
    }
    else
    {
      log.debug('No status data available for files per second chart.');
    }

    fileScannedTimeSeries.append( now, filesScanned);
    filesReadTimeSeries.append(   now, filesRead);
    nodesCreatedTimeSeries.append(now, nodesCreated);
    filesZeroTimeSeries.append(   now, 0); // Used to keep a fixed baseline - I don't like how smoothie has a variable baseline
  }, 1000);  // Update every second

  // Add the time series' to the chart
  filesPerSecondChart.addTimeSeries(fileScannedTimeSeries,  { strokeStyle : 'rgb(255, 0, 0)',   fillStyle : 'rgba(255, 0, 0, 0.0)', lineWidth : 3 } );
  filesPerSecondChart.addTimeSeries(filesReadTimeSeries,    { strokeStyle : 'rgb(0, 255, 0)',   fillStyle : 'rgba(0, 255, 0, 0.0)', lineWidth : 3 } );
  filesPerSecondChart.addTimeSeries(nodesCreatedTimeSeries, { strokeStyle : 'rgb(0, 0, 255)',   fillStyle : 'rgba(0, 0, 255, 0.0)', lineWidth : 3 } );
  filesPerSecondChart.addTimeSeries(filesZeroTimeSeries,    { strokeStyle : 'rgba(0, 0, 0, 0)', fillStyle : 'rgba(0, 0, 0, 0.0)',   lineWidth : 0 } );
}


/*
 * Start the "bytes per second" chart
 */
function startBytesPerSecondChart(canvasElement)
{
  log.debug('Starting bytes per second chart...');

  // Initialise the bytes per second chart
  bytesPerSecondChart = new SmoothieChart({
    grid: { strokeStyle      : 'rgb(127, 127, 127)',
            fillStyle        : 'rgb(0, 0, 0)',
            lineWidth        : 1,
            millisPerLine    : 500,
            verticalSections : 10 },
    labels: { fillStyle : 'rgb(255, 255, 255)' }
  });

  bytesPerSecondChart.streamTo(canvasElement, 1000);  // 1 second delay in rendering (for extra smoothiness!)

  // Data
  var bytesReadTimeSeries    = new TimeSeries();
  var bytesWrittenTimeSeries = new TimeSeries();
  var bytesZeroTimeSeries    = new TimeSeries();

  // Update the graph every second
  bytesPerSecondChartTimer = setInterval(function()
  {
    log.debug('Updating bytes per second chart...');

    var now          = new Date().getTime();
    var pd           = previousData;
    var cd           = currentData;
    var bytesRead    = 0;
    var bytesWritten = 0;

    if (cd != null)
    {
      bytesRead    = cd.sourceStatistics.contentBytesRead + cd.sourceStatistics.contentVersionBytesRead;
      bytesWritten = cd.targetStatistics.contentBytesWritten + cd.targetStatistics.contentVersionsBytesWritten;

      if (pd != null)
      {
        bytesRead    = Math.max(0, bytesRead    - (pd.sourceStatistics.contentBytesRead + pd.sourceStatistics.contentVersionBytesRead));
        bytesWritten = Math.max(0, bytesWritten - (pd.targetStatistics.contentBytesWritten + pd.targetStatistics.contentVersionsBytesWritten));
      }
    }
    else
    {
      log.debug('No status data available for bytes per second chart.');
    }

    bytesReadTimeSeries.append(   now, bytesRead);
    bytesWrittenTimeSeries.append(now, bytesWritten);
    bytesZeroTimeSeries.append(   now, 0); // Used to keep a fixed baseline - I don't like how smoothie has a variable baseline
  }, 1000);  // Update every second

  // Add the time series' to the chart
  bytesPerSecondChart.addTimeSeries(bytesReadTimeSeries,    { strokeStyle : 'rgb(0, 255, 0)',   fillStyle : 'rgba(0, 255, 0, 0.0)', lineWidth : 3 } );
  bytesPerSecondChart.addTimeSeries(bytesWrittenTimeSeries, { strokeStyle : 'rgb(0, 0, 255)',   fillStyle : 'rgba(0, 0, 255, 0.0)', lineWidth : 3 } );
  bytesPerSecondChart.addTimeSeries(bytesZeroTimeSeries,    { strokeStyle : 'rgba(0, 0, 0, 0)', fillStyle : 'rgba(0, 0, 0, 0.0)',   lineWidth : 0 } );
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


function stopImport(stopURI)
{
  var stopImportButton = document.getElementById("stopImportButton");

  stopImportButton.innerHTML = "Stop requested...";
  stopImportButton.disabled  = true;
  
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
