/*
 * Copyright (C) 2007-2015 Peter Monks.
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

package org.alfresco.extension.bulkimport.actions;

import java.util.List;
import java.util.Map;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.extension.bulkimport.BulkImporter;
import org.alfresco.extension.bulkimport.source.fs.FilesystemBulkImportSource;


/**
 * This class exposes the bulk import functionality as a repository action called
 * "bit.bulk-import-action" (note: action names are the same as the implementation
 * classes Spring bean id, which is a terrible design but I digress...).
 * 
 * The parameters for this action are:
 * 
 * <ul>
 * <li>source-bean-id - the Spring bean id of the source (optional, defaults to the built-in filesystem)</li>
 * <li>parameters - a JSON string of key / value-list pairs containing the parameters for the chosen source (mandatory, see sources for details on the parameters they require)</li>
 * <li>target-noderef - the NodeRef of the target space for the imported content (mandatory, must be a writeable cm:folder)</li>
 * </ul>
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public class BulkImportActionExecutor
    extends ActionExecuterAbstractBase
{
    private final static String PARAM_SOURCE_BEAN_ID = "source-bean-id";
    private final static String PARAM_PARAMETERS     = "parameters";
    private final static String PARAM_TARGET         = "target-noderef";
    
    private final static String DEFAULT_SOURCE_BEAN_ID = FilesystemBulkImportSource.IMPORT_SOURCE_NAME;
    
    
    private final BulkImporter bulkImport;
    
    
    public BulkImportActionExecutor(final BulkImporter bulkImport)
    {
        // PRECONDITIONS
        assert bulkImport != null : "bulkImport must not be null.";
        
        // Body
        this.bulkImport = bulkImport;
    }
    
    
    /**
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        paramList.add(new ParameterDefinitionImpl(PARAM_SOURCE_BEAN_ID, DataTypeDefinition.TEXT,     false, getParamDisplayLabel(PARAM_SOURCE_BEAN_ID)));
        paramList.add(new ParameterDefinitionImpl(PARAM_PARAMETERS,     DataTypeDefinition.TEXT,     true,  getParamDisplayLabel(PARAM_PARAMETERS)));
        paramList.add(new ParameterDefinitionImpl(PARAM_TARGET,         DataTypeDefinition.NODE_REF, true,  getParamDisplayLabel(PARAM_TARGET)));
    }


    /**
     * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected void executeImpl(final Action actionInstance, final NodeRef actedUponNodeRef)
    {
        // PRECONDITIONS
        assert actionInstance != null : "actionInstance must not be null.";

        // Body
        String                    sourceBeanId   = (String)actionInstance.getParameterValue(PARAM_SOURCE_BEAN_ID);
        String                    parametersJson = (String)actionInstance.getParameterValue(PARAM_PARAMETERS);
        NodeRef                   target         = (NodeRef)actionInstance.getParameterValue(PARAM_TARGET);
        Map<String, List<String>> parameters     = null;
        
        if (sourceBeanId == null)
        {
            sourceBeanId = DEFAULT_SOURCE_BEAN_ID;
        }
        
        parameters = parseParametersJson(parametersJson);
        
        // Initiate the import
        bulkImport.start(sourceBeanId, parameters, target);
    }
    
    
    private final Map<String, List<String>> parseParametersJson(final String parametersJson)
    {
        //####TODO: IMPLEMENT THIS!!!
        return(null);
    }

}
