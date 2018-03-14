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
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.extension.bulkimport.source.BulkImportItemVersion;


/**
 * This class is a miscellaneous grab bag of filesystem methods that are
 * intended to be statically imported.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class FilesystemSourceUtils
{
    private final static String DEFAULT_TEXT_ENCODING  = "UTF-8";
    private final static int    MAX_CONTENT_URL_LENGTH = 255;
    
    // Regexes for matching version files
    public  final static String  VERSION_LABEL_REGEX      = "([\\d]+)(\\.([\\d]+))?"; // Group 0 = version label, Group 1 = major version #, group 3 (if not null) = minor version #
    private final static String  VERSION_SUFFIX_REGEX     = "\\.v(" + VERSION_LABEL_REGEX + ")\\z"; // Note: group numbers are one greater than shown above
    private final static String  VERSION_FILENAME_REGEX   = ".+" + VERSION_SUFFIX_REGEX;
    private final static Pattern VERSION_FILENAME_PATTERN = Pattern.compile(VERSION_FILENAME_REGEX);
    
    /**
     * Returns true if the suspectedChild is within the given directory.  The
     * String version is preferred over this one, as it forces normalisation
     * of the two paths first.
     * 
     * @param directory      The directory in which to check <i>(may be null, although doing so will always return false)</i>.
     * @param suspectedChild The suspect child to check for <i>(may be null, although doing so will always return false)</i>.
     * @return true if and only if suspectedChild is "within" directory.  Note that this comparison is done solely at a "path string"
     *         level.  It will attempt to remove relative path elements (".." especially) to avoid incorrect results, but YMMV.
     */
    public final static boolean isInDirectory(final File directory, final File suspectedChild)
    {
        return(isInDirectory(directory.getAbsolutePath(), suspectedChild.getAbsolutePath()));
    }
    

    /**
     * Returns true if the suspectedChild is within the given directory.  This
     * method is preferred over the File version, as it forces normalisation
     * of the two paths
     * (see <a href="http://commons.apache.org/proper/commons-io/javadocs/api-2.4/org/apache/commons/io/FilenameUtils.html#normalize(java.lang.String, boolean)">this reference</a>)
     * first.
     * 
     * @param directoryPath      The directory in which to check <i>(may be null, although doing so will always return false)</i>.
     * @param suspectedChildPath The suspect child to check for <i>(may be null, although doing so will always return false)</i>.
     * @return true if and only if suspectedChild is "within" directory.  Note that this comparison is done solely at a "path string"
     *         level.
     */
    public final static boolean isInDirectory(final String directoryPath, final String suspectedChildPath)
    {
        return(isInDirectoryImpl(new File(FilenameUtils.normalize(directoryPath,      true)),
                                 new File(FilenameUtils.normalize(suspectedChildPath, true))));
    }
    
    
    private final static boolean isInDirectoryImpl(final File directory, final File suspectedChild)
    {
        boolean result = false;
        
        if (directory != null && suspectedChild != null)
        {
            if (suspectedChild.equals(directory))
            {
                result = true;
            }
            else
            {
                result = isInDirectoryImpl(directory, suspectedChild.getParentFile());
            }
        }
        
        return(result);
    }
 

    /**
     * Determines whether the given file is already located in an Alfresco managed content store.  Used to determine
     * whether to perform a streaming or in-place import.
     * 
     * @param contentStore The content store Alfresco is configured to use <i>(must not be null)</i>.
     * @param source The file to test.  Typically this would be the source directory for the import <i>(must not be null)</i>.
     * @return True if the given file is in an Alfresco managed content store, false otherwise.
     */
    public final static boolean isInContentStore(final ContentStore contentStore, final File source)
    {
        boolean      result           = false;
        final String contentStoreRoot = contentStore.getRootLocation();
        
        if (contentStoreRoot != null && contentStoreRoot.trim().length() > 0)
        {
            final File contentStoreRootFile = new File(contentStoreRoot);
            
            // If the content store root doesn't exist as a file, we're probably dealing with a non-filesystem content store
            if (contentStoreRootFile.exists() && contentStoreRootFile.isDirectory())
            {
                result = isInDirectory(contentStoreRoot, source.getAbsolutePath());
            }
        }

        return(result);
    }
    
    
    /**
     * @param file The file to get the name of <i>(may be null)</i>.
     * @return A human readable rendition of the file <i>(null when file is null)</i>.
     */
    public final static String getFileName(final File file)
    {
        String result = null;
        
        if (file != null)
        {
            result = file.getAbsolutePath();
        }
        
        return(result);
    }

    
    /**
     * This method does the magic of constructing the content URL for
     * "in-place" content.
     * 
     * @param mimeTypeService The Alfresco MimetypeService <i>(must not be null)</i>.
     * @param contentStore    The content store Alfresco is configured to use <i>(must not be null)</i>.
     * @param contentFile     The content file to build a content URL for <i>(must not be null)</i>.
     * @return The constructed <code>ContentData</code>, or null if the contentFile cannot be in-place imported for any reason.
     */
    public final static ContentData buildContentProperty(final MimetypeService mimeTypeService, final ContentStore contentStore, final File contentFile)
    {
        ContentData result = null;
        
        final String normalisedFilename         = FilenameUtils.normalize(contentFile.getAbsolutePath(), true);
        String       normalisedContentStoreRoot = FilenameUtils.normalize(contentStore.getRootLocation(), true);
        
        // Ensure content store root ends with a single / character
        if (!normalisedContentStoreRoot.endsWith("/"))
        {
            normalisedContentStoreRoot = normalisedContentStoreRoot + "/";
        }
        
        // If, after normalisation, the filename doesn't start with the content store root, we can't in-place import
        if (normalisedFilename.startsWith(normalisedContentStoreRoot))
        {
            final String contentStoreRelativeFilename = normalisedFilename.substring(normalisedContentStoreRoot.length());
            final String contentUrl                   = FileContentStore.STORE_PROTOCOL + ContentStore.PROTOCOL_DELIMITER + contentStoreRelativeFilename;
    
            // If the resulting content URL would be too long, we can't in-place import
            if (contentUrl.length() <= MAX_CONTENT_URL_LENGTH)
            {
                final String mimeType = mimeTypeService.guessMimetype(contentFile.getName());
                final String encoding = guessEncoding(mimeTypeService, contentFile, mimeType);
                
                result = new ContentData(contentUrl, mimeType, contentFile.length(), encoding);
            }
        }
        
        return(result);
    }
    
    
    /**
     * Attempt to guess the encoding of a text file , falling back to {@link #DEFAULT_TEXT_ENCODING}.
     *
     * @param mimeTypeService The Alfresco MimetypeService <i>(must not be null)</i>.
     * @param file            The {@link java.io.File} to test <i>(must not be null)</i>.
     * @param mimeType        The file MIME type. Used to first distinguish between binary and text files <i>(must not be null)</i>.
     * @return The text encoding as a {@link String}.
     */
    public final static String guessEncoding(final MimetypeService mimeTypeService, final File file, final String mimeType)
    {
        String                     result        = DEFAULT_TEXT_ENCODING;
        InputStream                is            = null;
        final ContentCharsetFinder charsetFinder = mimeTypeService.getContentCharsetFinder();

        if (mimeTypeService.isText(mimeType))
        {
            try
            {
               is     = new BufferedInputStream(new FileInputStream(file));
               result = charsetFinder.getCharset(is, mimeType).name();
            }
            catch (final IOException ioe)
            {
                result = DEFAULT_TEXT_ENCODING;
            }
            finally
            {
                IOUtils.closeQuietly(is);
            }
        }
        
        return(result);
    }
    
    
    /**
     * Strips the version suffix (if any) from a filename.
     * 
     * @param fileName The filename to strip the version suffix from <i>(must not be null, empty or blank)</i>.
     * @return The filename with the version suffix (if any) stripped.
     */
    public static String stripVersionSuffix(final String fileName)
    {
        String result = fileName;
        
        if (isVersionFile(result))
        {
            result = result.replaceFirst(VERSION_SUFFIX_REGEX, "");
        }
        
        return(result);
    }

    
    /**
     * Get the name of the parent file for this file.
     * 
     * @param metadataLoader The configured <code>MetadataLoader</code> <i>(must not be null)</i>.
     * @param fileName       The filename to check <i>(must not be null, empty or blank)</i>.
     * @return The name of the parent file this file.
     */
    public static String getParentName(final MetadataLoader metadataLoader, final String fileName)
    {
        String result = stripVersionSuffix(fileName);
        
        if (isMetadataFile(metadataLoader, result))
        {
            result = result.substring(0, result.length() - (MetadataLoader.METADATA_SUFFIX + metadataLoader.getMetadataFileExtension()).length());
        }
        
        return(result);
    }
    
    
    /**
     * @param fileName The filename to check <i>(must not be null, empty or blank)</i>.
     * @return True if the given filename represents a version file, false otherwise.
     */
    public static boolean isVersionFile(final String fileName)
    {
        Matcher matcher = VERSION_FILENAME_PATTERN.matcher(fileName);

        return(matcher.matches());
    }
    

    /**
     * @param metadataLoader The configured <code>MetadataLoader</code> <i>(must not be null)</i>.
     * @param fileName       The filename to check <i>(must not be null, empty or blank)</i>.
     * @return True if the given filename represents a metadata file, false otherwise.
     */
    public static boolean isMetadataFile(final MetadataLoader metadataLoader, final String fileName)
    {
        boolean result = false;
        
        if (metadataLoader != null)
        {
            final String tmpFileName = stripVersionSuffix(fileName);
            
            result = tmpFileName.endsWith(MetadataLoader.METADATA_SUFFIX + metadataLoader.getMetadataFileExtension());
        }

        return(result);
    }
    
    
    /**
     * @param fileName The filename to check <i>(must not be null, empty or blank)</i>.
     * @return The version label for the given filename, or <code>Version.VERSION_HEAD</code> if it doesn't have one.
     */
    public static BigDecimal getVersionNumber(final String fileName)
    {
        BigDecimal result = null;
        
        if (fileName != null)
        {
            Matcher m = VERSION_FILENAME_PATTERN.matcher(fileName);
            
            if (m.matches())
            {
                result = new BigDecimal(m.group(1));  // Group 1 = version label, including full stop separator for decimal version numbers
            }
            else
            {
                result = BulkImportItemVersion.VERSION_HEAD;  // Filename doesn't include a version label, so its version is HEAD
            }
        }
        
        return(result);
    }
    
}
