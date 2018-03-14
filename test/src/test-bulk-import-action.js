/*
 * Copyright (C) 2012 Peter Monks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is part of an unsupported extension to Alfresco.
 *
 */

/*
 * This file tests the "bulk-import" repository action.
 */

var bulkImport = actions.create("bulk-import");

if (bulkimport == null)
{
  throw "Bulk Import action is not available."
}

if (space === undefined)
{
  throw "Bulk Import must be executed in the context of a space."
}

bulkImport.parameters["import-source-bean-id"] = "bit.fs.source";   // Note: optional - will default if not provided
bulkImport.parameters["target-node-ref"]       = space.nodeRef;
bulkImport.parameters["parameters"]            =
"{ 'sourceDirectory': '/Users/pmonks/Development/Alfresco/forge/alfresco-bulk-import/test/data/SinglePassTests/MIMETypeTests', \
  'replaceExisting': false, \
  'dryRun': false }";

bulkImport.execute(null);
