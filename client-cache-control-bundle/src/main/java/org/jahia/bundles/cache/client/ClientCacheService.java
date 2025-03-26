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
@Component(service = {ClientCacheService.class}, configurationPid = "org.jahia.bundles.cache.client", immediate = true)
@Designate(ocd = ClientCacheService.Config.class)
public class ClientCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheService.class);

    @ObjectClassDefinition(
            name = "org.jahia.bundles.cache.client",
            description = "Configuration of the client cache control")
    public @interface Config {

        @AttributeDefinition(name = "Intermediates Cache Duration",
                description = "Duration while an intermediate can keep content in cache without revalidation (in seconds)")
        String ttl_intermediates() default "300";

        @AttributeDefinition(name = "Immutable Cache Duration",
                description = "Duration while content is considered immutable in all caches (in seconds)")
        String ttl_immutable() default "2678400";

        @AttributeDefinition(name = "Log Cache Control Header when overridden",
                description = "Log the Cache-Control header value when overrides by other component in the request/response processing chain is detected")
        String logOverrides() default "true";

        @AttributeDefinition(name = "Private Cache Header Template",
                description = "(DO NOT EDIT WITHOUT KNOWING THE IMPLICATIONS) Cache header template for private resource (client cache with revalidation and no intermediates cache)")
        String cache_header_template_private() default "private, no-cache, no-store, must-revalidate, proxy-revalidate, max-age=0";

        @AttributeDefinition(name = "Customized Cache Header Template",
                description = "(DO NOT EDIT WITHOUT KNOWING THE IMPLICATIONS) Cache header template for rendered resource with specific content cache properties (client cache with revalidation and intermediates cache duration reflecting internal jahia cache expiration)")
        String cache_header_template_custom() default "public, must-revalidate, max-age=1, s-maxage=%%jahiaClientCacheCustomTTL%%, stale-while-revalidate=15";

        @AttributeDefinition(name = "Public Cache Header Template",
                description = "(DO NOT EDIT WITHOUT KNOWING THE IMPLICATIONS) Cache header template for public dynamic resource like pages or files (client cache with systematic revalidation and intermediates cache with intermediates ttl value)")
        String cache_header_template_public() default "public, must-revalidate, max-age=1, s-maxage=##intermediates.ttl##, stale-while-revalidate=15";

        @AttributeDefinition(name = "Immutable Cache Header Template",
                description = "(DO NOT EDIT WITHOUT KNOWING THE IMPLICATIONS)  Cache header template for immutable resources that never changes (client and intermediates caching with immutable ttl value, no revalidation needed)")
        String cache_header_template_immutable() default "public, max-age=##immutable.ttl##, s-maxage=##immutable.ttl##, stale-while-revalidate=15, immutable";

    }

    public static final String CC_POLICY_ATTR = "jahiaClientCachePolicy";
    public static final String CC_ORIGINAL_REQUEST_URI_ATTR = "jahiaOriginalRequestURI";
    public static final String CC_CUSTOM_TTL_ATTR = "jahiaClientCacheCustomTTL";

    private ClientCacheFilterRuleFactory factory;
    private Map<String, String> cacheControlHeaderTemplates = new HashMap<>();
    private boolean logOverrides = true;

    @Activate
    @Modified
    public void activate(Config config) {
        LOGGER.info("Activating Client Cache Service...");
        this.cacheControlHeaderTemplates = this.computeCacheControlHeaderTemplates(config);
        this.logOverrides = Boolean.parseBoolean(config.logOverrides());
        cacheControlHeaderTemplates.forEach((cck, ccv) -> LOGGER.info("Cache Control Value: [{}] {}", cck, ccv));
    }

    @Deactivate
    public void deactivate() {
        LOGGER.debug("Deactivating Client Cache Service...");
        this.cacheControlHeaderTemplates = new HashMap<>();
    }

    @Reference(service = ClientCacheFilterRuleFactory.class, policy = ReferencePolicy.DYNAMIC, bind = "setRules", unbind = "clearRules")
    public void setRules(ClientCacheFilterRuleFactory factory) {
        LOGGER.info("Setting rule's factory");
        this.factory = factory;
    }

    public void clearRules(ClientCacheFilterRuleFactory factory) {
        LOGGER.info("Clearing rule's factory");
        this.factory = null;
    }

    public List<ClientCacheFilterRule> getRules() {
        if (factory == null) {
            return Collections.emptyList();
        }
        return this.factory.getRules();
    }

    public boolean logOverridesCacheControlHeader() {
        return logOverrides;
    }

    public Map<String, String> getCacheControlHeaderTemplates() {
        return cacheControlHeaderTemplates;
    }

    private Map<String, String> computeCacheControlHeaderTemplates(Config config) {
        Map<String, String> values = new HashMap<>();
        values.put(ClientCacheHeaderTemplate.PRIVATE.getName(), config.cache_header_template_private());
        values.put(ClientCacheHeaderTemplate.PUBLIC.getName(), configureCacheControlHeaderTemplate(config.cache_header_template_public(), config));
        values.put(ClientCacheHeaderTemplate.CUSTOM.getName(), configureCacheControlHeaderTemplate(config.cache_header_template_custom(), config));
        values.put(ClientCacheHeaderTemplate.IMMUTABLE.getName(), configureCacheControlHeaderTemplate(config.cache_header_template_immutable(), config));
        return values;
    }

    private String configureCacheControlHeaderTemplate(String value, Config config) {
        String configuredValue = value;
        configuredValue = configuredValue.replace("##intermediates.ttl##", config.ttl_intermediates());
        configuredValue = configuredValue.replace("##immutable.ttl##", config.ttl_immutable());
        return configuredValue;
    }

}
