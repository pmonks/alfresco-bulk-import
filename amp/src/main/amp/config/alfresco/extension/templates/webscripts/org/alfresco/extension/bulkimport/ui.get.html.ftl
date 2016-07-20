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
    <meta name="description" content="Bulk Import Tool">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    [#-- 3rd Party Stuff --]
    <link rel="stylesheet" href="//code.jquery.com/ui/1.12.0/themes/smoothness/jquery-ui.css">
    <script src="//code.jquery.com/jquery-2.2.4.js"></script>
    <script src="//code.jquery.com/ui/1.12.0/jquery-ui.js"></script>
    <script src="//cdnjs.cloudflare.com/ajax/libs/loglevel/1.4.0/loglevel.min.js"></script>
    <script src="${url.context}/scripts/bulkimport/modernizr-3.3.1.min.js"></script>
    <script src="${url.context}/scripts/bulkimport/favicon.min.js"></script>
    [#-- Bulk import --]
    <link rel="stylesheet" href="${url.context}/css/bulkimport/normalize.css">
    <link rel="stylesheet" href="${url.context}/css/bulkimport/main.css">
    <link rel="stylesheet" href="${url.context}/css/bulkimport/bulkimport.css">
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
        <h1><strong>Bulk Import Tool</strong></h1>
      </div>
    </div>

    <form id="initiateBulkImportForm" action="${url.service}/initiate" method="post" charset="utf-8">
      <fieldset><legend>Source Settings</legend>
        <p><label for="sourceBeanId">Source:</label><select id="sourceBeanId" name="sourceBeanId" required[#if sources?size <= 1] disabled[/#if]>
[#list sources as source]
  [#if source.name = "Default"]
          <option value="${source.beanId}" selected>${source.name}</option>
  [#else]
          <option value="${source.beanId}">${source.name}</option>
  [/#if]
[/#list]
        </select>&nbsp;&nbsp;&nbsp;&nbsp;<span id="sourceDescription"></span></p>
        <div id="customConfigSection"></div>
      </fieldset>
      <p></p>
      <fieldset><legend>Target Settings</legend>
        <p><label for="targetPath">Target space:</label> <input type="text" id="targetPath" name="targetPath" size="80" required/></p>
        <p><label for="replaceExisting">Replace:</label> <input type="checkbox" id="replaceExisting" name="replaceExisting" value="true" unchecked/> checked means files that already exist in the repository will be updated or replaced, depending on whether they're versioned or not</p>
        <p><label for="dryRun">Dry run:</label> <input type="checkbox" id="dryRun" name="dryRun" value="true" unchecked/> checked means run through the process without writing to the repository</p>
      </fieldset>

      <p><button class="button green" type="submit" name="submit">&#9658; Initiate Bulk Import</button></p>
    </form>
    <p>Please see the <a target="_blank" href="https://github.com/pmonks/alfresco-bulk-import">project site</a> for documentation, known issues, updated versions, etc.</p>
    <hr/>
    <p class="footnote">Bulk Import Tool v2.1.1-SNAPSHOT, Alfresco ${server.edition} v${server.version}</p>
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
          'beanId'             : '${source.beanId?js_string}',
          'name'               : '${source.name?js_string}',
          'description'        : '${source.description?js_string}',
    [#if source.configWebScriptURI??]
      [#if source.configWebScriptURI?starts_with("/")]
          'configWebScriptURI' : '${url.serviceContext}${source.configWebScriptURI?js_string}'
      [#else]
          'configWebScriptURI' : '${url.serviceContext}/${source.configWebScriptURI?js_string}'
      [/#if]
    [#else]
          'configWebScriptURI' : null
    [/#if]
        }[#if source.beanId != sources?last.beanId],[/#if]
  [/#list]
[/#if]
      ];

      [#-- Retrieve the description for the given bean Id, or null if the beanId couldn't be found in the bulkImportSources array --]
      function getDescription(beanId) {
        var result = null;

        for (var i = 0; i < bulkImportSources.length; i++) {
          if (beanId === bulkImportSources[i].beanId) {
            result = bulkImportSources[i].description;
            break;
          }
        }

        return(result);
      }

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

      [#-- Set the description text for the given beanId --]
      function setDescription(beanId) {
        var description = getDescription(beanId);

        if (description) {
          $('#sourceDescription').html(description);
        }
        else
        {
          $('#sourceDescription').html('');
        }
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
        var beanId = $(this).val();
        setDescription(beanId);
        loadCustomConfigPanel(beanId);
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
        favicon.change('${url.context}/images/bulkimport/logo.png');

        var beanId = $('#sourceBeanId').val();
        setDescription(beanId);
        loadCustomConfigPanel(beanId);
      });
    </script>
  </body>
</html>
