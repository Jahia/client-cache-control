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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheFilterRuleSet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheFilterRuleSet.class);

    private final String key;
    private String name;
    private String description;
    private Set<ClientCacheFilterRule> rules;

    public ClientCacheFilterRuleSet(String key) {
        this.key = key;
        this.rules = new HashSet<>();
    }

    public String getKey() {
        return key;
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

    public Set<ClientCacheFilterRule> getRules() {
        return rules;
    }

    public void setRules(Set<ClientCacheFilterRule> rules) {
        this.rules = rules;
    }

    public void addRule(ClientCacheFilterRule rule) {
        if (rule.isValid()) {
            rule.setRuleSetKey(this.key);
            this.rules.add(rule);
        } else {
            LOGGER.error("Invalid rule: {}", rule);
        }
    }

    public static ClientCacheFilterRuleSet build(String pid, Dictionary<String, ?> properties){
        LOGGER.debug("Building Client Cache Control ruleset for pid: {}, config size: {}", pid, properties.size());
        ClientCacheFilterRuleSet ruleset = new ClientCacheFilterRuleSet(pid);
        String name = (String) properties.get("name");
        if (StringUtils.isNotEmpty(name)) {
            ruleset.setName(name);
        }
        String description = (String) properties.get("description");
        if (StringUtils.isNotEmpty(description)) {
            ruleset.setDescription(description);
        }
        properties.keys().asIterator().forEachRemaining(key -> {
            if (key.startsWith("rule")) {
                String rule = (String) properties.get(key);
                if (StringUtils.isNotEmpty(rule)) {
                    ruleset.addRule(ClientCacheFilterRule.deserialize(rule));
                }
            }
        });
        return ruleset;
    }

}
