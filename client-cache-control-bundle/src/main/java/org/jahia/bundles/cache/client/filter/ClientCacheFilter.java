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
import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.bundles.cache.client.api.ClientCacheMode;
import org.jahia.bundles.cache.client.api.ClientCacheService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Rules to apply preset Client Cache Control Policies based on URL patterns.
 * The URL patterns are defined in the OSGi configuration and takes into account the order of the rules.
 * The URL are rewritten BEFORE that filter apply.
 *
 * @author Jerome Blanchard
 */
@Component(service = { AbstractServletFilter.class}, property = { "pattern=/*" }, immediate = true)
public class ClientCacheFilter extends AbstractServletFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheFilter.class);

    public static final String FILTER_NAME = "ClientCacheFilter";
    public static final float FILTER_ORDER = -3f;
    public static final boolean FILTER_MATCH_ALL_URLS = true;

    private ClientCacheService service;

    @Reference(service = ClientCacheService.class)
    public void setService(ClientCacheService service) {
        this.service = service;
    }

    @Activate
    public void activate() {
        LOGGER.debug("Activating Filter...");
        this.setOrder(FILTER_ORDER);
        this.setFilterName(FILTER_NAME);
        this.setMatchAllUrls(FILTER_MATCH_ALL_URLS);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest hRequest = (HttpServletRequest) request;
        ClientCacheResponseWrapper hResponseWrapper = new ClientCacheResponseWrapper((HttpServletResponse) response);
        LOGGER.debug("{} {} Entering Cache Control preset filter", hRequest.getMethod(), hRequest.getRequestURI());
        hRequest.setAttribute(ClientCacheService.CC_ORIGINAL_REQUEST_URI_ATTR, hRequest.getRequestURI());
        boolean defaultPreset = false;
        Optional<String> presetCacheControlValue = service.getCacheControlHeader(hRequest.getMethod(), hRequest.getRequestURI(), Collections.emptyMap());
        if (presetCacheControlValue.isPresent()) {
            hResponseWrapper.setHeader(HttpHeaders.CACHE_CONTROL, presetCacheControlValue.get());
            if (service.getMode().equals(ClientCacheMode.STRICT)) {
                // Strict mode prevent any further modification of cache headers, even if response.reset() is called).
                hResponseWrapper.setReadOnlyFilteredHeaders(true);
                hRequest.setAttribute(ClientCacheService.CC_SET_ATTR, "done"); // Most legacy rewrite rules use that attribute as condition.
            }
            LOGGER.debug("[{}] Predefining Cache-Control: [{}]", hRequest.getRequestURI(), presetCacheControlValue);
        } else if (!hResponseWrapper.containsHeader(HttpHeaders.CACHE_CONTROL)) {
            // Using the default preset when service did not find rule for that request.
            String defaultCacheControlValue = service.getDefaultCacheControlHeader();
            hResponseWrapper.setHeader(HttpHeaders.CACHE_CONTROL, defaultCacheControlValue);
            defaultPreset = true;
            LOGGER.debug("[{}] Predefining DEFAULT Cache-Control: [{}]", hRequest.getRequestURI(), defaultCacheControlValue);
        } else {
            LOGGER.warn("[{}] Cache-Control header unchanged: [{}]", hRequest.getRequestURI(), hResponseWrapper.getHeader(HttpHeaders.CACHE_CONTROL));
        }
        chain.doFilter(request, hResponseWrapper);
        if (!defaultPreset && presetCacheControlValue.isPresent() && !(presetCacheControlValue.get()).equals(hResponseWrapper.getHeader(HttpHeaders.CACHE_CONTROL))) {
            String currentCacheControlValue = hResponseWrapper.getHeader(HttpHeaders.CACHE_CONTROL) != null ? hResponseWrapper.getHeader(HttpHeaders.CACHE_CONTROL) : "Header Not Set";
            if (service.getMode().equals(ClientCacheMode.ALLOW_OVERRIDES)) {
                LOGGER.debug("[{}] Cache-Control header overridden by other component, current value: [{}] was preset to value: [{}]", hRequest.getRequestURI(), currentCacheControlValue, presetCacheControlValue);
            } else {
                LOGGER.error("[{}] Cache-Control header overridden/removed by other component whereas strict mode configured, current value: [{}] was preset to value: [{}]", hRequest.getRequestURI(), currentCacheControlValue, presetCacheControlValue);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            hResponseWrapper.getHeaderNames().forEach(headerName -> LOGGER.debug("[{}]  Final Header: [{}] Value: [{}]", hRequest.getRequestURI(), headerName, hResponseWrapper.getHeader(headerName)));
        }
    }

    @Override public void init(FilterConfig filterConfig) {
        // Nothing special to init here
    }

    @Override public void destroy() {
        // Nothing to do when destroy
    }

}
