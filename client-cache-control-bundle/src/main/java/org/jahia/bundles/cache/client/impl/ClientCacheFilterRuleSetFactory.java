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

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Jerome Blanchard
 */
@Component(service = { ClientCacheFilterRuleSetFactory.class, ManagedServiceFactory.class}, immediate = true, property = "service.pid=org.jahia.bundles.cache.client.ruleset")
public class ClientCacheFilterRuleSetFactory implements ManagedServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheFilterRuleSetFactory.class);

    private final List<ClientCacheFilterRule> rules = new LinkedList<>();

    public ClientCacheFilterRuleSetFactory() {
        LOGGER.debug("Creating Client Cache Control RuleSet Factory");
    }

    @Override
    public String getName() {
        return "Client Cache Control RuleSet Factory";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        LOGGER.info("Updating ruleset with key: {}, config size: {}", pid, properties.size());
        ClientCacheFilterRuleSet ruleset = ClientCacheFilterRuleSet.build(pid, properties);
        this.rules.removeIf(r -> r.getRuleSetKey().equals(ruleset.getKey()));
        this.rules.addAll(ruleset.getRules());
        this.sortRules();
    }

    @Override
    public void deleted(String pid) {
        LOGGER.info("Deleting Client Cache Control rule for pid: {}", pid);
        this.rules.removeIf(r -> r.getRuleSetKey().equals(pid));
        this.sortRules();
    }

    private void sortRules() {
        this.rules.sort(ClientCacheFilterRule::compareTo);
        LOGGER.info("Current active rule's entries (sorted):");
        this.rules.forEach(rule -> LOGGER.info("{}", rule));
    }

    public List<ClientCacheFilterRule> getRules() {
        return this.rules;
    }

}
