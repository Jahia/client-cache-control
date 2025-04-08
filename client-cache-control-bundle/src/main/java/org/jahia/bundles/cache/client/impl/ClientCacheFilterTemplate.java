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
package org.jahia.bundles.cache.client.impl;

import org.jahia.bundles.cache.client.api.ClientCacheTemplate;

import java.util.Map;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheFilterTemplate implements ClientCacheTemplate {

    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";
    public static final String PUBLIC_MEDIUM = "public-medium";
    public static final String CUSTOM = "custom";
    public static final String IMMUTABLE = "immutable";
    public static final String DEFAULT = IMMUTABLE;

    public static final ClientCacheFilterTemplate EMPTY = new ClientCacheFilterTemplate("empty", "");

    private final String name;
    private final String template;

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
