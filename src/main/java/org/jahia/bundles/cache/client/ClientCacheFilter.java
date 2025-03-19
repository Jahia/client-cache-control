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
        HttpServletResponse hResponse = (HttpServletResponse) response;
        LOGGER.debug("{} {} Entering Cache Control preset filter", hRequest.getMethod(), hRequest.getRequestURI());
        hRequest.setAttribute(ClientCacheService.CC_ORIGINAL_REQUEST_URI_ATTR, hRequest.getRequestURI());
        Optional<ClientCacheFilterRule> firstMatchingRule = service.getPolicies().stream()
                .filter(rule -> rule.getMethodsPattern().matcher(hRequest.getMethod()).matches()
                        && rule.getUrlPattern().matcher(hRequest.getRequestURI()).matches()).findFirst();
        if (firstMatchingRule.isPresent()) {
            request.setAttribute(ClientCacheService.CC_POLICY_ATTR, firstMatchingRule.get().getClientCachePolicy());
            String presetCacheControlValue = service.getCacheControlValues().getOrDefault(firstMatchingRule.get().getClientCachePolicy(),
                    service.getCacheControlValues().get(ClientCacheService.CC_POLICY_PRIVATE));
            hResponse.setHeader(HttpHeaders.CACHE_CONTROL, presetCacheControlValue);
            LOGGER.debug("[{}] Predefining client cache control for rule: {}", hRequest.getRequestURI(), firstMatchingRule.get().getName());
        } else {
            hResponse.setHeader(HttpHeaders.CACHE_CONTROL, service.getCacheControlValues().get(ClientCacheService.CC_POLICY_PRIVATE));
            LOGGER.debug("[{}] Predefining DEFAULT client cache control", hRequest.getRequestURI());
        }
        chain.doFilter(request, response);
        LOGGER.debug("{} {}, [{}] Final Cache-Control: {}",  hResponse.getStatus(), hRequest.getMethod(), hRequest.getRequestURI(), hResponse.getHeader(HttpHeaders.CACHE_CONTROL));
    }

    @Override public void init(FilterConfig filterConfig) {
    }

    @Override public void destroy() {
    }

}
