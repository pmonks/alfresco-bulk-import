[#ftl]
[#macro formatModelClass modelClass metatype]
        <h2>${metatype}: ${modelClass.name.prefixString}</h2>
  [#if modelClass.parentName??]
        <p>Parent: ${modelClass.parentName.prefixString}</p>
  [/#if]

  [#if modelClass.properties?size > 0]
      <table border="1" cellspacing="0" cellpadding="1">
        <thead>
          <tr><th>Property Name</th><th>Data Type</th><th>Multi-valued?</th><th>Mandatory?</th><th>Constraints</th></tr>
        </thead>
        <tbody>
    [#list modelClass.properties?values?sort_by(["name", "prefixString"]) as property]
      [@compress single_line=true]
          <tr>
            <td>${property.name.prefixString}</td>
            <td>${property.dataType.name.prefixString}</td>
            <td>${property.isMultiValued()?string}</td>
            <td>${property.isMandatoryEnforced()?string}</td>
            <td>
        [#if property.constraints?size > 0]
          [#list property.constraints as propertyConstraint]
            [#if propertyConstraint != property.constraints?first], [/#if]
              ${propertyConstraint.name.prefixString}
          [/#list]
        [/#if]
            </td>
          </tr>
      [/@compress]
    [/#list]
        </tbody>
      </table>
      <br/>
  [/#if]

  [#if modelClass.associations?size > 0]
      <table border="1" cellspacing="0" cellpadding="1">
        <thead>
          <tr><th>Association Name</th><th>Association Type</th><th>Source Type</th><th>Destination Type</th></tr>
        </thead>
        <tbody>
    [#list modelClass.associations?values?sort_by(["name", "prefixString"]) as association]
      [@compress single_line=true]
          <tr>
            <td>${association.name.prefixString}</td>
            <td>
        [#if association.isChild()]
              parent / child
        [#else]
              peer
        [/#if]
            </td>
            <td>${association.sourceClass.name.prefixString}</td>
            <td>${association.targetClass.name.prefixString}</td>
          </tr>
      [/@compress]
    [/#list]
        </tbody>
      </table>
  [/#if]
[/#macro]
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
    <title>Bulk Import Tool - Data Dictionary</title>
    <meta name="description" content="Bulk Import Tool - Data Dictionary">
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
        <h1><strong>Bulk Import Tool - Data Dictionary</strong></h1>
      </div>
    </div>

[#list dataDictionary?sort_by(["model", "name", "prefixString"]) as model]
    <hr/>
    <h1>Model: ${model.model.name.prefixString}</h1>

  [#if model.model.namespaces?size > 0]
    <h2>Namespaces</h2>
    <p>
    [#list model.model.namespaces as namespace]
      ${namespace.prefix} = ${namespace.uri}<br/>
    [/#list]
    </p>
  [/#if]

  [#if model.constraints?size > 0]
    <h2>Constraints</h2>
    <p>
    [#list model.constraints?sort_by(["name", "prefixString"]) as constraint]
      ${constraint.name.prefixString}<br/>
    [/#list]
    </p>
  [/#if]

  [#if model.types?size > 0]
    [#list model.types?sort_by(["name", "prefixString"]) as type]
      [@formatModelClass type "Type" /]
    [/#list]
  [/#if]

  [#if model.aspects?size > 0]
    [#list model.aspects?sort_by(["name", "prefixString"]) as aspect]
      [@formatModelClass aspect "Aspect" /]
    [/#list]
  [/#if]
[/#list]

    <p>Please see the <a target="_blank" href="https://github.com/pmonks/alfresco-bulk-import">project site</a> for documentation, known issues, updated versions, etc.</p>
    <hr/>
    <p class="footnote">Bulk Import Tool v2.1.1-SNAPSHOT, Alfresco ${server.edition} v${server.version}</p>
  </body>
</html>
