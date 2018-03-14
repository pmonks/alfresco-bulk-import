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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.apache.commons.logging.Log;

/**
 * This class is a grab bag of functions that are helpful for logging.
 * 
 * The logging functions themselves are basically an awful hack around Java's
 * lack of macros.  To use them, write code such as:
 * 
 * <code>
 * import static org.alfresco.extension.bulkimport.BulkImportLogUtils.*;
 * ...later...
 * if (debug(log)) debug("your message goes here");
 * </code>
 * 
 * The primary advantage of using these methods is that they will identify every
 * log entry it generates with the string "BULKIMPORT", which makes grepping
 * the log files for bulk import specific entries reliable.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class LogUtils
{
    private final static String IDENTIFIER    = "BULKIMPORT";
    private final static String PREFIX        = IDENTIFIER + ": ";
    private final static String RAW_EXCEPTION = IDENTIFIER + " threw: ";
    
    private final static long NS_PER_MICROSECOND = 1000L;
    private final static long NS_PER_MILLISECOND = NS_PER_MICROSECOND * 1000L;
    private final static long NS_PER_SECOND      = NS_PER_MILLISECOND * 1000L;
    private final static long NS_PER_MINUTE      = NS_PER_SECOND * 60L;
    private final static long NS_PER_HOUR        = NS_PER_MINUTE * 60L;
    private final static long NS_PER_DAY         = NS_PER_HOUR * 24;
    
    
    /**
     * @param durationInNs A duration in nanoseconds (i.e. from System.nanoTime()) <i>(may be null)</i>.
     * @return A human readable string representing that duration as "Ud Vh Wm Xs Y.Zms", "<unknown>" if the duration is null.
     */
    public final static String getHumanReadableDuration(final Long durationInNs)
    {
        return(getHumanReadableDuration(durationInNs, true));
    }
    
    
    /**
     * @param durationInNs A duration in nanoseconds (i.e. from System.nanoTime()) <i>(may be null)</i>.
     * @param includeMs    Flag indicating whether to include milliseconds or not.
     * @return A human readable string representing that duration as "Ud Vh Wm Xs Y.Zms", "<unknown>" if the duration is null.
     */
    public final static String getHumanReadableDuration(final Long durationInNs, final boolean includeMs)
    {
        String result = null;
        
        if (durationInNs == null)
        {
            result = "<unknown>";
        }
        else
        {
            result = getHumanReadableDuration(durationInNs.longValue(), includeMs);
        }
        
        return(result);
    }
    
    
    /**
     * @param durationInNs A duration in nanoseconds (i.e. from System.nanoTime()).
     * @return A human readable string representing that duration as "Ud Vh Wm Xs Y.Zms".
     */
    public final static String getHumanReadableDuration(final long durationInNs)
    {
        return(getHumanReadableDuration(durationInNs, true));
    }
    
    
    /**
     * @param durationInNs A duration in nanoseconds (i.e. from System.nanoTime()).
     * @param includeMs    Flag indicating whether to include milliseconds or not.
     * @return A human readable string representing that duration as "Ud Vh Wm Xs Y.Zms".
     */
    public final static String getHumanReadableDuration(final long durationInNs, final boolean includeMs)
    {
        String result = null;
        
        if (durationInNs <= 0)
        {
            result = "0d 0h 0m 0s" + (includeMs ? " 0.0ms" : "");
        }
        else
        {
            int days         = (int)(durationInNs / NS_PER_DAY);
            int hours        = (int)((durationInNs / NS_PER_HOUR)        % 24);
            int minutes      = (int)((durationInNs / NS_PER_MINUTE)      % 60);
            int seconds      = (int)((durationInNs / NS_PER_SECOND)      % 60);
            int milliseconds = (int)((durationInNs / NS_PER_MILLISECOND) % 1000);
            int microseconds = (int)((durationInNs / NS_PER_MICROSECOND) % 1000);

            // Ternaries, how I love thee...  ;-)
            result = (days > 0                              ? days    + "d " : "") +
                     (days > 0 || hours > 0                 ? String.format("%02dh ", hours)   : "") +
                     (days > 0 || hours > 0 || minutes > 0  ? String.format("%02dm ", minutes) : "") +
                     String.format("%02ds", seconds) +
                     (includeMs ? " " + String.format("%03d.%03dms", milliseconds, microseconds) : "");
        }
        
        return(result);
    }
    
    
    public final static String getDurationInSeconds(final long durationInNs)
    {
        return((float)durationInNs / NS_PER_SECOND + "s");
    }
    
    
    public final static String dumpThread(final String threadName)
    {
        final StringBuilder result       = new StringBuilder();
        final ThreadMXBean  threadMXBean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[]  threadsInfo  = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
        
        for (final ThreadInfo threadInfo : threadsInfo)
        {
            if (threadName == null || threadName.equals(threadInfo.getThreadName()))
            {
                result.append("\nName: ");
                result.append(threadInfo.getThreadName());
                result.append("\nState: ");
                result.append(threadInfo.getThreadState());
                result.append("\nStack Trace:");
                
                for (final StackTraceElement stackTraceElement : threadInfo.getStackTrace())
                {
                    result.append("\n\t\tat ");
                    result.append(stackTraceElement);
                }
            }
        }
        
        return(result.toString());
    }
    
    
    // TRACE level methods
    public final static boolean trace(final Log log)
    {
        return(log.isTraceEnabled());
    }
    
    public final static void trace(final Log log, final String message)
    {
        log.trace(PREFIX + message);
    }
    
    public final static void trace(final Log log, final String message, final Throwable cause)
    {
        log.trace(PREFIX + message, cause);
    }

    public final static void trace(final Log log, final Throwable cause)
    {
        log.trace(RAW_EXCEPTION, cause);
    }

    // DEBUG level methods
    public final static boolean debug(final Log log)
    {
        return(log.isDebugEnabled());
    }
    
    public final static void debug(final Log log, final String message)
    {
        log.debug(PREFIX + message);
    }
    
    public final static void debug(final Log log, final String message, final Throwable cause)
    {
        log.debug(PREFIX + message, cause);
    }

    public final static void debug(final Log log, final Throwable cause)
    {
        log.debug(RAW_EXCEPTION, cause);
    }

    // INFO level methods
    public final static boolean info(final Log log)
    {
        return(log.isInfoEnabled());
    }
    
    public final static void info(final Log log, final String message)
    {
        log.info(PREFIX + message);
    }
    
    public final static void info(final Log log, final String message, final Throwable cause)
    {
        log.info(PREFIX + message, cause);
    }

    public final static void info(final Log log, final Throwable cause)
    {
        log.info(RAW_EXCEPTION, cause);
    }

    // WARN level methods
    public final static boolean warn(final Log log)
    {
        return(log.isWarnEnabled());
    }
    
    public final static void warn(final Log log, final String message)
    {
        log.warn(PREFIX + message);
    }
    
    public final static void warn(final Log log, final String message, final Throwable cause)
    {
        log.warn(PREFIX + message, cause);
    }

    public final static void warn(final Log log, final Throwable cause)
    {
        log.warn(RAW_EXCEPTION, cause);
    }

    // ERROR level methods
    public final static boolean error(final Log log)
    {
        return(log.isErrorEnabled());
    }
    
    public final static void error(final Log log, final String message)
    {
        log.error(PREFIX + message);
    }
    
    public final static void error(final Log log, final String message, final Throwable cause)
    {
        log.error(PREFIX + message, cause);
    }

    public final static void error(final Log log, final Throwable cause)
    {
        log.error(RAW_EXCEPTION, cause);
    }

    // FATAL level methods
    public final static boolean fatal(final Log log)
    {
        return(log.isFatalEnabled());
    }
    
    public final static void fatal(final Log log, final String message)
    {
        log.fatal(PREFIX + message);
    }
    
    public final static void fatal(final Log log, final String message, final Throwable cause)
    {
        log.fatal(PREFIX + message, cause);
    }

    public final static void fatal(final Log log, final Throwable cause)
    {
        log.fatal(RAW_EXCEPTION, cause);
    }
}
