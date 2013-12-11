[#ftl]
{
  "data" :
  [
[#if results??]
  [#list results as result]
    {
      "nodeRef" : "${result.nodeRef}",
      "path"    : "${result.nodePath}"
    }[#if result.nodeRef != results?last.nodeRef],[/#if]
  [/#list]
[/#if]
  ]
}
