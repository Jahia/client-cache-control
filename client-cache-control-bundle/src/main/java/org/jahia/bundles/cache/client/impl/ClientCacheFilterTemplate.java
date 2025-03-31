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
package org.jahia.bundles.cache.client.impl;

import org.jahia.bundles.cache.client.api.ClientCacheTemplate;

import java.util.Map;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheFilterTemplate implements ClientCacheTemplate {

    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";
    public static final String CUSTOM = "custom";
    public static final String IMMUTABLE = "immutable";
    public static final String DEFAULT = PRIVATE;

    public static final ClientCacheFilterTemplate EMPTY = new ClientCacheFilterTemplate("empty", "");

    private String name;
    private String template;

    public ClientCacheFilterTemplate(String name, String template) {
        this.name = name;
        this.template = template;
    }

    @Override public String getName() {
        return name;
    }

    @Override public String getTemplate() {
        return template;
    }

    @Override public String toString() {
        return "ClientCacheFilterTemplate{" + "name='" + name + '\'' + ", template='" + template + '\'' + '}';
    }

    public String getFilteredTemplate(Map<String, String> params) {
        String filteredTemplate = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            filteredTemplate = filteredTemplate.replaceAll("%%" + entry.getKey() + "%%", entry.getValue());
        }
        return filteredTemplate;
    }
}
