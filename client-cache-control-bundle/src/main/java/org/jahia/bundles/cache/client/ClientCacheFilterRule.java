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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.regex.Pattern;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheFilterRule implements Comparable<ClientCacheFilterRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheFilterRule.class);

    private final String key;
    private int index = -1;
    private String name;
    private String description;
    private Pattern methodsPattern;
    private Pattern urlPattern;
    private ClientCacheHeaderTemplate headerTemplate;

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

    public void setMethodsPattern(String methodsPattern) {
        this.methodsPattern = Pattern.compile(methodsPattern);
    }

    public Pattern getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = Pattern.compile(urlPattern);
    }

    public ClientCacheHeaderTemplate getHeaderTemplate() {
        return headerTemplate;
    }

    public void setHeaderTemplate(String headerTemplate) {
        this.headerTemplate = ClientCacheHeaderTemplate.forName(headerTemplate);
    }

    public boolean isValid() {
        return index >= 0 && StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(name) && methodsPattern != null && urlPattern != null && headerTemplate != null;
    }

    @Override public String toString() {
        return "ClientCacheFilterRule{" + "key='" + key + '\'' + ", index=" + index + ", name='" + name + '\'' + ", description='"
                + description + '\'' + ", methodsPattern=" + methodsPattern + ", urlPattern=" + urlPattern + ", headerTemplate='"
                + headerTemplate + '\'' + '}';
    }

    @Override
    public int compareTo(ClientCacheFilterRule o) {
        return Integer.compare(this.index, o.index);
    }

    public static ClientCacheFilterRule build(String pid, Dictionary<String, ?> properties){
        LOGGER.debug("Building Client Cache Control rule for pid: {}, config size: {}", pid, properties.size());
        ClientCacheFilterRule config = new ClientCacheFilterRule(pid);
        String index = (String) properties.get("index");
        if (StringUtils.isNotEmpty(index)) {
            config.setIndex(Integer.parseInt(index));
        }
        String name = (String) properties.get("name");
        if (StringUtils.isNotEmpty(name)) {
            config.setName(name);
        }
        String description = (String) properties.get("description");
        if (StringUtils.isNotEmpty(description)) {
            config.setDescription(description);
        }
        String methodsPattern = (String) properties.get("methodsPattern");
        if (StringUtils.isNotEmpty(methodsPattern)) {
            config.setMethodsPattern(methodsPattern);
        }
        String urlPattern = (String) properties.get("urlPattern");
        if (StringUtils.isNotEmpty(urlPattern)) {
            config.setUrlPattern(urlPattern);
        }
        String headerTemplateName = (String) properties.get("headerTemplate");
        if (StringUtils.isNotEmpty(headerTemplateName)) {
            config.setHeaderTemplate(headerTemplateName);
        }
        return config;
    }
}
