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

import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Jerome Blanchard
 */
public abstract class ClientCacheRule {

    /**
     * The priority of the rule. Lower values have higher priority.
     *
     * @return the priority
     */
    abstract public float getPriority();

    /**
     * A set of methods that the rule can be applied.
     *
     * @return the set of methods names
     */
    abstract public Set<String> getMethods();

    /**
     * A URL regular expression that the rule can be applied to.
     *
     * @return the regexp
     */
    abstract public String getUrlRegexp();

    /**
     * A cache control header
     * The header can be either a reference to a ClientCacheTemplate (template:{name})
     * nor a definitive Cache-Control header value to apply asis.
     *
     * @return the header value
     */
    abstract public String getHeader();

}
