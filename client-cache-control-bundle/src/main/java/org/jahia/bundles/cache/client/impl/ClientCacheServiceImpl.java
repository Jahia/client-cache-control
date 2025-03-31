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

import org.jahia.bundles.cache.client.api.ClientCacheMode;
import org.jahia.bundles.cache.client.api.ClientCacheService;
import org.jahia.bundles.cache.client.api.ClientCacheTemplate;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Jerome Blanchard
 */
@Component(service = { ClientCacheService.class}, configurationPid = "org.jahia.bundles.cache.client", immediate = true)
@Designate(ocd = ClientCacheServiceImpl.Config.class)
public class ClientCacheServiceImpl implements ClientCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheServiceImpl.class);

    @ObjectClassDefinition(
            name = "org.jahia.bundles.cache.client",
            description = "Configuration of the client cache control")
    public @interface Config {

        @AttributeDefinition(name = "Allow Cache Control Header overrides",
                description = "Allow the Cache-Control header value to be overridden by other component in the request/response processing chain",
                options = {
                        @Option(label = "Lock cache header", value = "strict"),
                        @Option(label = "Allow overrides", value = "overrides")
                })
        String mode() default "overrides";

        @AttributeDefinition(name = "Intermediates Cache Duration",
                description = "Duration while an intermediate can keep content in cache without revalidation (in seconds)")
        String intermediates_ttl() default "300";

        @AttributeDefinition(name = "Immutable Cache Duration",
                description = "Duration while content is considered immutable in all caches (in seconds)")
        String immutable_ttl() default "2678400";

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

    private ClientCacheFilterRuleSetFactory factory;
    private Map<String, ClientCacheFilterTemplate> cacheControlHeaderTemplates = new HashMap<>();
    private boolean allowOverrides = true;

    @Activate
    @Modified
    public void setup(Config config) {
        LOGGER.info("Activate/Update Client Cache Service...");
        this.cacheControlHeaderTemplates = this.computeCacheControlHeaderTemplates(config);
        this.allowOverrides = config.mode().equals("overrides");
        cacheControlHeaderTemplates.forEach((cck, ccv) -> LOGGER.info("Cache Control Header Templates: [{}] {}", cck, ccv));
    }

    @Deactivate
    public void teardown() {
        LOGGER.debug("Deactivate Client Cache Service...");
        this.cacheControlHeaderTemplates = new HashMap<>();
    }

    @Reference(service = ClientCacheFilterRuleSetFactory.class, policy = ReferencePolicy.DYNAMIC, bind = "setRuleSetFactory", unbind = "clearRuleSetFactory")
    public void setRuleSetFactory(ClientCacheFilterRuleSetFactory factory) {
        LOGGER.info("Setting RuleSet factory");
        this.factory = factory;
    }

    public void clearRuleSetFactory(ClientCacheFilterRuleSetFactory factory) {
        LOGGER.info("Clearing RuleSet factory");
        this.factory = null;
    }

    public boolean allowOverridesCacheControlHeader() {
        return allowOverrides;
    }

    @Override public ClientCacheMode getMode() {
        if (allowOverrides) {
            return ClientCacheMode.ALLOW_OVERRIDES;
        } else {
            return ClientCacheMode.STRICT;
        }
    }

    @Override
    public List<ClientCacheFilterRule> listRules() {
        if (factory == null) {
            return Collections.emptyList();
        }
        return this.factory.getRules();
    }

    @Override public Collection<? extends ClientCacheTemplate> listHeaderTemplates() {
        return cacheControlHeaderTemplates.values();
    }

    @Override public String getCacheControlHeader(String method, String uri, Map<String, String> params) {
        Optional<ClientCacheFilterRule> mRule = listRules().stream()
                .filter(rule -> rule.getMethods().contains(method) && rule.getUrlPattern().matcher(uri).matches()).findFirst();
        if (mRule.isPresent()) {
            if (mRule.get().getHeaderValue() != null) {
                LOGGER.debug("Rule {} matched for method: {} and uri: {}, returning header value: {}", mRule.get(), method, uri, mRule.get().getHeaderValue());
                return mRule.get().getHeaderValue();
            }
            if (mRule.get().getHeaderTemplate() != null) {
                String headerValue = cacheControlHeaderTemplates.getOrDefault(mRule.get().getHeaderTemplate(), ClientCacheFilterTemplate.EMPTY).getFilteredTemplate(params);
                LOGGER.debug("Rule {} matched for method: {} and uri: {}, returning header value: {}", mRule.get(), method, uri, headerValue);
                return headerValue;
            }
        }
        return "";
    }

    @Override public String getCacheControlHeader(String templateName, Map<String, String> params) {
        String headerValue = cacheControlHeaderTemplates.getOrDefault(templateName, ClientCacheFilterTemplate.EMPTY).getFilteredTemplate(params);
        LOGGER.debug("TemplateName {} returned header value: {}", templateName, headerValue);
        return headerValue;
    }

    private Map<String, ClientCacheFilterTemplate> computeCacheControlHeaderTemplates(Config config) {
        Map<String, ClientCacheFilterTemplate> values = new HashMap<>();
        values.put(ClientCacheFilterTemplate.PRIVATE,
                new ClientCacheFilterTemplate(ClientCacheFilterTemplate.PRIVATE, configureCacheControlHeaderTemplate(config.cache_header_template_private(), config)));
        values.put(ClientCacheFilterTemplate.PUBLIC,
                new ClientCacheFilterTemplate(ClientCacheFilterTemplate.PUBLIC, configureCacheControlHeaderTemplate(config.cache_header_template_public(), config)));
        values.put(ClientCacheFilterTemplate.CUSTOM,
                new ClientCacheFilterTemplate(ClientCacheFilterTemplate.CUSTOM, configureCacheControlHeaderTemplate(config.cache_header_template_custom(), config)));
        values.put(ClientCacheFilterTemplate.IMMUTABLE,
                new ClientCacheFilterTemplate(ClientCacheFilterTemplate.IMMUTABLE, configureCacheControlHeaderTemplate(config.cache_header_template_immutable(), config)));
        return values;
    }

    private String configureCacheControlHeaderTemplate(String value, Config config) {
        String configuredValue = value;
        configuredValue = configuredValue.replace("##intermediates.ttl##", config.intermediates_ttl());
        configuredValue = configuredValue.replace("##immutable.ttl##", config.immutable_ttl());
        return configuredValue;
    }

}
