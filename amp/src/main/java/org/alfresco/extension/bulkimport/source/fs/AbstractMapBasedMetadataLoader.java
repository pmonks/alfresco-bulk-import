/*
 * Copyright (C) 2007 Peter Monks
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

package org.alfresco.extension.bulkimport.source.fs;


import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;


/**
 * Abstract MetadataLoader abstracts out the common features of loading metadata
 * from a <code>java.util.Map</code>, regardless of where it came from.
 *
 * @author Peter Monks (pmonks@gmail.com)
 * @see MetadataLoader
 */
abstract class AbstractMapBasedMetadataLoader
    implements MetadataLoader
{
    private final static Log log = LogFactory.getLog(AbstractMapBasedMetadataLoader.class);
    
    private final static String PROPERTY_NAME_TYPE            = "type";
    private final static String PROPERTY_NAME_ASPECTS         = "aspects";
    private final static String PROPERTY_NAME_NAMESPACE       = "namespace";
    private final static String PROPERTY_NAME_PARENT_ASSOC    = "parentAssociation";
    private final static String PROPERTY_NAME_VERSION_COMMENT = "versionComment";
    private final static String PROPERTY_NAME_SEPARATOR       = "separator";

    private final static String DEFAULT_SEPARATOR = ",";
    
    protected final NamespaceService  namespaceService;
    protected final DictionaryService dictionaryService;
    protected final String            defaultSeparator;
    protected final String            metadataFileExtension;
    
    
    
    protected AbstractMapBasedMetadataLoader(final ServiceRegistry serviceRegistry, final String fileExtension)
    {
        this(serviceRegistry, DEFAULT_SEPARATOR, fileExtension);
    }
    
    
    protected AbstractMapBasedMetadataLoader(final ServiceRegistry serviceRegistry, final String defaultSeparator, final String fileExtension)
    {
        // PRECONDITIONS
        assert serviceRegistry  != null : "serviceRegistry must not be null";
        assert defaultSeparator != null : "defaultSeparator must not be null";
        assert fileExtension    != null : "fileExtension must not be null";
        
        // Body
        this.namespaceService      = serviceRegistry.getNamespaceService();
        this.dictionaryService     = serviceRegistry.getDictionaryService();
        this.defaultSeparator      = defaultSeparator;
        this.metadataFileExtension = fileExtension;
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.source.fs.MetadataLoader#getMetadataFileExtension()
     */
    @Override
    public final String getMetadataFileExtension()
    {
        return(metadataFileExtension);
    }
    
    
    /**
     * Method that actually loads the properties from the file. 
     * @param metadataFile The file to load the properties from <i>(must not be null)</i>.
     * @return A new <code>Properties</code> object loaded from that file.
     */
    abstract protected Map<String,Serializable> loadMetadataFromFile(final File metadataFile);


    /**
     * @see org.alfresco.extension.bulkimport.source.fs.MetadataLoader#loadMetadata(java.io.File)
     */
    @Override
    public final Metadata loadMetadata(final File metadataFile)
    {
        Metadata result = new Metadata();
        
        if (metadataFile != null)
        {
            if (metadataFile.canRead())
            {
                Map<String,Serializable> metadataProperties = loadMetadataFromFile(metadataFile);
                String                   separator          = defaultSeparator;
                
                if (metadataProperties != null)
                {
                    // Process and remove the "special keys" first, before any metadata properties
                    if (metadataProperties.containsKey(PROPERTY_NAME_SEPARATOR))  // This one **MUST** be processed first!
                    {
                        separator = (String)metadataProperties.get(PROPERTY_NAME_SEPARATOR);
                        metadataProperties.remove(PROPERTY_NAME_SEPARATOR);
                    }
                    
                    if (metadataProperties.containsKey(PROPERTY_NAME_NAMESPACE))
                    {
                        result.setNamespace((String)metadataProperties.get(PROPERTY_NAME_NAMESPACE));
                        metadataProperties.remove(PROPERTY_NAME_NAMESPACE);
                    }
                    
                    if (metadataProperties.containsKey(PROPERTY_NAME_TYPE))
                    {
                        result.setType((String)metadataProperties.get(PROPERTY_NAME_TYPE));
                        metadataProperties.remove(PROPERTY_NAME_TYPE);
                    }
                    
                    if (metadataProperties.containsKey(PROPERTY_NAME_ASPECTS))
                    {
                        String[] aspectNames = ((String)metadataProperties.get(PROPERTY_NAME_ASPECTS)).split(separator);
                        
                        for (final String aspectName : aspectNames)
                        {
                            result.addAspect(aspectName.trim());
                        }
                        
                        metadataProperties.remove(PROPERTY_NAME_ASPECTS);
                    }
                    
                    if (metadataProperties.containsKey(PROPERTY_NAME_PARENT_ASSOC))
                    {
                        result.setParentAssoc((String)metadataProperties.get(PROPERTY_NAME_PARENT_ASSOC));
                        metadataProperties.remove(PROPERTY_NAME_PARENT_ASSOC);
                    }

                    if (metadataProperties.containsKey(PROPERTY_NAME_VERSION_COMMENT))
                    {
                        result.setVersionComment((String)metadataProperties.get(PROPERTY_NAME_VERSION_COMMENT));
                        metadataProperties.remove(PROPERTY_NAME_VERSION_COMMENT);
                    }

                    // Treat everything else as a metadata property
                    for (final String key : metadataProperties.keySet())
                    {
                        //####TODO: Issue #5 (https://github.com/pmonks/alfresco-bulk-import/issues/5): figure out how to handle properties of type cm:content - they need to be streamed in via a Writer
                    	QName              name               = QName.createQName(key, namespaceService);
                    	PropertyDefinition propertyDefinition = dictionaryService.getProperty(name);  // TODO: measure performance impact of this API call!!
                    	
                    	if (propertyDefinition != null)
                    	{
                        	if (propertyDefinition.isMultiValued())
                        	{
                                // Multi-valued property
                        		ArrayList<Serializable> values = new ArrayList<Serializable>(Arrays.asList(((String)metadataProperties.get(key)).split(separator)));
                        		result.addProperty(key, mapValues(propertyDefinition.getDataType(), values));
                        	}
                        	else
                        	{
                        	    // Single value property
                        	    result.addProperty(key, mapValue(propertyDefinition.getDataType(), metadataProperties.get(key)));
                        	}
                    	}
                    	else
                    	{
                    	    // Residual property
                            if (warn(log)) warn(log, "Property " + String.valueOf(name) + " doesn't exist in the Data Dictionary. Treating as a residual property.");
                            
                            // Try to guess whether it's single or multi- valued
                            ArrayList<Serializable> values = new ArrayList<Serializable>(Arrays.asList(((String)metadataProperties.get(key)).split(separator)));
                            
                            if (values.size() > 1)
                            {
                                // Assume multi-valued
                                result.addProperty(key, values);
                            }
                            else
                            {
                                Serializable value = null;
                                
                                if (values.size() > 0)
                                {
                                    value = metadataProperties.get(key);
                                }
                                    
                                result.addProperty(key, value);
                            }
                    	}
                    }
                }
            }
            else
            {
                if (warn(log)) warn(log, "Metadata file '" + metadataFile.getAbsolutePath() + "' is not readable.");
            }
        }
        
        return(result);
    }
    
    
    /**
     * This method performs mapping for multi-value property values.
     * 
     * @param dataType The data type of the property <i>(must not be null)</i>.
     * @param values   The current values <i>(may be null)</i>.
     * @return The mapped values <i>(may be null)</i>.
     */
    private final ArrayList<Serializable> mapValues(final DataTypeDefinition dataType, final List<Serializable> values)
    {
        // While it would be ideal to use List<Serializable> for the return type, List is not Serializable...
        ArrayList<Serializable> result = null;

        if (values != null)
        {
            result = new ArrayList<>(values.size());

            for (final Serializable value : values)
            {
                result.add(mapValue(dataType, value));
            }
        }
        
        return(result);
    }
    

    /**
     * This method performs mapping for property values.  Right now this means mapping from the value "NOW" to today's date/time
     * for d:date and d:datetime properties.
     * 
     * @param dataType The data type of the property <i>(must not be null)</i>.
     * @param value    The current value <i>(may be null)</i>.
     * @return The mapped value <i>(may be null)</i>.
     */
    private final Serializable mapValue(final DataTypeDefinition dataType, final Serializable value)
    {
        Serializable result = value;
        
        if ((DataTypeDefinition.DATE.equals(dataType.getName()) ||
             DataTypeDefinition.DATETIME.equals(dataType.getName())) &&
            "NOW".equals(value))
        {
            result = new Date();
        }
        
        return(result);
    }

}
