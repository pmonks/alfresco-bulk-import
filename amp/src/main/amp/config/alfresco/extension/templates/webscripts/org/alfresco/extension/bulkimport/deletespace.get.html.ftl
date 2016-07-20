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
    <title>Bulk Import Tool - Delete Space</title>
    <meta name="description" content="Bulk Import Tool - Delete Space">
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
        <h1><strong>Bulk Import Tool - Delete Space</strong></h1>
      </div>
    </div>

    <form id="initiateDeleteForm" action="${url.service}" method="post" charset="utf-8">
      <fieldset><legend>Deletion Target</legend>
        <p><label for="targetPath">Space to delete:</label> <input type="text" id="targetPath" name="targetPath" size="80" required/></p>
      </fieldset>

      <p><strong style="color:red">Warning! This operation will delete the space and <u>all</u> contents unconditionally, without archiving or an audit record!  It cannot be interrupted or undone!</strong></p>
      <p><button class="button red" type="submit" name="submit">&#9888; Delete! &#9888;</button></p>
      <p>Note: this operation can take a very long time (minutes to hours), and does not provide any status reporting.</p>
    </form>

    <p>Please see the <a target="_blank" href="https://github.com/pmonks/alfresco-bulk-import">project site</a> for documentation, known issues, updated versions, etc.</p>
    <hr/>
    <p class="footnote">Bulk Import Tool v2.1.1-SNAPSHOT, Alfresco ${server.edition} v${server.version}</p>
    <script>
      [#-- Target field autocomplete --]
      $(function() {
        $('#targetPath').autocomplete({
          source: '${url.context}/s/bulk/import/ajax/suggest/spaces.json',
          minLength: 2
        });
      });

      [#-- Form submission behaviour --]
      $("#initiateDeleteForm").submit(function(event) {
        var submitButton = $(this).find("button[type='submit']");

        submitButton.prop('disabled', true);
        submitButton.text('Deleting...');
        submitButton.switchClass('red', 'gray');
      });

      $(document).ready(function() {
        favicon.change('${url.context}/images/bulkimport/logo.png');
      });
    </script>
  </body>
</html>
