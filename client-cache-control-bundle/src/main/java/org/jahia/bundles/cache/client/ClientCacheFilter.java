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
package org.jahia.bundles.cache.client;

import org.apache.http.HttpHeaders;
import org.jahia.bin.filters.AbstractServletFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

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
    public static final float FILTER_ORDER = -3f; //Just after URLRewriteFilter
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
        // Check if there is a rule that matches the request
        Optional<ClientCacheFilterRule> firstMatchingRule = service.getRules().stream()
                .filter(rule -> rule.getMethodsPattern().matcher(hRequest.getMethod()).matches()
                        && rule.getUrlPattern().matcher(hRequest.getRequestURI()).matches()).findFirst();
        // If a rule matches, preset the cache control header and set the policy attribute
        // Otherwise, set the default cache control header (PRIVATE)
        String presetCacheControlValue = "";
        if (firstMatchingRule.isPresent()) {
            LOGGER.debug("[{}] Found matching rule: {}", hRequest.getRequestURI(), firstMatchingRule.get().getName());
            request.setAttribute(ClientCacheService.CC_POLICY_ATTR, firstMatchingRule.get().getHeaderTemplate());
            presetCacheControlValue = service.getCacheControlHeaderTemplates().getOrDefault(firstMatchingRule.get().getHeaderTemplate().getName(),
                    service.getCacheControlHeaderTemplates().get(ClientCacheHeaderTemplate.PRIVATE.getName()));
            if (hResponseWrapper.containsHeader(HttpHeaders.CACHE_CONTROL)) {
                LOGGER.warn("[{}] Cache-Control header already set to value: [{}]", hRequest.getRequestURI(), hResponseWrapper.getHeader(HttpHeaders.CACHE_CONTROL));
            }
            hResponseWrapper.setHeader(HttpHeaders.CACHE_CONTROL, presetCacheControlValue);
            hResponseWrapper.setReadOnlyFilteredHeaders(!service.allowOverridesCacheControlHeader());
            LOGGER.debug("[{}] Predefining and Cache-Control: [{}]", hRequest.getRequestURI(), presetCacheControlValue);
        } else {
            presetCacheControlValue = service.getCacheControlHeaderTemplates().get(ClientCacheHeaderTemplate.PRIVATE.getName());
            hResponseWrapper.setHeader(HttpHeaders.CACHE_CONTROL, presetCacheControlValue);
            LOGGER.debug("[{}] Predefining DEFAULT Cache-Control: [{}]", hRequest.getRequestURI(), presetCacheControlValue);
        }
        chain.doFilter(request, hResponseWrapper);
        if (service.logOverridesCacheControlHeader() && !presetCacheControlValue.equals(hResponseWrapper.getHeader(HttpHeaders.CACHE_CONTROL))) {
            String currentCacheControlValue = hResponseWrapper.getHeader(HttpHeaders.CACHE_CONTROL) != null ? hResponseWrapper.getHeader(HttpHeaders.CACHE_CONTROL) : "Header Not Set";
            LOGGER.info("[{}] Cache-Control header overridden/removed by other component, current value: [{}] was preset to value: [{}]", hRequest.getRequestURI(), currentCacheControlValue, presetCacheControlValue);
        }
        if (LOGGER.isDebugEnabled()) {
            hResponseWrapper.getHeaderNames().forEach(headerName -> LOGGER.debug("[{}]  Final Header: [{}] Value: [{}]", hRequest.getRequestURI(), headerName, hResponseWrapper.getHeader(headerName)));
        }
    }

    @Override public void init(FilterConfig filterConfig) {
    }

    @Override public void destroy() {
    }

}
