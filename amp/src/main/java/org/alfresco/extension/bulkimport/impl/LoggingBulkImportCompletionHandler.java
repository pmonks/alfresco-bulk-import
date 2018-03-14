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


package org.alfresco.extension.bulkimport.impl;

import java.util.IllegalFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.extension.bulkimport.BulkImportCompletionHandler;
import org.alfresco.extension.bulkimport.BulkImportStatus;

import static java.util.concurrent.TimeUnit.*;
import static org.alfresco.extension.bulkimport.util.Utils.*;
import static org.alfresco.extension.bulkimport.util.LogUtils.*;

/**
 * This class is a simple <code>BulkImportCompletionHandler</code> that simply logs a summary report of the import (at INFO level).
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class LoggingBulkImportCompletionHandler
    implements BulkImportCompletionHandler
{
    private final static Log log = LogFactory.getLog(LoggingBulkImportCompletionHandler.class);

    /**
     * @see org.alfresco.extension.bulkimport.BulkImportCompletionHandler#importComplete(org.alfresco.extension.bulkimport.BulkImportStatus)
     */
    @Override
    public final void importComplete(BulkImportStatus importStatus)
    {
        if (info(log))
        {
            final String processingState            = importStatus.getProcessingState();
            final String durationStr                = getHumanReadableDuration(importStatus.getDurationInNs());
            final long   batchesSubmitted           = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BATCHES_SUBMITTED)             == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BATCHES_SUBMITTED);
            final long   batchesImported            = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE)              == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE);
            final float  batchesPerSecond           = importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE, SECONDS) == null ? 0.0F : importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BATCHES_COMPLETE, SECONDS);
            final float  nodesPerSecond             = importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_NODES_IMPORTED, SECONDS)   == null ? 0.0F : importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_NODES_IMPORTED, SECONDS);
            final float  bytesPerSecond             = importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BYTES_IMPORTED, SECONDS)   == null ? 0.0F : importStatus.getTargetCounterRate(BulkImportStatus.TARGET_COUNTER_BYTES_IMPORTED, SECONDS);
            final long   nodesImported              = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_IMPORTED)                == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_IMPORTED);
            final long   versionsImported           = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_VERSIONS_IMPORTED)             == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_VERSIONS_IMPORTED);
            final long   metadataPropertiesImported = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_METADATA_PROPERTIES_IMPORTED)  == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_METADATA_PROPERTIES_IMPORTED);
            final long   bytesImported              = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BYTES_IMPORTED)                == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_BYTES_IMPORTED);
            final long   contentInPlace             = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_IN_PLACE_CONTENT_LINKED)       == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_IN_PLACE_CONTENT_LINKED);
            final long   contentStreamed            = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_CONTENT_STREAMED)              == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_CONTENT_STREAMED);
            final long   filesSkipped               = importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_SKIPPED)                 == null ? 0L   : importStatus.getTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_SKIPPED);

            try
            {
                final String message = String.format("%s bulk import completed (%s) in %s.\n" +
                                                     "\tBatch%s\t\t%d imported of %d submitted (%.3f / sec)\n" +
                                                     "\tNode%s:\t\t\t%d (%.3f / sec)\n" +
                                                     "\tByte%s:\t\t\t%d (%.3f / sec)\n" +
                                                     "\tVersion%s:\t\t%d\n" +
                                                     "\tMetadata propert%s:\t%d\n" +
                                                     "\tFiles:\t\t\t%d in-place, %d streamed, %d skipped\n",
                                                     (importStatus.inPlaceImportPossible() ? "In place" : "Streaming"),  processingState, durationStr,
                                                     pluralise(batchesImported, "es:", ":\t"),          batchesImported, batchesSubmitted, batchesPerSecond,
                                                     pluralise(nodesImported),                          nodesImported,   nodesPerSecond,
                                                     pluralise(bytesImported),                          bytesImported,   bytesPerSecond,
                                                     pluralise(versionsImported),                       versionsImported,
                                                     pluralise(metadataPropertiesImported, "ies", "y"), metadataPropertiesImported,
                                                     contentInPlace,                                    contentStreamed, filesSkipped);

                info(log, message);
            }
            catch (final IllegalFormatException ife)
            {
                // To help troubleshoot bugs in the String.format call above
                error(log, ife);
            }
        }
    }

}
