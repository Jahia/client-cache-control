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

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheFilterRule implements Comparable<ClientCacheFilterRule> {

    private final String key;
    private int index = -1;
    private String name;
    private String description;
    private Pattern methodsPattern;
    private Pattern urlPattern;
    private String clientCachePolicy;

    public ClientCacheFilterRule(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Pattern getMethodsPattern() {
        return methodsPattern;
    }

    public void setMethodsPattern(Pattern methodsPattern) {
        this.methodsPattern = methodsPattern;
    }

    public Pattern getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(Pattern urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getClientCachePolicy() {
        return clientCachePolicy;
    }

    public void setClientCachePolicy(String clientCachePolicy) {
        this.clientCachePolicy = clientCachePolicy;
    }

    public boolean isValid() {
        return index >= 0 && StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(name) && methodsPattern != null && urlPattern != null && StringUtils.isNotEmpty(clientCachePolicy);
    }

    @Override public String toString() {
        return "ClientCacheFilterRule{" + "key='" + key + '\'' + ", index=" + index + ", name='" + name + '\'' + ", description='"
                + description + '\'' + ", methodsPattern=" + methodsPattern + ", urlPattern=" + urlPattern + ", clientCachePolicy='"
                + clientCachePolicy + '\'' + '}';
    }

    @Override
    public int compareTo(ClientCacheFilterRule o) {
        return Integer.compare(this.index, o.index);
    }
}
