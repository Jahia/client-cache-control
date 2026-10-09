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
import org.jahia.services.render.filter.cache.ClientCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
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
    private static final String FORCE_PREFIX = "Force-";

    private boolean readOnlyFilteredHeaders = false;
    private final List<String> filteredHeadersNames = List.of(HttpHeaders.CACHE_CONTROL, HttpHeaders.EXPIRES, HttpHeaders.PRAGMA);
    private final ServletRequest request;
    private final ClientCacheService service;
    private ClientCachePolicy presetPolicy = ClientCachePolicy.DEFAULT;
    private boolean requestPolicyResolved = false;
    private String requestPolicyValue;
    private boolean cacheHeadersFinal = false;
    private String replacedCacheControl;

    public ClientCacheResponseWrapper(HttpServletResponse response) {
        this(response, null, null);
    }

    public ClientCacheResponseWrapper(HttpServletResponse response, ServletRequest request, ClientCacheService service) {
        super(response);
        this.request = request;
        this.service = service;
    }

    public void setReadOnlyFilteredHeaders(boolean readOnlyFilteredHeaders) {
        this.readOnlyFilteredHeaders = readOnlyFilteredHeaders;
    }

    /**
     * The policy of the preset: the most permissive policy this response can get. A policy contributed by the
     * request applies only when it is stricter, as a policy computed by the render chain only ever gets
     * stricter.
     */
    public void setPresetPolicy(ClientCachePolicy presetPolicy) {
        this.presetPolicy = presetPolicy != null ? presetPolicy : ClientCachePolicy.DEFAULT;
    }

    /**
     * The Cache-Control value the policy contributed by the request resolved to, or empty when the request
     * contributed none, or none stricter than the preset policy.
     */
    public Optional<String> getRequestPolicyValue() {
        return Optional.ofNullable(requestPolicyValue);
    }

    /**
     * The Cache-Control value the request policy replaced, or empty when no request policy was applied. When it
     * differs from the preset, a component had set it.
     */
    public Optional<String> getReplacedCacheControl() {
        return Optional.ofNullable(replacedCacheControl);
    }

    /**
     * Apply the {@link ClientCachePolicy} contributed in the {@link ClientCacheService#CC_REQUEST_POLICY_ATTR}
     * request attribute. Headers are frozen once the response commits, so this runs at each point that can
     * commit it, and once more when the filter chain returns. Only the first call that finds the response
     * uncommitted resolves the policy.
     */
    public void applyRequestPolicy() {
        if (requestPolicyResolved || request == null || service == null) {
            return;
        }
        if (isCommitted()) {
            LOGGER.debug("Response committed before the request policy could be applied");
            return;
        }
        requestPolicyResolved = true;
        Object attribute = request.getAttribute(ClientCacheService.CC_REQUEST_POLICY_ATTR);
        if (!(attribute instanceof ClientCachePolicy)) {
            return;
        }
        ClientCachePolicy policy = (ClientCachePolicy) attribute;
        if (!policy.isStronger(presetPolicy)) {
            LOGGER.debug("Request policy {} is not stricter than the preset policy {}, keeping the preset", policy, presetPolicy);
            return;
        }
        Optional<String> value = service.getCacheControlHeader(policy.getLevel().getValue(),
                Map.of(ClientCacheService.CC_CUSTOM_TTL_ATTR, Integer.toString(policy.getTtl())));
        if (value.isPresent()) {
            LOGGER.debug("Applying request policy {}: {}", policy, value.get());
            replaceCacheControl(value.get());
            // Nothing is stricter than private, so no later write can have a reason to change it.
            cacheHeadersFinal = policy.getLevel() == ClientCachePolicy.Level.PRIVATE;
        } else {
            LOGGER.warn("Unable to find cache control value for request policy level: {}", policy.getLevel().getValue());
        }
    }

    private void replaceCacheControl(String value) {
        replacedCacheControl = super.getHeader(HttpHeaders.CACHE_CONTROL);
        requestPolicyValue = value;
        super.setHeader(HttpHeaders.CACHE_CONTROL, value);
    }

    @Override public ServletOutputStream getOutputStream() throws IOException {
        applyRequestPolicy();
        return super.getOutputStream();
    }

    @Override public PrintWriter getWriter() throws IOException {
        applyRequestPolicy();
        return super.getWriter();
    }

    @Override public void flushBuffer() throws IOException {
        applyRequestPolicy();
        super.flushBuffer();
    }

    /**
     * An error, or a temporary redirect, can depend on who asks. Such a response is private, whatever the preset
     * and the policy the request contributed, and its cache headers are final.
     */
    private void applyPrivatePolicy() {
        if (service == null || isCommitted()) {
            return;
        }
        Optional<String> value = service.getCacheControlHeader(ClientCachePolicy.PRIVATE.getLevel().getValue(), Collections.emptyMap());
        if (value.isPresent()) {
            requestPolicyResolved = true;
            replaceCacheControl(value.get());
            cacheHeadersFinal = true;
        } else {
            LOGGER.warn("Unable to find cache control value for policy level: {}", ClientCachePolicy.PRIVATE.getLevel().getValue());
        }
    }

    @Override public void sendError(int sc) throws IOException {
        applyPrivatePolicy();
        super.sendError(sc);
    }

    @Override public void sendError(int sc, String msg) throws IOException {
        applyPrivatePolicy();
        super.sendError(sc, msg);
    }

    @Override public void sendRedirect(String location) throws IOException {
        applyPrivatePolicy();
        super.sendRedirect(location);
    }

    @Override public void addHeader(String name, String value) {
        if (isFiltered(name)) {
            if (!isReadOnly()) {
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
        if (name.regionMatches(true, 0, FORCE_PREFIX, 0, FORCE_PREFIX.length())) {
            if (cacheHeadersFinal && isFiltered(name.substring(FORCE_PREFIX.length()))) {
                LOGGER.debug("Ignoring header {} with value {}, the cache headers of this response are final", name, value);
                return;
            }
            LOGGER.debug("Overriding header {} with value {}", name, value);
            super.setHeader(name.substring(FORCE_PREFIX.length()), value);
        } else if (isFiltered(name)) {
            if (!isReadOnly()) {
                LOGGER.debug("Setting filtered header {} with value {}", name, value);
                super.setHeader(name, value);
            } else {
                LOGGER.debug("Ignoring filtered header {} with value {}", name, value);
            }
        } else {
            super.setHeader(name, value);
        }
    }

    @Override public void setDateHeader(String name, long date) {
        if (isFiltered(name) && isReadOnly()) {
            LOGGER.debug("Ignoring filtered header {} with value {}", name, date);
        } else {
            super.setDateHeader(name, date);
        }
    }

    @Override public void addDateHeader(String name, long date) {
        if (isFiltered(name) && isReadOnly()) {
            LOGGER.debug("Ignoring filtered header {} with value {}", name, date);
        } else if (isFiltered(name)) {
            super.setDateHeader(name, date);
        } else {
            super.addDateHeader(name, date);
        }
    }

    @Override public void setIntHeader(String name, int value) {
        if (isFiltered(name) && isReadOnly()) {
            LOGGER.debug("Ignoring filtered header {} with value {}", name, value);
        } else {
            super.setIntHeader(name, value);
        }
    }

    @Override public void addIntHeader(String name, int value) {
        if (isFiltered(name) && isReadOnly()) {
            LOGGER.debug("Ignoring filtered header {} with value {}", name, value);
        } else if (isFiltered(name)) {
            super.setIntHeader(name, value);
        } else {
            super.addIntHeader(name, value);
        }
    }

    @Override public void reset() {
        Map<String, String> readOnlyHeaders = new HashMap<>();
        if (isReadOnly()) {
            filteredHeadersNames.stream().filter(super::containsHeader)
                    .map(name -> Map.entry(name, super.getHeader(name)))
                    .forEach(entry -> readOnlyHeaders.put(entry.getKey(), entry.getValue()));
        }
        super.reset();
        if (isReadOnly()) {
            readOnlyHeaders.forEach(super::setHeader);
        }
    }

    private boolean isReadOnly() {
        return readOnlyFilteredHeaders || cacheHeadersFinal;
    }

    /** Header names are case-insensitive (RFC 9110, section 5.1). */
    private boolean isFiltered(String name) {
        return name != null && filteredHeadersNames.stream().anyMatch(name::equalsIgnoreCase);
    }
}
