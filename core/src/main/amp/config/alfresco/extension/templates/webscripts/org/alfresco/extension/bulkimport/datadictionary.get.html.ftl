[#ftl]
[#macro formatModelClass modelClass metatype]
        <h3>${metatype}: ${modelClass.name.prefixString}</h3>
  [#if modelClass.parentName??]
        <p>Parent: ${modelClass.parentName.prefixString}</p>
  [/#if]

  [#if modelClass.properties?size > 0]
      <table border="1" cellspacing="0" cellpadding="1">
        <tr><th>Property Name</th><th>Data Type</th><th>Multi-valued?</th><th>Mandatory?</th><th>Constraints</th></tr>
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
      </table>
      <br/>
  [/#if]

  [#if modelClass.associations?size > 0]
      <table border="1" cellspacing="0" cellpadding="1">
        <tr><th>Association Name</th><th>Association Type</th><th>Source Type</th><th>Destination Type</th></tr>
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
      </table>
  [/#if]
[/#macro]

<!DOCTYPE HTML>
<html>
<head>
  <title>Bulk Import Tool - Data Dictionary</title>
  <link rel="stylesheet" href="${url.context}/css/main.css" type="text/css"/>
</head>
<body>
  <table>
    <tr>
      <td><img src="${url.context}/images/logo/AlfrescoLogo32.png" alt="Alfresco" /></td>
      <td><nobr>Bulk Import Tool - Data Dictionary</nobr></td>
    </tr>
    <tr><td><td>Alfresco ${server.edition} v${server.version}
  </table>
  <blockquote>
    <h1>Models</h1>
  [#list dataDictionary?sort_by(["model", "name", "prefixString"]) as model]
      <hr/>
      <h2>Model: ${model.model.name.prefixString}</h2>

    [#if model.model.namespaces?size > 0]
      <h3>Namespaces</h3>
      <p>
      [#list model.model.namespaces as namespace]
        ${namespace.prefix} = ${namespace.uri}<br/>
      [/#list]
      </p>
    [/#if]

    [#if model.constraints?size > 0]
      <h3>Constraints</h3>
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
  </blockquote>
</body>
</html>
