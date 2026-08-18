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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Optional;
import java.util.regex.Pattern;

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
        Optional<String> presetCacheControlValue = service.getCacheControlHeader(hRequest.getMethod(), resolvedPath(hRequest), Collections.emptyMap());
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

    /**
     * The path the container resolved this request to, which is what the rules are matched against.
     * {@code getServletPath()} and {@code getPathInfo()} are decoded and normalized by the container, and
     * together they are the path it used to choose the servlet. Matching them keeps this filter's view of
     * the request identical to the one that decides which resource answers, so the policy describes the
     * response that is actually produced.
     *
     * <p>Where the container exposes neither, the request URI answers, canonicalized here: it is the one
     * path in this filter that nothing upstream has decoded or normalized. That is the only reason this
     * class carries {@link #canonicalize(String)} at all — on the resolved path it would be redundant
     * work, and a second reading of the request is what makes two components disagree about what a path
     * means.</p>
     */
    static String resolvedPath(HttpServletRequest request) {
        String servletPath = request.getServletPath() != null ? request.getServletPath() : "";
        String pathInfo = request.getPathInfo() != null ? request.getPathInfo() : "";
        String resolved = servletPath + pathInfo;
        return resolved.isEmpty() ? canonicalize(request.getRequestURI()) : resolved;
    }

    private static final Pattern DUPLICATE_SEPARATOR = Pattern.compile("/{2,}");

    /**
     * Rewrites a request URI to the single form that stands for every spelling of the same path:
     * percent-encoding decoded once, duplicate separators collapsed, dot segments removed.
     *
     * <p>Returns the argument unchanged when it is already canonical, which is the common case.</p>
     */
    static String canonicalize(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        String canonical = removeDotSegments(DUPLICATE_SEPARATOR.matcher(decodeOnce(uri)).replaceAll("/"));
        return canonical.equals(uri) ? uri : canonical;
    }

    /**
     * Percent-decodes once. A {@code %} that is not followed by two hexadecimal digits is left as it
     * stands, so a path that carries a literal percent sign is not corrupted. Decoding runs over the
     * collected bytes rather than character by character, so a multi-byte UTF-8 sequence written as
     * several percent groups decodes to the character it stands for.
     */
    private static String decodeOnce(String uri) {
        if (uri.indexOf('%') < 0) {
            return uri;
        }
        StringBuilder decoded = new StringBuilder(uri.length());
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        int i = 0;
        while (i < uri.length()) {
            if (uri.charAt(i) == '%' && i + 2 < uri.length() && isHex(uri.charAt(i + 1)) && isHex(uri.charAt(i + 2))) {
                pending.write(Integer.parseInt(uri.substring(i + 1, i + 3), 16));
                i += 3;
            } else {
                flush(pending, decoded);
                decoded.append(uri.charAt(i));
                i++;
            }
        }
        flush(pending, decoded);
        return decoded.toString();
    }

    private static void flush(ByteArrayOutputStream pending, StringBuilder out) {
        if (pending.size() > 0) {
            out.append(new String(pending.toByteArray(), StandardCharsets.UTF_8));
            pending.reset();
        }
    }

    private static boolean isHex(char c) {
        return Character.digit(c, 16) >= 0;
    }

    /**
     * Removes {@code .} and {@code ..} segments, following RFC 3986 section 5.2.4. A {@code ..} that
     * would climb above the root is dropped rather than applied.
     */
    private static String removeDotSegments(String path) {
        if (path.indexOf('.') < 0) {
            return path;
        }
        boolean rooted = path.startsWith("/");
        Deque<String> kept = new ArrayDeque<>();
        for (String segment : (rooted ? path.substring(1) : path).split("/", -1)) {
            if ("..".equals(segment)) {
                // A rooted path has no parent above its root, so a climb that would leave it is dropped
                // rather than applied. Leaving it in would make the canonical form name a path the
                // request could not reach.
                kept.pollLast();
            } else if (!".".equals(segment)) {
                kept.addLast(segment);
            }
        }
        String rebuilt = (rooted ? "/" : "") + String.join("/", kept);
        // A path whose last segment was a dot segment loses its trailing separator when it is rebuilt.
        if (path.endsWith("/") && !rebuilt.endsWith("/")) {
            rebuilt = rebuilt + "/";
        }
        return rebuilt.isEmpty() ? "/" : rebuilt;
    }

    @Override public void init(FilterConfig filterConfig) {
        // Nothing special to init here
    }

    @Override public void destroy() {
        // Nothing to do when destroy
    }

}
