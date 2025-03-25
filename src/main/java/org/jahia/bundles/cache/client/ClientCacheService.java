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

import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Jerome Blanchard
 */
@Component(service = {ClientCacheService.class}, immediate = true)
@Designate(ocd = ClientCacheService.Config.class)
public class ClientCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheService.class);

    @ObjectClassDefinition(name = "%config.name", description = "%config.description", localization = "OSGI-INF/l10n/clientCache")
    public @interface Config {

        @AttributeDefinition(name = "%ttl.intermediates.name", defaultValue = "300", description = "%ttl.intermediates.description")
        String intermediatesTtl() default "300";

        @AttributeDefinition(name = "%ttl.immutable.name", defaultValue = "2678400", description = "%ttl.immutable.description")
        String immutableTtl() default "2678400";

        @AttributeDefinition(name = "%cacheHeaderTemplate.private.name", description = "%cacheHeaderTemplate.private.description")
        String privateCacheHeaderTemplate() default "private, no-cache, no-store, must-revalidate, proxy-revalidate, max-age=0";

        @AttributeDefinition(name = "%cacheHeaderTemplate.custom.name", description = "%cacheHeaderTemplate.custom.description")
        String customCacheHeaderTemplate() default "public, must-revalidate, max-age=1, s-maxage=%%jahiaClientCacheCustomTTL%%, stale-while-revalidate=15";

        @AttributeDefinition(name = "%cacheHeaderTemplate.public.name", description = "%cacheHeaderTemplate.public.description")
        String publicCacheHeaderTemplate() default "public, must-revalidate, max-age=1, s-maxage=##intermediates.ttl##, stale-while-revalidate=15";

        @AttributeDefinition(name = "%cacheHeaderTemplate.immutable.name", description = "%cacheHeaderTemplate.immutable.description")
        String immutableCacheHeaderTemplate() default "public, max-age=##immutable.ttl##, s-maxage=##immutable.ttl##, stale-while-revalidate=15, immutable";

    }

    public static final String CC_POLICY_ATTR = "jahiaClientCachePolicy";
    public static final String CC_ORIGINAL_REQUEST_URI_ATTR = "jahiaOriginalRequestURI";
    public static final String CC_CUSTOM_TTL_ATTR = "jahiaClientCacheCustomTTL";

    private List<ClientCacheFilterRule> rules = new LinkedList<>();
    private Map<String, String> cacheControlValues = new HashMap<>();

    @Activate
    @Modified
    public void activate(Config config) {
        LOGGER.info("Activating Client Cache Service...");
        this.cacheControlValues = this.computeCacheControlValues(config);
        cacheControlValues.forEach((cck, ccv) -> LOGGER.info("Cache Control Value: [{}] {}", cck, ccv));
    }

    @Deactivate
    public void deactivate() {
        LOGGER.debug("Deactivating Client Cache Service...");
        this.rules = new LinkedList<>();
        this.cacheControlValues = new HashMap<>();
    }

    @Reference(service = ClientCacheFilterRuleFactory.class, policy = ReferencePolicy.DYNAMIC, bind = "setRules", unbind = "clearRules")
    public void setRules(ClientCacheFilterRuleFactory factory) {
        LOGGER.debug("Setting rules from factory");
        this.rules = new LinkedList<>();
        this.rules.addAll(factory.getRules());
        this.rules.sort(ClientCacheFilterRule::compareTo);
        rules.forEach(policy -> LOGGER.info("Enabled Rules: {}", policy));
    }

    public void clearRules(ClientCacheFilterRuleFactory factory) {
        LOGGER.debug("Clearing rules");
        this.rules = new ArrayList<>();
    }

    public List<ClientCacheFilterRule> getRules() {
        return rules;
    }

    public Map<String, String> getCacheControlValues() {
        return cacheControlValues;
    }

    private Map<String, String> computeCacheControlValues(Config config) {
        Map<String, String> values = new HashMap<>();
        values.put(ClientCacheHeaderTemplate.PRIVATE.getName(), config.privateCacheHeaderTemplate());
        values.put(ClientCacheHeaderTemplate.PUBLIC.getName(), configureCacheControlValue(config.publicCacheHeaderTemplate(), config));
        values.put(ClientCacheHeaderTemplate.CUSTOM.getName(), configureCacheControlValue(config.customCacheHeaderTemplate(), config));
        values.put(ClientCacheHeaderTemplate.IMMUTABLE.getName(), configureCacheControlValue(config.immutableCacheHeaderTemplate(), config));
        return values;
    }

    private String configureCacheControlValue(String value, Config config) {
        String configuredValue = value;
        configuredValue = configuredValue.replace("##intermediates.ttl##", config.intermediatesTtl());
        configuredValue = configuredValue.replace("##immutable.ttl##", config.immutableTtl());
        return configuredValue;
    }

}
