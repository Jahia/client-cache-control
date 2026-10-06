/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.bundles.cache.client.filter;

import org.apache.http.HttpHeaders;
import org.jahia.bundles.cache.client.api.ClientCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheResponseWrapper extends HttpServletResponseWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheResponseWrapper.class);

    /**
     * A component that knows the final Cache-Control, but not its value, sets this header to the name
     * of a cache policy (a {@code ClientCachePolicy} level such as {@code private}). The wrapper
     * resolves that name to the configured template, writes {@code Cache-Control}, and never emits
     * this header. The resolution runs when the header is set, so it applies before the response
     * commits, which lets a streamed response carry the resolved value.
     */
    public static final String CLIENT_CACHE_POLICY_HEADER = "X-Jahia-Internal-Cache-Policy";

    private boolean readOnlyFilteredHeaders = false;
    private final List<String> filteredHeadersNames = List.of(HttpHeaders.CACHE_CONTROL, HttpHeaders.EXPIRES, HttpHeaders.PRAGMA);
    private final transient ClientCacheService service;

    public ClientCacheResponseWrapper(HttpServletResponse response) {
        this(response, null);
    }

    public ClientCacheResponseWrapper(HttpServletResponse response, ClientCacheService service) {
        super(response);
        this.service = service;
    }

    public void setReadOnlyFilteredHeaders(boolean readOnlyFilteredHeaders) {
        this.readOnlyFilteredHeaders = readOnlyFilteredHeaders;
    }

    @Override public void addHeader(String name, String value) {
        if (CLIENT_CACHE_POLICY_HEADER.equalsIgnoreCase(name)) {
            applyNamedPolicy(value);
            return;
        }
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
        if (CLIENT_CACHE_POLICY_HEADER.equalsIgnoreCase(name)) {
            applyNamedPolicy(value);
            return;
        }
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

    /**
     * Resolve a cache policy name to its template and write it as {@code Cache-Control}. The value is
     * written through the read-only guard, as the {@code Force-} override is, so a named policy also
     * applies in strict mode. An unknown name leaves the header unchanged.
     */
    private void applyNamedPolicy(String policyName) {
        if (service == null) {
            LOGGER.debug("No cache service bound; ignoring policy header value {}", policyName);
            return;
        }
        Optional<String> cacheControl = service.getCacheControlHeader(policyName, Collections.emptyMap());
        if (cacheControl.isPresent()) {
            super.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl.get());
        } else {
            LOGGER.warn("Unknown cache policy {} requested; leaving Cache-Control unchanged", policyName);
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
