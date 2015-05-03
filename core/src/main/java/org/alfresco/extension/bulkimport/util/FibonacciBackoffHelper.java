/*
 * Copyright (C) 2007-2015 Peter Monks.
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



/**
 * Helper class for Fibonacci back-off retry logic.  Would be ideal to encapsulate
 * the retry logic here as well, but Java is goddawful at functional logic.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public class FibonacciBackoffHelper
{
    // Fibonacci back-off - will sleep approximately this many milliseconds X 10 on each subsequent retry
    private final static int[] FIBONACCI_NUMBERS = new int[] { 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987 };
    private final static int   FIBONACCI_COUNT   = FIBONACCI_NUMBERS.length;

    
    /**
     * Sleeps for a variable number of milliseconds, based on the following
     * calculation:
     * 
     * <code>
     *     Fibonacci[retryNumber] X 10 X (random[0.0 -> 1.0] + 0.5)
     * </code>
     * 
     * So for example a retryNumber of 7 might result in a sleep of:
     * 
     * <code>
     *     21 X 10 X (0.6528 + 0.5) = 242ms
     * </code>
     * 
     * @param retryNumber
     * @throws InterruptedException
     */
    public static void backOffSleep(int retryNumber)
        throws InterruptedException
    {
        if (retryNumber >= 0)
        {
            if (retryNumber >= FIBONACCI_COUNT)
            {
                retryNumber = FIBONACCI_COUNT - 1;
            }
            
            // Add some random jitter to each sleep
            long sleepMillis = (long)(FIBONACCI_NUMBERS[retryNumber] * 10.0 * (Math.random() + 0.5));
            
            Thread.sleep(sleepMillis);
        }
    }

}
