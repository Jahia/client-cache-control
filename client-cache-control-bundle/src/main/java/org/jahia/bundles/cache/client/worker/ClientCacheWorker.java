/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.bundles.cache.client.worker;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.DelayQueue;

/**
 * @author Jerome Blanchard
 */
@Component(service = ClientCacheWorker.class, scope = ServiceScope.SINGLETON)
public class ClientCacheWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheWorker.class);

    private ClientCacheWorkerThread worker;
    private Thread workerThread;
    private DelayQueue<ClientCacheJob> queue;

    @Activate
    public void start() {
        if (workerThread != null && workerThread.isAlive()) {
            LOGGER.warn("ClientCache worker already started");
            return;
        }
        LOGGER.info("Starting ClientCache Worker thread");
        worker = new ClientCacheWorkerThread();
        queue = new DelayQueue<>();
        workerThread = new Thread(worker);
        workerThread.setName("ClientCache Worker Thread");
        workerThread.start();
    }

    @Deactivate
    public void stop() {
        LOGGER.info("Stopping ClientCache worker thread");
        worker.stop();
    }

    private class ClientCacheWorkerThread implements Runnable {

        private boolean run = true;

        public void stop() {
            this.run = false;
        }

        @Override
        public void run() {
            while (run) {
                try {
                    ClientCacheJob job = queue.take();
                    LOGGER.debug("Treating job: " + job);


                } catch (InterruptedException e) {
                    LOGGER.warn("interrupted while trying to take next job", e);
                }

            }
        }
    }

}
