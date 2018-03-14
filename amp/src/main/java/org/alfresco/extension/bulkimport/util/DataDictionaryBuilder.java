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

import java.util.Collection;

import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;

/**
 * This interface provides an easy way to get an Alfresco Data Dictionary.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public interface DataDictionaryBuilder
{
    /**
     * This method returns the entire registered data dictionary in the Alfresco instance.
     *  
     * @return A handy representation of an Alfresco data dictionary.
     */
    public Collection<Model> getDataDictionary();
    
    public class Model
    {
        public ModelDefinition                  model;
        public Collection<ConstraintDefinition> constraints;
        public Collection<TypeDefinition>       types;
        public Collection<AspectDefinition>     aspects;
        
        // Getters required for Freemarker - see http://freemarker.sourceforge.net/docs/pgui_misc_beanwrapper.html#beanswrapper_hash
        public ModelDefinition                  getModel()       { return(model); }
        public Collection<ConstraintDefinition> getConstraints() { return(constraints); }
        public Collection<TypeDefinition>       getTypes()       { return(types); }
        public Collection<AspectDefinition>     getAspects()     { return(aspects); }
    }
}
