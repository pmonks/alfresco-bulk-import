[#ftl]
[#if results??]
  [#list results as result]
${result.nodeRef},${result.nodePath}
  [/#list]
[/#if]
