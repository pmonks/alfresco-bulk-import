/*
 * Copyright (C) 2007-2013 Peter Monks.
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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * Definition of a metadata loader - a class that can load metadata for a file from some other source.
 * Note that metadata loaders can be "chained", so an implementation needs to be careful about how the
 * Metadata object is populated in the populateMetadata method.
 * 
 * Implementors also need to be careful when configuring the bulk import process, as the order in which
 * metadata loaders are configured into a bulk importer is the order of precendence (from lowest to
 * highest).
 *
 * @author Peter Monks (pmonks@alfresco.com)
 */
public interface MetadataLoader
{
    /**
     * Metadata filename suffix (excluding file-type specific ending)
     */
    public final static String METADATA_SUFFIX = ".metadata.";
    
    /**
     * @return The extension for files used to store this metadata, minus the stop character (.) e.g. "properties", "xml", "json", etc.
     */
    String getMetadataFileExtension();
        

    /**
     * Method that populates the type, aspects and properties to attach to a given file or space.
     * 
     * @param contentAndMetadata The contentAndMetadata from which to obtain the metadata <i>(will not be null)</i>.
     * @param metadata           The metadata object to populate <i>(will not be null, and may already be partially populated)</i>.
     */
    void loadMetadata(final BulkImportItem.ContentAndMetadata contentAndMetadata, MetadataLoader.Metadata metadata);
    

    /**
     * Class used to encapsulate the type, aspects and property values for a single file or folder.
     */
    public final class Metadata
    {
        private QName                    type;
        private Set<QName>               aspects;
        private String                   namespace;
        private QName                    parentAssoc;
        private Map<QName, Serializable> properties;
        
        
        public Metadata()
        {
            type        = null;
            aspects     = new HashSet<QName>();
            namespace   = NamespaceService.CONTENT_MODEL_1_0_URI;
            parentAssoc = ContentModel.ASSOC_CONTAINS;
            properties  = new HashMap<QName, Serializable>(); 
        }
        

        /**
         * @return the type
         */
        public QName getType()
        {
            return(type);
        }
        

        /**
         * @param type The type to set in this metadata object <i>(must not be null)</i>.
         */
        public void setType(final QName type)
        {
            // PRECONDITIONS
            assert type != null : "type must not be null.";
            
            // Body
            this.type = type;
        }
        
        
        /**
         * @return The set of aspects in this metadata object <i>(will not be null, but may be empty)</i>.
         */
        public Set<QName> getAspects()
        {
            return(Collections.unmodifiableSet(aspects));
        }
        
        
        /**
         * @param aspect An aspect to add to this metadata object <i>(must not be null)</i>.
         */
        public void addAspect(final QName aspect)
        {
            // PRECONDITIONS
            assert aspect != null : "aspect must not be null.";
            
            // Body
            aspects.add(aspect);
        }
        

        /**
         * @return the namespace
         */
        public String getNamespace()
        {
            return namespace;
        }


        /**
         * @param namespace The namespace to set in this metadata object <i>(must not be null)</i>.
         */
        public void setNamespace(final String namespace)
        {
            // PRECONDITIONS
            assert namespace != null : "namespace must not be null.";
            
            this.namespace = namespace;
        }


        /**
         * @return The parent association type.
         */
        public QName getParentAssoc()
        {
            return(parentAssoc);
        }
        
        
        /**
         * @param parentAssoc The parent association type to set in this metadata object <i>(must not be null)</i>.
         */
        public void setParentAssoc(final QName parentAssoc)
        {
            // PRECONDITIONS
            assert parentAssoc != null : "parentAssoc must not be null.";
            
            // Body
            this.parentAssoc = parentAssoc;
        }

        
        /**
         * @return The properties in this metadata object <i>(will not be null, but may be empty)</i>.
         */
        public Map<QName, Serializable> getProperties()
        {
            return(Collections.unmodifiableMap(properties));
        }
        
        
        /**
         * Adds a property and its value to this metadata object. 
         * 
         * @param property The property to populate <i>(must not be null)</i>.
         * @param value    The value of the property <i>(may be null)</i>.
         */
        public void addProperty(final QName property, final Serializable value)
        {
            // PRECONDITIONS
            assert property != null : "property must not be null";
            
            // Body
            properties.put(property, value);
        }
        
        
        @Override
        public String toString()
        {
            return(new ToStringBuilder(this)
                   .append("type",        type)
                   .append("parentAssoc", parentAssoc)
                   .append("aspects",     aspects)
                   .append("properties",  properties)
                   .toString());
        }
        
    }
    
    
}
