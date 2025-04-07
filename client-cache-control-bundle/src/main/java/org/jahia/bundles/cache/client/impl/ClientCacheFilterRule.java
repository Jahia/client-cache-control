/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.jahia.bundles.cache.client.api.ClientCacheRule;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheFilterRule implements ClientCacheRule, Comparable<ClientCacheFilterRule> {

    public static final String RULE_PART_SEPARATOR = ";";
    public static final String TEMPLATE_PREFIX = "template:";

    private float priority = 0;
    private String ruleSetKey;
    private Set<String> methods;
    private String urlRegexp;
    private Pattern urlPattern;
    private String header;

    public ClientCacheFilterRule() {
    }

    public String getRuleSetKey() {
        return ruleSetKey;
    }

    public void setRuleSetKey(String ruleSetKey) {
        this.ruleSetKey = ruleSetKey;
    }

    @Override public float getPriority() {
        return priority;
    }

    public void setPriority(float priority) {
        this.priority = priority;
    }

    @Override public Set<String> getMethods() {
        return methods;
    }

    public void setMethods(Set<String> methods) {
        this.methods = methods;
    }

    @Override public String getUrlRegexp() {
        return urlRegexp;
    }

    public void setUrlRegexp(String urlRegexp) {
        this.urlRegexp = urlRegexp;
    }

    public Pattern getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(Pattern urlPattern) {
        this.urlPattern = urlPattern;
    }

    @Override
    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getHeaderTemplate() {
        return isHeaderTemplate()?getHeaderTemplateName():null;
    }

    public String getHeaderValue() {
        return isHeaderTemplate()?null:header;
    }

    private boolean isHeaderTemplate() {
        return header.startsWith(TEMPLATE_PREFIX);
    }

    private String getHeaderTemplateName() {
        return header.substring(TEMPLATE_PREFIX.length());
    }

    public boolean isValid() {
        return methods!= null && !methods.isEmpty() && urlPattern != null && StringUtils.isNotEmpty(header);
    }

    @Override public String toString() {
        return "RuleEntry{" + "priority='" + priority + '\'' + ", methods=" + methods + ", urlPattern=" + urlPattern
                + ", header='" + header + '\'' + '}';
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClientCacheFilterRule that = (ClientCacheFilterRule) o;
        return priority == that.priority && Objects.equals(ruleSetKey, that.ruleSetKey) && Objects.equals(methods, that.methods)
                && Objects.equals(urlRegexp, that.urlRegexp) && Objects.equals(header, that.header);
    }

    @Override public int hashCode() {
        return Objects.hash(priority, ruleSetKey, methods, urlRegexp, header);
    }

    public static ClientCacheFilterRule deserialize(String serialized) {
        ClientCacheFilterRule entry = new ClientCacheFilterRule();
        String[] parts = serialized.split(RULE_PART_SEPARATOR);
        if (parts.length != 4) {
            return entry;
        }
        entry.setPriority(Float.parseFloat(parts[0]));
        entry.setMethods(Set.of(StringUtils.split(parts[1], '|')));
        entry.setUrlRegexp(parts[2]);
        entry.setUrlPattern(Pattern.compile(entry.getUrlRegexp()));
        entry.setHeader(parts[3]);
        return entry;
    }

    @Override public int compareTo(ClientCacheFilterRule ruleEntry) {
        return Float.compare(this.priority, ruleEntry.priority);
    }
}
