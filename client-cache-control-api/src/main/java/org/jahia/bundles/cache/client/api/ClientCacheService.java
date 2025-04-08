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
package org.jahia.bundles.cache.client.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Jerome Blanchard
 */
public interface ClientCacheService {

    String CC_SET_ATTR = "jahiaCacheControlSet";
    String CC_ORIGINAL_REQUEST_URI_ATTR = "jahiaOriginalRequestURI";
    String CC_CUSTOM_TTL_ATTR = "jahiaClientCacheCustomTTL";

    ClientCacheMode getMode();

    List<? extends ClientCacheRule> listRules();

    Collection<? extends ClientCacheTemplate> listHeaderTemplates();

    String getCacheControlHeader(String method, String uri, Map<String, String> templateParams);

    String getCacheControlHeader(String template, Map<String, String> templateParams);
}
