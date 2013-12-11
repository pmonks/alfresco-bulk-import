/*
 * Builds the full path of the given node, or null if it's "invalid".
 */
function buildFullPath(theNode)
{
  if (theNode)
  {
    if (theNode.equals(companyhome))  // We hit Company Home, return it
    {
      return("/" + theNode.name);
    }
    else if (theNode.parent)  // We're at a normal folder node, recurse
    {
      var parentPath = buildFullPath(theNode.parent);
      return(parentPath + "/" + theNode.name);
    }
  }
    
  return(null);  // Shouldn't ever get here, but you never know...
}


/*
 * Script starts here.
 */
if (args.query === undefined || args.query.length == 0)
{
   status.code     = 400;
   status.message  = "Mandatory parameter 'query' was not provided.";
   status.redirect = true;
}
else
{
  var queryTerm   = args.query;
  var luceneQuery = 'TYPE:"cm\\:folder" AND NOT TYPE:"wca\\:webfolder" AND PATH:"/app\\:company\\_home//*" AND @cm\\:name:"' + queryTerm + '*"';

  logger.log("About to execute luceneQuery: " + luceneQuery);

  var nodes = search.luceneSearch("workspace://SpacesStore", luceneQuery);
  
  model.results = [];

  for (var i = 0; i < nodes.length; i++)
  {
    var node     = nodes[i];
    var nodeRef  = "" + node.nodeRef;  // Force coercion to a string. This shouldn't be necessary...
    var nodePath = buildFullPath(node);
    
    if (nodePath !== null)
    {
      var result = { "nodeRef" : nodeRef, "nodePath" : nodePath };
      model.results.push(result);
    }
  }
}
