[#ftl]
<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js"> <!--<![endif]-->
<html>
  <head>
    <meta charset="utf-8">
    <link href='//fonts.googleapis.com/css?family=Open+Sans:400italic,600italic,400,600' rel='stylesheet' type='text/css'>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Bulk Import Tool</title>
    <meta name="description" content="UI Web Script for the Bulk Import Tool">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    [#-- favicons - good lord!  o.O --]
    <link rel="shortcut icon" href="${url.context}/images/bulkimport/favicon.ico" type="image/x-icon" />
    <link rel="apple-touch-icon" href="${url.context}/images/bulkimport/apple-touch-icon.png" />
    <link rel="apple-touch-icon" sizes="57x57" href="${url.context}/images/bulkimport/apple-touch-icon-57x57.png" />
    <link rel="apple-touch-icon" sizes="72x72" href="${url.context}/images/bulkimport/apple-touch-icon-72x72.png" />
    <link rel="apple-touch-icon" sizes="76x76" href="${url.context}/images/bulkimport/apple-touch-icon-76x76.png" />
    <link rel="apple-touch-icon" sizes="114x114" href="${url.context}/images/bulkimport/apple-touch-icon-114x114.png" />
    <link rel="apple-touch-icon" sizes="120x120" href="${url.context}/images/bulkimport/apple-touch-icon-120x120.png" />
    <link rel="apple-touch-icon" sizes="144x144" href="${url.context}/images/bulkimport/apple-touch-icon-144x144.png" />
    <link rel="apple-touch-icon" sizes="152x152" href="${url.context}/images/bulkimport/apple-touch-icon-152x152.png" />
    [#-- JQuery --]
    <link rel="stylesheet" href="//code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css">
    <script src="//code.jquery.com/jquery-1.9.1.js"></script>
    <script src="//code.jquery.com/ui/1.10.3/jquery-ui.js"></script>
    [#-- Bulk import --]
    <script src="${url.context}/scripts/bulkimport/vendor/modernizr-2.6.2.min.js"></script>
    <link rel="stylesheet" href="${url.context}/css/bulkimport/normalize.css">
    <link rel="stylesheet" href="${url.context}/css/bulkimport/main.css">
    <link rel="stylesheet" href="${url.context}/css/bulkimport/bulkimport.css">
  </head>
  <body>
    <!--[if lt IE 7]>
        <p class="browsehappy">You are using an <strong>outdated</strong> browser. Please <a href="http://browsehappy.com/">upgrade your browser</a> to improve your experience.</p>
    <![endif]-->
    <div class="container">
      <div class="block">
        <img style="margin:15px;vertical-align:middle" src="${url.context}/images/bulkimport/apple-touch-icon-57x57.png" alt="Alfresco Bulk Import Tool" />
      </div>
      <div class="block">
        <h1><strong>Bulk Import Tool v2.0-SNAPSHOT</strong></h1>
      </div>
    </div>

[#--    <form action="${url.service}/initiate" method="post" enctype="multipart/form-data" charset="utf-8"> --]
    <form id="initiateBulkImportForm" action="${url.service}/initiate" method="post" charset="utf-8">
      <fieldset><legend>Source Settings</legend>
        <p><label for="sourceBeanId">Source:</label><select id="sourceBeanId" name="sourceBeanId" required[#if sources?size <= 1] disabled[/#if]>
[#list sources as source]
  [#if source.name = "Filesystem"]
          <option value="${source.beanId}" selected>${source.name}</option>
  [#else]
          <option value="${source.beanId}">${source.name}</option>
  [/#if]
[/#list]
        </select></p>
        
        <div id="customConfigSection"></div>
      </fieldset>
      <p></p>
      <fieldset><legend>Target Settings</legend>
        <p><label for="targetPath">Target space:</label> <input type="text" id="targetPath" name="targetPath" size="80" required/></p>
        <p><label for="replaceExisting">Replace:</label> <input type="checkbox" id="replaceExisting" name="replaceExisting" value="true" unchecked/> checked means files that already exist in the repository will be updated or replaced, depending on whether they're versioned or not</p>
        <p><label for="dryRun">Dry run:</label> <input type="checkbox" id="dryRun" name="dryRun" value="true" unchecked/> checked means run through the process without writing to the repository</p>
      </fieldset>
      
      <p><button class="button green" type="submit" name="submit">Initiate Bulk Import</button></p>
    </form>
    <p>Please see the <a target="_blank" href="https://github.com/pmonks/alfresco-bulk-import">project site</a> for documentation, known issues, updated versions, etc.</p>
    <hr>
    <p class="footnote">Alfresco ${server.edition} v${server.version}</p>
    <script>
      [#-- Re-enable the sourceBeanId field prior to submission, to workaround the stupid behaviour of "<select disabled>" --]
      $('#initiateBulkImportForm').on('submit', function() {
        $('#sourceBeanId').attr('disabled', false);
      });

      [#-- List of bulk import sources as an array --]
      var bulkImportSources = [
[#if sources??]
  [#list sources as source]
        {
          'beanId'             : '${source.beanId}',
[#--           'name'               : '${source.name}',    Unused in this array --]
    [#if source.configWebScriptURI??]
      [#if source.configWebScriptURI?starts_with("/")]
          'configWebScriptURI' : '${url.serviceContext}${source.configWebScriptURI}'
      [#else]
          'configWebScriptURI' : '${url.serviceContext}/${source.configWebScriptURI}'
      [/#if]
    [#else]
          'configWebScriptURI' : null
    [/#if]
        }[#if source.beanId != sources?last.beanId],[/#if]
  [/#list]
[/#if]
      ];
      
      [#-- Retrieve the config web script URI for the given bean Id, or null if the beanId couldn't be found in the bulkImportSources array --]
      function getConfigWebScriptURI(beanId) {
        var result = null;

        for (var i = 0; i < bulkImportSources.length; i++) {
          if (beanId === bulkImportSources[i].beanId) {
            result = bulkImportSources[i].configWebScriptURI;
            break;
          }
        }

        return(result);
      }
      
      [#-- Load the custom config panel for the given beanId --]
      function loadCustomConfigPanel(beanId) {
        var configWebScriptURI = getConfigWebScriptURI(beanId);
        
        $('#customConfigSection').html('');
        
        if (configWebScriptURI) {
          $.get(configWebScriptURI, function(data) {
            $('#customConfigSection').html(data);
          })
        }
      }

      [#-- Source field onChange --]
      $('#sourceBeanId').change(function() {
        loadCustomConfigPanel($(this).val());
      });

      [#-- Target field autocomplete --]
      $(function() {
        $('#targetPath').autocomplete({
          source: '${url.service}/ajax/suggest/spaces.json',
          minLength: 2
        });
      });
      
      [#-- Load the default custom config panel on document ready --]
      $(document).ready(function() {
        loadCustomConfigPanel($('#sourceBeanId').val());
      });
    </script>
  </body>
</html>
