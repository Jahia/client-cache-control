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

/**
 * @author Jerome Blanchard
 */
public enum ClientCacheHeaderTemplate {

    PRIVATE("private"),
    PUBLIC("public"),
    CUSTOM("custom"),
    IMMUTABLE("immutable");

    private final String name;

    ClientCacheHeaderTemplate(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ClientCacheHeaderTemplate forName(String name) {
        for (ClientCacheHeaderTemplate template : values()) {
            if (template.getName().equals(name)) {
                return template;
            }
        }
        return null;
    }

}
