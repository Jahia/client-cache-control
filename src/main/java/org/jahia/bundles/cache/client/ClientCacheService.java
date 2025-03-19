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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Jerome Blanchard
 */
@Component(service = {ClientCacheService.class}, configurationPid = "org.jahia.bundles.cache.client", immediate = true)
public class ClientCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheService.class);

    private static final String CC_POLICY_PRIVATE_VALUE = "private, no-cache, no-store, must-revalidate, proxy-revalidate, max-age=0";
    private static final String CC_POLICY_CUSTOM_VALUE = "public, must-revalidate, max-age=##expires.maxage.ttl##, s-maxage=%%jahiaClientCacheCustomTTL%%, stale-while-revalidate=##expires.stale.ttl##";
    private static final String CC_POLICY_PUBLIC_VALUE = "public, must-revalidate, max-age=##public.maxage.ttl##, s-maxage=##public.smaxage.ttl##, stale-while-revalidate=##public.stale.ttl##";
    private static final String CC_POLICY_IMMUTABLE_VALUE = "public, max-age=##immutable.maxage.ttl##, s-maxage=##immutable.smaxage.ttl##, stale-while-revalidate=##immutable.stale.ttl##, immutable";

    public static final String CC_POLICY_ATTR = "jahiaClientCachePolicy";
    public static final String CC_ORIGINAL_REQUEST_URI_ATTR = "jahiaOriginalRequestURI";
    public static final String CC_CUSTOM_TTL_ATTR = "jahiaClientCacheCustomTTL";

    public static final String CC_POLICY_PUBLIC = "public";
    public static final String CC_POLICY_PRIVATE = "private";
    public static final String CC_POLICY_CUSTOM = "custom";
    public static final String CC_POLICY_IMMUTABLE = "immutable";

    private List<ClientCacheFilterRule> policies = new LinkedList<>();
    private Map<String, String> cacheControlValues = new HashMap<>();

    @Reference
    private ConfigurationAdmin configAdmin;

    @Activate
    @Modified
    public void activate(Map<String, String> properties, BundleContext context) {
        LOGGER.info("Activating Client Cache Service...");
        if (properties != null) {
            if (!properties.containsKey("config.description")) {
                properties = loadDefaultConfig(context);
            }
            this.policies = this.parseRules(properties);
            this.policies.sort(ClientCacheFilterRule::compareTo);
            this.cacheControlValues = this.computeCacheControlValues(properties);
        }
        policies.forEach(policy -> LOGGER.info("Enabled Policy: {}", policy));
        cacheControlValues.forEach((cck, ccv) -> LOGGER.info("Cache Control Value: [{}] {}", cck, ccv));
    }

    @Deactivate
    public void deactivate() {
        LOGGER.debug("Deactivating Client Cache Service...");
        this.policies = new LinkedList<>();
        this.cacheControlValues = new HashMap<>();
    }

    private Map<String, String> loadDefaultConfig(BundleContext context) {
        Properties defaultProps = new Properties();
        try {
            Bundle bundle = context.getBundle();
            URL configUrl = bundle.getResource("META-INF/configurations/org.jahia.bundles.cache.client.cfg");
            if (configUrl != null) {
                try (var input = configUrl.openStream()) {
                    defaultProps.load(input);
                }
                Configuration config = configAdmin.getConfiguration("org.jahia.bundles.cache.client", null);
                Dictionary<String, Object> properties = new Hashtable<>();
                defaultProps.forEach((key, value) -> properties.put((String) key, value));
                config.updateIfDifferent(properties);
                LOGGER.info("Loaded default config from META-INF/configurations/");
            } else {
                LOGGER.warn("No default config found in META-INF/configurations/");
            }
        } catch (IOException e) {
            LOGGER.error("Error loading default config", e);
        }
        return defaultProps.entrySet().stream().collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
    }

    public List<ClientCacheFilterRule> getPolicies() {
        return policies;
    }

    public Map<String, String> getCacheControlValues() {
        return cacheControlValues;
    }

    private List<ClientCacheFilterRule> parseRules(Map<String, ?> properties) {
        Map<String, ClientCacheFilterRule> rules = new HashMap<>();
        properties.keySet().stream().map(o -> o.split("\\.")).filter(e -> e.length == 3 && e[0].equals("policies"))
                .forEach(e -> {
                    String key = e[1];
                    if (!rules.containsKey(key)) {
                        rules.put(key, new ClientCacheFilterRule(key));
                    }
                    switch (e[2]) {
                        case "name":
                            rules.get(key).setName((String) properties.get("policies." + key + ".name"));
                            break;
                        case "index":
                            rules.get(key).setIndex(Integer.parseInt((String) properties.get("policies." + key + ".index")));
                            break;
                        case "description":
                            rules.get(key).setDescription((String) properties.get("policies." + key + ".description"));
                            break;
                        case "methodsPattern":
                            rules.get(key).setMethodsPattern(Pattern.compile((String) properties.get("policies." + key + ".methodsPattern")));
                            break;
                        case "urlPattern":
                            rules.get(key).setUrlPattern(Pattern.compile((String) properties.get("policies." + key + ".urlPattern")));
                            break;
                        case "clientCachePolicy":
                            rules.get(key).setClientCachePolicy((String) properties.get("policies." + key + ".clientCachePolicy"));
                            break;
                    }
                });
        if (rules.values().stream().anyMatch(rule -> !rule.isValid())) {
            LOGGER.warn("Some rules are invalid, they will be ignored.");
        }
        return rules.values().stream().filter(ClientCacheFilterRule::isValid).collect(Collectors.toList());
    }

    private Map<String, String> computeCacheControlValues(Map<String, ?> properties) {
        Map<String, String> values = new HashMap<>();
        values.put(CC_POLICY_PRIVATE, CC_POLICY_PRIVATE_VALUE);
        values.put(CC_POLICY_PUBLIC, configureCacheControlValue(CC_POLICY_PUBLIC_VALUE, properties));
        values.put(CC_POLICY_CUSTOM, configureCacheControlValue(CC_POLICY_CUSTOM_VALUE, properties));
        values.put(CC_POLICY_IMMUTABLE, configureCacheControlValue(CC_POLICY_IMMUTABLE_VALUE, properties));
        return values;
    }

    private String configureCacheControlValue(String value, Map<String, ?> properties) {
        String configuredValue = value;
        for (String key:  properties.keySet()) {
            configuredValue = configuredValue.replace("##" + key + "##", properties.get(key).toString());
        }
        return configuredValue;
    }

}
