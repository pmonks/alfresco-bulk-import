[#ftl]
<!DOCTYPE HTML>
<html>
  <head>
    <title>Bulk Import Tool</title>
    <link rel="stylesheet" href="${url.context}/css/main.css" TYPE="text/css">

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
    <!-- Validation functions -->
    <script>
      function validateRequired(field, errorMessageElement, errorMessage)
      {
        var result = true;

        if (field.value == null || field.value == "")
        {
          errorMessageElement.textContent = errorMessage;
          result = false;
        }
        else
        {
          errorMessageElement.textContent = "";
        }

        return result;
      }


      function validateForm(form)
      {
        var result = true;

        result = validateRequired(form.sourceDirectory, document.getElementById("sourceDirectoryMessage"), "Source directory is mandatory.");

        if (result)
        {
          result = validateRequired(form.targetPath, document.getElementById("targetPathMessage"), "Target space is mandatory.");
        }

        return result;
      }
    </script>
  </head>
  <body class="yui-skin-sam">
    <table cellpadding="5">
      <tr>
        <td><img src="${url.context}/images/logo/AlfrescoLogo32.png" alt="Alfresco" /></td>
        <td><strong>Bulk Import Tool v2.0-SNAPSHOT</strong><br/>
            Alfresco ${server.edition} v${server.version}</td>
      </tr>
    </table>
    <p>Please see the <a target="_blank" href="http://code.google.com/p/alfresco-bulk-filesystem-import/">project site</a> for documentation, known issues, updated versions, etc.</p>
    <form action="${url.service}/initiate" method="get" enctype="multipart/form-data" charset="utf-8" onsubmit="return validateForm(this);">
      <table>
        <tr>
          <td>Import directory:</td><td><input type="text" name="sourceDirectory" size="128" /></td><td id="sourceDirectoryMessage" style="color:red"></td>
        </tr>
        <tr>
          <td><br/><label for="targetPath">Target space:</label></td>
          <td>
            <div id="targetNodeRefAutoComplete">
              <input id="targetPath" type="text" name="targetPath" size="128" />
              <div id="targetPathAutoSuggestContainer"></div>
            </div>
          </td>
          <td id="targetPathMessage" style="color:red"></td>
        </tr>
        <tr>
          <td colspan="3">&nbsp;</td>
        </tr>
        <tr>
          <td><label for="replaceExisting">Replace existing files:</label></td><td><input type="checkbox" id="replaceExisting" name="replaceExisting" value="replaceExisting" unchecked/> (unchecked means skip files that already exist in the repository)</td><td></td>
        </tr>
        <tr>
          <td colspan="3">&nbsp;</td>
        </tr>
        <tr>
          <td colspan="3"><input type="submit" name="submit" value="Initiate Bulk Import"></td>
        </tr>
      </table>
      <br/>
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
    
  </body>
</html>
