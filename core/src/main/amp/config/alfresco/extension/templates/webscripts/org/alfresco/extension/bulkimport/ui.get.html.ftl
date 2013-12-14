[#ftl]
<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js"> <!--<![endif]-->
<html>
  <head>
    <meta charset="utf-8">
    <link href='http://fonts.googleapis.com/css?family=Open+Sans:400italic,600italic,400,600' rel='stylesheet' type='text/css'>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Bulk Import Tool</title>
    <meta name="description" content="UI Web Script for the Bulk Import Tool">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="shortcut icon" href="${url.context}/images/bulkimport/favicon.ico" type="image/x-icon" />
    <link rel="apple-touch-icon" href="${url.context}/images/bulkimport/apple-touch-icon.png" />
    <link rel="apple-touch-icon" sizes="57x57" href="${url.context}/images/bulkimport/apple-touch-icon-57x57.png" />
    <link rel="apple-touch-icon" sizes="72x72" href="${url.context}/images/bulkimport/apple-touch-icon-72x72.png" />
    <link rel="apple-touch-icon" sizes="76x76" href="${url.context}/images/bulkimport/apple-touch-icon-76x76.png" />
    <link rel="apple-touch-icon" sizes="114x114" href="${url.context}/images/bulkimport/apple-touch-icon-114x114.png" />
    <link rel="apple-touch-icon" sizes="120x120" href="${url.context}/images/bulkimport/apple-touch-icon-120x120.png" />
    <link rel="apple-touch-icon" sizes="144x144" href="${url.context}/images/bulkimport/apple-touch-icon-144x144.png" />
    <link rel="apple-touch-icon" sizes="152x152" href="${url.context}/images/bulkimport/apple-touch-icon-152x152.png" />
    <link rel="stylesheet" href="${url.context}/css/bulkimport/normalize.css">
    <link rel="stylesheet" href="${url.context}/css/bulkimport/main.css">
    <link rel="stylesheet" href="${url.context}/css/bulkimport/bulkimport.css">
    <script src="${url.context}/scripts/bulkimport/vendor/modernizr-2.6.2.min.js"></script>

    <!-- YUI 3.x -->
    <script src="http://yui.yahooapis.com/3.8.0/build/simpleyui/simpleyui-min.js"></script>
    <style type="text/css">
      .yui3-aclist-content {
        background-color   : white;
        border             : 1px solid darkgrey;
        box-shadow         : 3px 3px 4px lightgrey;
        -webkit-box-shadow : 3px 3px 4px lightgrey; /* Safari and Chrome */
       }
    </style>
    <script>
      var bulkImportSources = [
[#if sources??]
  [#list sources as source]
        {
          'name'               : '${source.name}',
          'beanId'             : '${source.beanId}',
          'configWebScriptURI' : '${source.configWebScriptURI}'
        }[#if source != sources?last],[/#if]
  [/#list]
[/#if]
      ];
      
      //####TODO: Add function for pulling down config HTML when the source type changes...
    </script>
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

    <p>Please see the <a target="_blank" href="https://github.com/pmonks/alfresco-bulk-import">project site</a> for documentation, known issues, updated versions, etc.</p>
    <form action="${url.service}/initiate" method="get" enctype="multipart/form-data" charset="utf-8">
      <fieldset><legend>Source Settings</legend>
        <p><label for="sourceBeanId">Source:</label><select id="sourceBeanId" required>
[#if sources??]
  [#list sources as source]
    [#if source.name = "Filesystem"]
          <option value="${source.beanId}" selected>${source.name}</option>
    [#else]
          <option value="${source.beanId}">${source.name}</option>
    [/#if]
  [/#list]
[/#if]
        </select></p>

        ####TODO: LOAD FILESYSTEM SOURCE'S CONFIG WEB SCRIPT VIA AJAX BY DEFAULT HERE!!!!
      </fieldset>
      <p></p>
      <fieldset><legend>Target Settings</legend>
        <p><label for="targetPath">Target space:</label> <div id="targetNodeRefAutoComplete"><input id="targetPath" type="text" name="targetPath" size="80" required/><div id="targetPathAutoSuggestContainer"></div><div id="targetPathMessage" style="color:red"></div></p>
        <p><label for="replaceExisting">Replace:</label> <input type="checkbox" id="replaceExisting" name="replaceExisting" value="true" unchecked/> (checked means files that already exist in the repository will be updated, false means skip them)</p>
        <p><label for="dryRun">Dry run:</label> <input type="checkbox" id="dryRun" name="dryRun" value="true" unchecked/> (checked means run through the process without writing to the repository)</p>
      </fieldset>
      
      <p><button type="submit" name="submit">Initiate Bulk Import</button></p>
    </form>
    
    <script>
      YUI().use('autocomplete', 'autocomplete-highlighters', function (Y)
      {
        Y.one('#targetPath').plug(Y.Plugin.AutoComplete,
        {
          maxResults        : 25,
          enableCache       : false,
          resultHighlighter : 'phraseMatch',
          resultListLocator : 'data',
          resultTextLocator : 'path',
          source            : '${url.service}/ajax/suggest/spaces.json?query={query}'
        });
      });
    </script>
    
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
    <script>window.jQuery || document.write('<script src="scripts/bulkimport/vendor/jquery-1.10.2.min.js"><\/script>')</script>
    <script src="scripts/bulkimport/plugins.js"></script>
    <script src="scripts/bulkimport/main.js"></script>
    <hr>
    <p>Alfresco ${server.edition} v${server.version}</p>
  </body>
</html>
