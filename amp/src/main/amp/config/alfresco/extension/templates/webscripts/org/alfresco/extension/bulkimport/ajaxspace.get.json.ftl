[#ftl]
[#-- Returns the matching spaces in jQuery compatible format (i.e. a top-level JS array of Strings) --]
[
[#if results??]
  [#list results as result]
  "${result.nodePath}"[#if result.nodeRef != results?last.nodeRef],[/#if]
  [/#list]
[/#if]
]
