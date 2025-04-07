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

    private int priority = 0;
    private String ruleSetKey;
    private Set<String> methods;
    private String urlPatternString;
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

    @Override public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override public Set<String> getMethods() {
        return methods;
    }

    public void setMethods(Set<String> methods) {
        this.methods = methods;
    }

    public String getUrlPatternString() {
        return urlPatternString;
    }

    public void setUrlPatternString(String urlPatternString) {
        this.urlPatternString = urlPatternString;
    }

    @Override public Pattern getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(Pattern urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getHeader() {
        return header;
    }

    @Override public String getHeaderTemplate() {
        return isHeaderTemplate()?getHeaderTemplateName():null;
    }

    @Override public String getHeaderValue() {
        return isHeaderTemplate()?null:header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    private boolean isHeaderTemplate() {
        return header.startsWith(TEMPLATE_PREFIX);
    }

    private String getHeaderTemplateName() {
        return header.substring(TEMPLATE_PREFIX.length());
    }

    public boolean isValid() {
        return priority > 0 && !methods.isEmpty() && urlPattern != null && StringUtils.isNotEmpty(header);
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
                && Objects.equals(urlPatternString, that.urlPatternString) && Objects.equals(header, that.header);
    }

    @Override public int hashCode() {
        return Objects.hash(priority, ruleSetKey, methods, urlPatternString, header);
    }

    public static ClientCacheFilterRule deserialize(String serialized) {
        ClientCacheFilterRule entry = new ClientCacheFilterRule();
        String[] parts = serialized.split(RULE_PART_SEPARATOR);
        if (parts.length != 4) {
            return entry;
        }
        entry.setPriority(Integer.parseInt(parts[0]));
        entry.setMethods(Set.of(StringUtils.split(parts[1], '|')));
        entry.setUrlPatternString(parts[2]);
        entry.setUrlPattern(Pattern.compile(entry.getUrlPatternString()));
        entry.setHeader(parts[3]);
        return entry;
    }

    @Override public int compareTo(ClientCacheFilterRule ruleEntry) {
        int segmentCount1 = this.urlPatternString.split("/").length;
        int segmentCount2 = ruleEntry.urlPatternString.split("/").length;
        if (segmentCount1 != segmentCount2) {
            return Integer.compare(segmentCount2, segmentCount1);
        }

        int wildcardCount1 = (int) this.urlPatternString.chars().filter(ch -> ch == '.').count();
        int wildcardCount2 = (int) ruleEntry.urlPatternString.chars().filter(ch -> ch == '.').count();
        if (wildcardCount1 != wildcardCount2) {
            return Integer.compare(wildcardCount1, wildcardCount2);
        }

        return Integer.compare(this.urlPatternString.length(), ruleEntry.urlPatternString.length());
    }
}
