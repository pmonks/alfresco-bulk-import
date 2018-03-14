/*
 * Copyright (C) 2012 Peter Monks
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.extension.bulkimport.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.NamespaceDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.QName;


/**
 * This class implements the DataDictionaryBuilder interface.
 *
 * @author Peter Monks (pmonks@gmail.com)
 * @see org.alfresco.extension.bulkimport.util.DataDictionaryBuilder
 *
 */
public class DataDictionaryBuilderImpl
    implements DataDictionaryBuilder
{
    private final DictionaryService dictionaryService;
    
    
    /**
     * Constructor for the DataDictionaryBuilder
     * 
     * @param serviceRegistry The Alfresco ServiceRegistry <i>(must not be null)</i>.
     */
    public DataDictionaryBuilderImpl(final ServiceRegistry serviceRegistry)
    {
        // PRECONDITIONS
        assert serviceRegistry != null : "serviceRegistry must not be null.";
        
        //BODY
        this.dictionaryService = serviceRegistry.getDictionaryService();
    }
    

    /**
     * @see org.alfresco.extension.bulkimport.util.DataDictionaryBuilder#getDataDictionary()
     */
    @Override
    public Collection<Model> getDataDictionary()
    {
        Collection<Model> result = new ArrayList<Model>();
        
        for (final QName modelQName : dictionaryService.getAllModels())
        {
            Model model = new Model();
            
            model.model       = dictionaryService.getModel(modelQName);
            model.constraints = dictionaryService.getConstraints(modelQName);
            model.types       = new ArrayList<TypeDefinition>();
            
            for (final QName aspectQName : dictionaryService.getTypes(modelQName))
            {
                model.types.add(dictionaryService.getType(aspectQName));
            }
            
            model.aspects = new ArrayList<AspectDefinition>();
            
            for (final QName aspectQName : dictionaryService.getAspects(modelQName))
            {
                model.aspects.add(dictionaryService.getAspect(aspectQName));
            }
            
            result.add(model);
        }
        
        return(result);
    }
    

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        final StringBuilder     result         = new StringBuilder(1024);
        final Collection<Model> dataDictionary = getDataDictionary();
        
        result.append("Models:");
        
        if (dataDictionary != null)
        {
            for (Model model : dataDictionary)
            {
                ModelDefinition modelDefinition = model.model;
                
                result.append("\n\t");
                result.append(modelDefinition.getName().toPrefixString());
                
                result.append("\n\t\tNamespaces:");
                Collection<NamespaceDefinition> namespaces = modelDefinition.getNamespaces();
                if (namespaces != null && namespaces.size() > 0)
                {
                    for (NamespaceDefinition namespace : namespaces)
                    {
                        result.append("\n\t\t\t");
                        result.append(namespace.getPrefix());
                        result.append(" = ");
                        result.append(namespace.getUri());
                    }
                }
                else
                {
                    result.append("\n\t\t\t<none>");
                }
                
                result.append("\n\t\tConstraints:");
                if (model.constraints != null && model.constraints.size() > 0)
                {
                    for (ConstraintDefinition constraint : model.constraints)
                    {
                        result.append("\n\t\t\t");
                        result.append(constraint.getName().toPrefixString());
                    }
                }
                else
                {
                    result.append("\n\t\t\t<none>");
                }
                
                result.append("\n\t\tTypes:");
                if (model.types != null && model.types.size() > 0)
                {
                    for (TypeDefinition type : model.types)
                    {
                        result.append(classDefinitionToString(type));
                    }
                }
                else
                {
                    result.append("\n\t\t\t<none>");
                }

                result.append("\n\t\tAspects:");
                if (model.aspects != null && model.aspects.size() > 0)
                {
                    for (AspectDefinition aspect : model.aspects)
                    {
                        result.append(classDefinitionToString(aspect));
                    }
                }
                else
                {
                    result.append("\n\t\t\t<none>");
                }
            }
        }
        else
        {
            result.append("\n\t<none>");
        }
        
        return(result.toString());
    }
    
    
    private String classDefinitionToString(final ClassDefinition definition)
    {
        StringBuilder result = null;
        
        if (definition != null)
        {
            result = new StringBuilder(1024);
            result.append("\n\t\t\t");
            result.append(definition.getName().toPrefixString());
            
            result.append("\n\t\t\t\tParent: ");
            result.append(definition.getParentName() == null ? "<none>" : definition.getParentName().getPrefixString());
            
            result.append("\n\t\t\t\tProperties:");
            Map<QName,PropertyDefinition> properties = definition.getProperties();
            if (properties != null && properties.size() > 0)
            {
                for (QName propertyName : properties.keySet())
                {
                    PropertyDefinition propertyDefinition = properties.get(propertyName);
                    
                    result.append("\n\t\t\t\t\t");
                    result.append(propertyName.toPrefixString());
                    result.append(" (");
                    result.append(propertyDefinition.getDataType().getName().getPrefixString());
                    result.append(")");
                    
                    if (propertyDefinition.isMultiValued())
                    {
                        result.append(" (multi-valued)");
                    }
                    
                    if (propertyDefinition.isMandatoryEnforced())
                    {
                        result.append(" (mandatory)");
                    }
                    
                    List<ConstraintDefinition> propertyConstraints = propertyDefinition.getConstraints();
                    if (propertyConstraints != null && propertyConstraints.size() > 0)
                    {
                        result.append(" (constraints: ");
                        for (ConstraintDefinition propertyConstraint : propertyConstraints)
                        {
                            result.append(propertyConstraint.getName().toPrefixString());
                            result.append(", ");
                        }
                        
                        result.delete(result.length() - ", ".length(), result.length());
                        result.append(")");
                    }
                }
            }
            else
            {
                result.append("\n\t\t\t\t\t<none>");
            }
            
            result.append("\n\t\t\t\tAssociations:");
            Map<QName, AssociationDefinition> associations = definition.getAssociations();
            if (associations != null && associations.size() > 0)
            {
                for (QName associationName : associations.keySet())
                {
                    AssociationDefinition associationDefinition = associations.get(associationName);
                    
                    result.append("\n\t\t\t\t\t");
                    result.append(associationName.toPrefixString());
                    
                    result.append(associationDefinition.isChild() ? " (parent/child)" : " (peer)");
                    
                    result.append(" (source: ");
                    result.append(associationDefinition.getSourceClass().getName().toPrefixString());
                    result.append(")");
                    
                    result.append(" (target: ");
                    result.append(associationDefinition.getTargetClass().getName().toPrefixString());
                    result.append(")");
                }
            }
            else
            {
                result.append("\n\t\t\t\t\t<none>");
            }
        }
        
        return(result == null ? null : result.toString());
    }

}
