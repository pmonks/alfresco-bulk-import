/*
 * Copyright (C) 2007 Peter Monks
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

package org.alfresco.extension.bulkimport.source;

import org.alfresco.extension.bulkimport.BulkImportStatus;

/**
 * This interface provides a minimal write interface on top of
 * <code>BulkImportStatus</code>, to allow a <code>BulkImportSource</code>
 * to track source-side statistics.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public interface BulkImportSourceStatus
    extends BulkImportStatus
{
    /**
     * @param name The name of the source object currently being scanned <i>(may be null)</i>.
     */
    void setCurrentlyScanning(String name);
    
    /**
     * "Preregisters" counters - creates them and initialises them to zero.  This method is
     * optional - incrementing a counter that doesn't already exist will create it.  It does
     * however ensure that all counters show up in the status display, even if they haven't
     * been incremented yet.
     * 
     * @param counterNames The list of counter names to preregister <i>(may be null)</i>.
     */
    void preregisterSourceCounters(String[] counterNames);
    
    /**
     * @param counterName The name of the counter to increment. Will be created (and
     * set to 1) if it doesn't already exist. <i>(must not be null)</i>
     */
    void incrementSourceCounter(String counterName);

    /**
     * @param counterName The name of the counter to increment. Will be created (and
     * set to value) if it doesn't already exist. <i>(must not be null)</i>
     * @param value The value to increment by.
     */
    void incrementSourceCounter(String counterName, long value);
}
