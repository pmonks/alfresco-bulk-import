/*
 * Copyright (C) 2007-2016 Peter Monks.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.alfresco.extension.bulkimport.util.LogUtils.*;
import static org.alfresco.extension.bulkimport.util.Utils.*;


/**
 * This class encapsulates the logic for pausing and resuming imports.
 *
 * @author Peter Monks (pmonks@gmail.com)
 */
public final class Pauser
{
    private final static Log log = LogFactory.getLog(Pauser.class);

    private final WritableBulkImportStatus importStatus;

    private volatile boolean       paused;
    private final    ReentrantLock pauseLock;
    private final    Condition     pauseCondition;


    public Pauser(final WritableBulkImportStatus importStatus)
    {
        // PRECONDITIONS
        assert importStatus != null : "importStatus must not be null.";

        // BODY
        this.importStatus = importStatus;

        this.paused         = false;
        this.pauseLock      = new ReentrantLock();
        this.pauseCondition = pauseLock.newCondition();
    }


    /**
     * Pause the current import.
     */
    public void pause()
    {
        // If scanning is not active, we're already paused (awaiting thread pool termination)
        if (importStatus.isScanning())
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

        importStatus.pauseRequested();
    }


    /**
     * Resume the current import.
     */
    public void resume()
    {
        importStatus.resumeRequested();

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

