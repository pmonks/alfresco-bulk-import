/*
 * Copyright (C) 2012 Peter Monks
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

var logLevel = log.levels.DEBUG;  // See http://pimterry.github.io/loglevel/ for details

// Global variables
var statusURI;
var pauseURI;
var resumeURI;
var stopURI;
var webAppContext;
var previousData;
var currentData;
var nodesPerSecondChart;
var nodesPerSecondChartTimer;
var bytesPerSecondChart;
var bytesPerSecondChartTimer;
var importStatusTimer;
var refreshTextTimer;

log.setLevel(logLevel);

/*
 * Initialise the status UI
 */
function initStatus(alfrescoWebAppContext, alfrescoWebScriptContext)
{
  statusURI     = alfrescoWebScriptContext + "/bulk/import/status.json";
  pauseURI      = alfrescoWebScriptContext + "/bulk/import/pause";
  resumeURI     = alfrescoWebScriptContext + "/bulk/import/resume";
  stopURI       = alfrescoWebScriptContext + "/bulk/import/stop";
  webAppContext = alfrescoWebAppContext;

  currentData = getStatusInfo();  // Pull down an initial set of status info

  if (currentData != null && currentData.inProgress === false)
  {
    // If the import completed before the page even loaded, update the text area then bomb out
    favicon.change(webAppContext + "/images/bulkimport/logo.png");
    refreshTextElements(currentDate);
  }
  else
  {
    log.debug('Import in progress, starting UI.');
    startSpinner();
    startImportStatusTimer();
    startRefreshTextTimer();
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

  $.getJSON(statusURI, function(data)
  {
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
        document.getElementById("currentStatus").textContent = currentData.processingState;
        document.getElementById("currentStatus").style.color = stateToColour(currentData.processingState);
        
        // If we're idle, stop the world
        if (currentData.inProgress === false)
        {
          log.debug('Import complete, shutting down UI.');

          // Kill all the spinners, charts and timers
          favicon.stopAnimate();
          favicon.change(webAppContext + "/images/bulkimport/logo.png");

          if (nodesPerSecondChart      != null) nodesPerSecondChart.stop();
          if (nodesPerSecondChartTimer != null) { clearInterval(nodesPerSecondChartTimer); nodesPerSecondChartTimer = null; }
          if (bytesPerSecondChart      != null) bytesPerSecondChart.stop();
          if (bytesPerSecondChartTimer != null) { clearInterval(bytesPerSecondChartTimer); bytesPerSecondChartTimer = null; }
          if (importStatusTimer        != null) { clearInterval(importStatusTimer);        importStatusTimer        = null; }
          if (refreshTextTimer         != null) { clearInterval(refreshTextTimer);         refreshTextTimer         = null; }

          // Update the text one last time
          refreshTextElements(currentData);
          
          // Update the status
          document.getElementById("estimatedDuration").textContent = "";
          document.getElementById("detailsCurrentlyImporting").textContent = "";

          // Hide buttons and show initiate another import link
          hideElement(document.getElementById("pauseImportButton"));
          hideElement(document.getElementById("resumeImportButton"));
          hideElement(document.getElementById("stopImportButton"));
          showElement(document.getElementById("initiateAnotherImport", false));
        }
        else  // We're not idle, so update stuff
        {
          // Check if we've just been paused or resumed
          if (currentData.paused != previousData.paused)
          {
            if (currentData.paused)
            {
              favicon.stopAnimate();
              nodesPerSecondChart.stop();
              bytesPerSecondChart.stop();
            }
            else
            {
              startSpinner();
              nodesPerSecondChart.start();
              bytesPerSecondChart.start();
            }
          }

          if (currentData.estimatedRemainingDuration !== undefined)
          {
            document.getElementById("estimatedDuration").textContent = ", estimated completion in " + currentData.estimatedRemainingDuration + ".";
          }
          else
          {
            document.getElementById("estimatedDuration").textContent = ", estimated completion in <unknown>.";
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

  importStatusTimer = setInterval(getImportStatus, 1000)
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

  refreshTextTimer = setInterval(refreshText, 2000)
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
      movingAverage = cd.targetCounters["Nodes imported"].Rate;

      if (pd != null)
      {
        var previousNodesImported = pd.targetCounters["Nodes imported"].Count;
        var currentNodesImported  = cd.targetCounters["Nodes imported"].Count;

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
      movingAverage = cd.targetCounters["Bytes imported"].Rate;

      if (pd != null)
      {
        var previousBytesImported = pd.targetCounters["Bytes imported"].Count;
        var currentBytesImported  = cd.targetCounters["Bytes imported"].Count;

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
    document.getElementById("detailsStatus").textContent = cd.processingState;
    document.getElementById("detailsStatus").style.color = stateToColour(cd.processingState);

    // Queue & threads
    if (cd.queuedBatches === undefined)
    {
      document.getElementById("detailsQueueSize").textContent = "0";
    }
    else
    {
      document.getElementById("detailsQueueSize").textContent = cd.queuedBatches;
    }

    if (cd.numberOfActiveThreads === undefined)
    {
      document.getElementById("detailsActiveThreads").textContent = "0";
    }
    else
    {
      document.getElementById("detailsActiveThreads").textContent = cd.numberOfActiveThreads;
    }

    if (cd.totalNumberOfThreads === undefined)
    {
      document.getElementById("detailsTotalThreads").textContent = "0";
    }
    else
    {
      document.getElementById("detailsTotalThreads").textContent = cd.totalNumberOfThreads;
    }

    // Scan End date
    if (cd.scanEndDate) document.getElementById("detailsScanEndDate").textContent = cd.scanEndDate;

    // End date
    if (cd.endDate) document.getElementById("detailsEndDate").textContent = cd.endDate;

    // Scan Duration
    if (cd.scanDuration) document.getElementById("detailsScanDuration").textContent = cd.scanDuration;

    // Duration
    if (cd.duration) document.getElementById("detailsDuration").textContent = cd.duration;
    
    // Currently importing
    if (cd.currentlyImporting) document.getElementById("detailsCurrentlyImporting").textContent = cd.currentlyImporting;

    // Counters
    if (cd.sourceCounters) updateTableBody("sourceCounterTableBody", cd.sourceCounters);
    if (cd.targetCounters) updateTableBody("targetCounterTableBody", cd.targetCounters);

    // Last exception
    if (cd.lastException)
    {
      document.getElementById("detailsErrorInformation").style.display = "block";
      document.getElementById("detailsLastException").textContent      = cd.lastException;
    }
  }
}


function updateTableBody(tableBodyId, counterData)
{
  var oldTableBody = document.getElementById(tableBodyId);
  var newTableBody = document.createElement("tbody");

  newTableBody.setAttribute("id", tableBodyId);

  for (var counter in counterData)
  {
    if (counterData.hasOwnProperty(counter))
    {
      var counterRow       = newTableBody.insertRow();
      var counterNameCell  = counterRow.insertCell();
      var counterValueCell = counterRow.insertCell();
      var counterRateCell  = counterRow.insertCell();
      var count            = counterData[counter].Count;
      var rate             = counterData[counter].Rate;

      counterNameCell.textContent  = counter + ":";
      counterNameCell.setAttribute("width", "25%");
      counterValueCell.textContent = count;
      counterValueCell.setAttribute("width", "35%");
      counterRateCell.textContent = roundToDigits(rate, 3) + " / sec";
      counterRateCell.setAttribute("width", "40%");
    }
  }

  oldTableBody.parentNode.replaceChild(newTableBody, oldTableBody);
}


function pauseImport()
{
  var pauseImportButton  = $("#pauseImportButton")[0];
  var resumeImportButton = $("#resumeImportButton")[0];

  hideElement(pauseImportButton);
  showElement(resumeImportButton, true);

  $.post(pauseURI).fail(function() { log.error("Error when calling " + pauseURI + "."); });
}


function resumeImport()
{
  var pauseImportButton  = $("#pauseImportButton")[0];
  var resumeImportButton = $("#resumeImportButton")[0];

  showElement(pauseImportButton, true);
  hideElement(resumeImportButton);

  $.post(resumeURI).fail(function() { log.error("Error when calling " + resumeURI + "."); });
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


function roundToDigits(number, numberOfDigits)
{
  var multiplicationFactor = Math.pow(10, numberOfDigits);

  return(Math.round(number * multiplicationFactor) / multiplicationFactor);
}


function hideElement(element)
{
  element.style.display = "none";
}


function showElement(element, inline)
{
  if (inline)
  {
    element.style.display = "inline";
  }
  else
  {
    element.style.display = "inline-block";
  }
}
