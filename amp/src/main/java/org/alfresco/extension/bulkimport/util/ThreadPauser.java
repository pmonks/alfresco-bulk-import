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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/**
 * This class encapsulates the logic for pausing and resuming imports.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public final class ThreadPauser
{
    private final static Log log = LogFactory.getLog(ThreadPauser.class);

    private volatile boolean       paused         = false;
    private final    ReentrantLock pauseLock      = new ReentrantLock();
    private final    Condition     pauseCondition = pauseLock.newCondition();


    /**
     * @return The current pause state.
     */
    public boolean isPaused()
    {
        return(paused);
    }


    /**
     * Pause the current import.
     */
    public void pause()
    {
        if (!paused)
        {
            pauseLock.lock();

            try
            {
                paused = true;
            }
            finally
            {
                pauseLock.unlock();
            }
        }
    }


    /**
     * Resume the current import.
     */
    public void resume()
    {
        if (paused)
        {
            pauseLock.lock();

            try
            {
                paused = false;
                pauseCondition.signalAll();
            }
            finally
            {
                pauseLock.unlock();
            }
        }
    }


    /**
     * Blocks the current thread if an import is paused.
     * @throws InterruptedException
     */
    public void blockIfPaused()
        throws InterruptedException
    {
        if (paused)
        {
            pauseLock.lock();

            try
            {
                while (paused) pauseCondition.await();
            }
            finally
            {
                pauseLock.unlock();
            }
        }
    }

}

