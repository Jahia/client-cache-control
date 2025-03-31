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
package org.jahia.bundles.cache.client.filter;

import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheResponseWrapper extends HttpServletResponseWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheResponseWrapper.class);

    private boolean readOnlyFilteredHeaders = false;
    private final List<String> filteredHeadersNames = List.of(HttpHeaders.CACHE_CONTROL, HttpHeaders.EXPIRES, HttpHeaders.PRAGMA);

    public ClientCacheResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    public void setReadOnlyFilteredHeaders(boolean readOnlyFilteredHeaders) {
        this.readOnlyFilteredHeaders = readOnlyFilteredHeaders;
    }

    @Override public void addHeader(String name, String value) {
        if (filteredHeadersNames.contains(name)) {
            if (!readOnlyFilteredHeaders) {
                LOGGER.debug("Setting filtered header {} with value {}", name, value);
                super.setHeader(name, value);
            } else {
                LOGGER.debug("Ignoring filtered header {} with value {}", name, value);
            }
        } else {
            super.addHeader(name, value);
        }
    }

    @Override public void setHeader(String name, String value) {
        if (name.startsWith("Force-")) {
            LOGGER.debug("Overriding header {} with value {}", name, value);
            super.setHeader(name.substring("Force-".length()), value);
        } else if (filteredHeadersNames.contains(name)) {
            if (!readOnlyFilteredHeaders) {
                LOGGER.debug("Setting filtered header {} with value {}", name, value);
                super.setHeader(name, value);
            } else {
                LOGGER.debug("Ignoring filtered header {} with value {}", name, value);
            }
        } else {
            super.setHeader(name, value);
        }
    }

    @Override public void reset() {
        Map<String, String> readOnlyHeaders = new HashMap<>();
        if (readOnlyFilteredHeaders) {
            filteredHeadersNames.stream().filter(super::containsHeader)
                    .map(name -> Map.entry(name, super.getHeader(name)))
                    .forEach(entry -> readOnlyHeaders.put(entry.getKey(), entry.getValue()));
        }
        super.reset();
        if (readOnlyFilteredHeaders) {
            readOnlyHeaders.forEach(super::setHeader);
        }
    }
}
