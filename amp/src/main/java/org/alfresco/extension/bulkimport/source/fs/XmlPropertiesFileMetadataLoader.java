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


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.service.ServiceRegistry;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;
import static org.alfresco.extension.bulkimport.source.fs.FilesystemSourceUtils.*;


/**
 * MetadataLoader that loads metadata from an (optional) "shadow" properties
 * file in XML format.  This shadow properties file must have <strong>exactly</strong>
 * the same name and extension as the file for whom it is storing metadata, but
 * with the suffix ".metadata.properties.xml".  So for example, if there is a file
 * called "IMG_1967.jpg", the "shadow" metadata file for it would be called
 * "IMG_1967.jpg.metadata.properties.xml". 
 * 
 * The metadata file itself follows the usual rules for Java properties XML
 * files, with a property with the key "type" containing the qualified name of
 * the content type to use for the file, a property with the key "aspects"
 * containing a comma-delimited list of qualified names of the aspects to
 * attach to the file, and then one Java property per metadata property, with
 * the key being the Alfresco property QName and the value being the value of
 * that property.
 * 
 * For example (note escaping rules for namespace separator!):
 * 
 * <code>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
 * <properties>
 *   <comment>Metadata for IMG_1967.jpg</comment>
 *   <entry key="type">cm:content</entry>
 *   <entry key="aspects">cm:versionable, custom:myAspect</entry>
 *   <entry key="cm:title">This is the value of the cm:title field.</entry>
 *   <entry key="cm:description">This is the value of the cm:description field.</entry>
 *   <entry key="cm:taggable">workspace://SpacesStore/3da6c395-3a4b-4a57-836d-8e5</entry>
 *   <entry key="custom:myProperty">This is the value of the custom:myProperty field.</entry>
 *   <entry key="custom:aDateProperty">2001-01-01T12:00:00.000+01:00</entry>
 * </properties>
 * </code>
 * 
 * Notes:
 * <ul>
 *   <li>Java XML properties files fully support Unicode characters.</li>
 *   <li>the metadata must conform to the type and aspect definitions
 *       configured in Alfresco (including mandatory fields, constraints and data
 *       types).  Any violations will terminate the bulk import process.</li>
 *   <li>associations are not yet supported</li>
 *   <li>dates, times and date times <u>must</u> be stored in ISO8601 format
 *       (although note that Alfresco ignores timezone modifiers)</li>
 * </ul>
 *
 * @author Peter Monks (pmonks@gmail.com)
 * @see MetadataLoader, AbstractMapBasedMetadataLoader
 */
public final class XmlPropertiesFileMetadataLoader
    extends AbstractMapBasedMetadataLoader
{
    private final static Log log = LogFactory.getLog(XmlPropertiesFileMetadataLoader.class);
    
    private final static String METADATA_FILE_EXTENSION = "properties.xml";

    
    public XmlPropertiesFileMetadataLoader(final ServiceRegistry serviceRegistry)
    {
        super(serviceRegistry, METADATA_FILE_EXTENSION);
    }
    
    
    public XmlPropertiesFileMetadataLoader(final ServiceRegistry serviceRegistry, final String defaultMultiValuedSeparator)
    {
        super(serviceRegistry, defaultMultiValuedSeparator, METADATA_FILE_EXTENSION);
    }

    
    /**
     * @see org.alfresco.extension.bulkimport.source.fs.AbstractMapBasedMetadataLoader#loadMetadataFromFile(java.io.File)
     */
    @Override
    protected Map<String,Serializable> loadMetadataFromFile(final File metadataFile)
    {
        Map<String, Serializable> result              = null;
        InputStream               metadataInputStream = null;
        
        try
        {
            Properties props = new Properties();
            
            metadataInputStream = new BufferedInputStream(new FileInputStream(metadataFile)); 
            props.loadFromXML(metadataInputStream);
            
            @SuppressWarnings({"rawtypes", "unchecked"})
            Map<String, Serializable> properties = (Map)props;

            result = new HashMap<>(properties);
        }
        catch (final IOException ioe)
        {
            if (warn(log)) warn(log, "Metadata file '" + getFileName(metadataFile) + "' could not be read.", ioe);
        }
        finally
        {
            IOUtils.closeQuietly(metadataInputStream);   // Note: this shouldn't normally be needed as loadFromXML closes the stream passed to it
        }
        
        return(result);
    }

}
