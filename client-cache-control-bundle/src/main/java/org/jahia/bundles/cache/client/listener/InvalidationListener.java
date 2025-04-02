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
package org.jahia.bundles.cache.client.listener;

import org.apache.jackrabbit.spi.Event;
import org.jahia.services.content.DefaultEventListener;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.observation.EventIterator;
import java.util.stream.IntStream;

/**
 * @author Jerome Blanchard
 */
@Component(service = DefaultEventListener.class)
public class InvalidationListener extends DefaultEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidationListener.class);

    private static final int NODE_EVENTS = IntStream.of(Event.NODE_ADDED, Event.NODE_REMOVED, Event.NODE_MOVED).sum();
    private static final int PROPERTY_EVENTS = IntStream.of(Event.PROPERTY_ADDED, Event.PROPERTY_REMOVED, Event.PROPERTY_CHANGED).sum();

    @Override public int getEventTypes() {
        return NODE_EVENTS + PROPERTY_EVENTS;
    }

    @Override public void onEvent(EventIterator events) {
        // ${TODO} Auto-generated method stub
        CacheHelper.invalidateCache(events);
    }
}
