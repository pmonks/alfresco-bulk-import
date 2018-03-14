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


package org.alfresco.extension.bulkimport.util;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.alfresco.repo.nodelocator.CompanyHomeNodeLocator;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;


/**
 * This class is a miscellaneous grab bag of methods that are intended to be
 * statically imported.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public class Utils
{
    private final static String COMPANY_HOME_NAME = "Company Home";
    private final static String COMPANY_HOME_PATH = "/" + COMPANY_HOME_NAME;

    
    /**
     * @param t The throwable to get the root cause <i>(may be null)</i>.
     * @return The root cause of t, or null if t was null.
     */
    public final static Throwable getRootCause(final Throwable t)
    {
        Throwable result = null;
        
        if (t != null)
        {
            result = t;
            
            while (result.getCause() != null)
            {
                result = result.getCause();
            }
        }
        
        return(result);
    }
    
    
    /**
     * Converts a path string of the format <code>/fu/bar/blah</code> into a
     * NodeRef
     * 
     * @param serviceRegistry The ServiceRegistry <i>(must not be null)</i>.
     * @param path            The path <i>(must not be null, empty or blank)</i>.
     * @return
     * @throws FileNotFoundException
     */
    public final static NodeRef convertPathToNodeRef(final ServiceRegistry serviceRegistry, final String path)
        throws FileNotFoundException
    {
        // PRECONDITIONS
        assert serviceRegistry != null                             : "serviceRegistry must not be null";
        assert path            != null && path.trim().length() > 0 : "path must not be null, empty or blank";
        
        // BODY
        NodeRef result      = null;
        NodeRef companyHome = serviceRegistry.getNodeLocatorService().getNode(CompanyHomeNodeLocator.NAME, null, null);
        
        if (path.indexOf("://") > 0)  // We have a NodeRef, not a path
        {
            result = new NodeRef(path);
        }
        else
        {
            String cleanTargetPath = path.replaceAll("/+", "/");
            
            if (cleanTargetPath.startsWith(COMPANY_HOME_PATH))
            {
                cleanTargetPath = cleanTargetPath.substring(COMPANY_HOME_PATH.length());
            }
            
            if (cleanTargetPath.startsWith("/"))
            {
                cleanTargetPath = cleanTargetPath.substring(1);
            }
            
            if (cleanTargetPath.endsWith("/"))
            {
                cleanTargetPath = cleanTargetPath.substring(0, cleanTargetPath.length() - 1);
            }
            
            if (cleanTargetPath.length() == 0)
            {
                result = companyHome;
            }
            else
            {
                result = serviceRegistry.getFileFolderService().resolveNamePath(companyHome, Arrays.asList(cleanTargetPath.split("/"))).getNodeRef();
            }
        }
        
        return(result);
    }
    
    
    /**
     * Returns a human-readable rendition of the repository path of the given NodeRef.
     * 
     * @param serviceRegistry The ServiceRegistry <i>(must not be null)</i>.
     * @param nodeRef         The nodeRef from which to derive a path <i>(may be null)</i>.
     * @return The human-readable path <i>(will be null if the nodeRef is null or the nodeRef doesn't exist)</i>.
     */
    public final static String convertNodeRefToPath(final ServiceRegistry serviceRegistry, final NodeRef nodeRef)
    {
        String result = null;
        
        if (nodeRef != null)
        {
            List<FileInfo> pathElements = null;
            
            try
            {
                pathElements = serviceRegistry.getFileFolderService().getNamePath(null, nodeRef);   // Note: violates Google Code issue #132, but allowable in this case since this is a R/O method without an obvious alternative

                if (pathElements != null && pathElements.size() > 0)
                {
                    StringBuilder temp = new StringBuilder();
                    
                    for (FileInfo pathElement : pathElements)
                    {
                        temp.append("/");
                        temp.append(pathElement.getName());
                    }
                    
                    result = temp.toString();
                }
            }
            catch (final FileNotFoundException fnfe)
            {
                // Do nothing
            }
        }
        
        return(result);
    }
    
    
    /**
     * Converts a string into a QName. Required because QName.createQName(String) stopped working for prefixed QName values in Alfresco v5.0.  #facepalm
     * 
     * @param serviceRegistry The ServiceRegistry <i>(must not be null)</i>.
     * @param qname           The qname value to convert to a QName <i>(may be null)</i>.
     * @return The QName <i>(will be null if the qname value is null)</i>.
     */
    public final static QName createQName(final ServiceRegistry serviceRegistry, final String qname)
    {
        QName result = null;
        
        if (qname != null)
        {
            if (qname.startsWith("{"))  // Fully namespaced, ala "{http://www.alfresco.org/model/content/1.0}folder"
            {
                result = QName.createQName(qname);
            }
            else  // Assume prefixed, ala "cm:folder"
            {
                result = QName.createQName(qname, serviceRegistry.getNamespaceService());
            }
        }
        
        return(result);
    }
    
    
    public final static String pluralise(final Number number)
    {
        return(pluralise(number, "s"));
    }
    
    
    public final static String pluralise(final Number number, final String pluralForm)
    {
        return(pluralise(number, pluralForm, ""));
    }
    
    
    public final static String pluralise(final Number number, final String pluralForm, final String singularForm)
    {
        // Java's numerical tower is such rubbish...
        return(BigDecimal.ONE.equals(new BigDecimal(number.toString())) ? singularForm : pluralForm);
    }
    
    
    public final static String buildTextMessage(final Throwable t)
    {
        StringBuilder result        = new StringBuilder();
        String        timeOfFailure = (new Date()).toString();
        String        hostName      = null;
        String        ipAddress     = null;

        try
        {
            hostName  = InetAddress.getLocalHost().getHostName();
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (final UnknownHostException uhe)
        {
            hostName  = "unknown";
            ipAddress = "unknown";
        }

        result.append("\nTime of failure:             " + timeOfFailure);
        result.append("\nHost where failure occurred: " + hostName + " (" + ipAddress + ")");
        
        if (t != null)
        {
            result.append("\nRoot exception:");
            result.append(renderExceptionStackAsText(t));
        }
        else
        {
            result.append("\nNo exception was provided.");
        }

        return(result.toString());
    }
    
    
    public final static String renderExceptionStackAsText(final Throwable t)
    {
        StringBuilder result = new StringBuilder();

        if (t != null)
        {
            String    message = t.getMessage();
            Throwable cause   = t.getCause();

            if (cause != null)
            {
                result.append(renderExceptionStackAsText(cause));
                result.append("\nWrapped by:");
            }

            if (message == null)
            {
                message = "";
            }

            result.append("\n");
            result.append(t.getClass().getName());
            result.append(": ");
            result.append(message);
            result.append("\n");
            result.append(renderStackTraceElements(t.getStackTrace()));
        }

        return(result.toString());
    }
    
    
    private final static String renderStackTraceElements(final StackTraceElement[] elements)
    {
        StringBuilder result = new StringBuilder();

        if (elements != null)
        {
            for (int i = 0; i < elements.length; i++)
            {
                result.append("\tat " + elements[i].toString() + "\n");
            }
        }

        return(result.toString());
    }
    
    
}
