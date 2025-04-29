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
package org.jahia.bundles.cache.client.impl;

import org.jahia.bundles.cache.client.api.ClientCacheMode;
import org.jahia.bundles.cache.client.api.ClientCacheRule;
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

    @ObjectClassDefinition( name = "org.jahia.bundles.cache.client", description = "%config.description", localization = "OSGI-INF/l10n/clientCache")
    public @interface Config {

        @AttributeDefinition(name = "%mode.name", description = "%mode.description",
                options = {
                        @Option(label = "%mode.strict.label", value = "strict"),
                        @Option(label = "%mode.overrides.label", value = "overrides")
                })
        String mode() default "overrides";

        @AttributeDefinition(name = "%ttl.short.name", description = "%ttl.short.description")
        String short_ttl() default "60";

        @AttributeDefinition(name = "%ttl.medium.name", description = "%ttl.medium.description")
        String medium_ttl() default "600";

        @AttributeDefinition(name = "%ttl.immutable.name", description = "%ttl.immutable.description")
        String immutable_ttl() default "2678400";

        @AttributeDefinition(name = "%cacheHeaderTemplate.private.name", description = "%cacheHeaderTemplate.private.description")
        String cache_header_template_private() default "private, no-cache, no-store, must-revalidate, proxy-revalidate, max-age=0";

        @AttributeDefinition(name = "%cacheHeaderTemplate.custom.name", description = "%cacheHeaderTemplate.custom.description")
        String cache_header_template_custom() default "public, must-revalidate, max-age=1, s-maxage=%%jahiaClientCacheCustomTTL%%, stale-while-revalidate=15";

        @AttributeDefinition(name = "%cacheHeaderTemplate.public.name", description = "%cacheHeaderTemplate.public.description")
        String cache_header_template_public() default "public, must-revalidate, max-age=1, s-maxage=##short.ttl##, stale-while-revalidate=15";

        @AttributeDefinition(name = "%cacheHeaderTemplate.public.medium.name", description = "%cacheHeaderTemplate.public.medium.description")
        String cache_header_template_public_medium() default "public, must-revalidate, max-age=1, s-maxage=##medium.ttl##, stale-while-revalidate=15";

        @AttributeDefinition(name = "%cacheHeaderTemplate.immutable.name", description = "%cacheHeaderTemplate.immutable.description")
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
        LOGGER.info("Clearing RuleSet factory {}", factory.getName());
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
    public List<ClientCacheRule> listRules() {
        return new ArrayList<>(listFilterRules());
    }

    private List<ClientCacheFilterRule> listFilterRules() {
        if (factory == null) {
            return Collections.emptyList();
        }
        return this.factory.getRules();
    }

    @Override
    public Collection<ClientCacheTemplate> listHeaderTemplates() {
        return new ArrayList<>(cacheControlHeaderTemplates.values());
    }

    @Override public Optional<String> getCacheControlHeader(String method, String uri, Map<String, String> params) {
        Optional<ClientCacheFilterRule> mRule = listFilterRules().stream()
                .filter(rule -> rule.getMethods().contains(method) && rule.getUrlPattern().matcher(uri).matches()).findFirst();
        if (mRule.isPresent()) {
            if (mRule.get().getHeaderValue() != null) {
                LOGGER.debug("[{} - {}] matched with rule {}, returning header: {}", method, uri, mRule.get(), mRule.get().getHeaderValue());
                return Optional.of(mRule.get().getHeaderValue());
            }
            if (mRule.get().getHeaderTemplate() != null) {
                String headerValue = cacheControlHeaderTemplates.getOrDefault(mRule.get().getHeaderTemplate(), ClientCacheFilterTemplate.EMPTY).getFilteredTemplate(params);
                LOGGER.debug("[{} - {}] matched with rule {}, returning header: {}", uri, method, mRule.get(), headerValue);
                return Optional.of(headerValue);
            }
        }
        return Optional.empty();
    }

    @Override public Optional<String> getCacheControlHeader(String templateName, Map<String, String> params) {
        if (cacheControlHeaderTemplates.containsKey(templateName)) {
            String headerValue = cacheControlHeaderTemplates.get(templateName).getFilteredTemplate(params);
            LOGGER.debug("TemplateName {} returned header value: {}", templateName, headerValue);
            return Optional.of(headerValue);
        }
        LOGGER.warn("TemplateName {} not found", templateName);
        return Optional.empty();
    }

    @Override public String getDefaultCacheControlHeader() {
        return cacheControlHeaderTemplates.get(ClientCacheFilterTemplate.DEFAULT).getTemplate();
    }

    private Map<String, ClientCacheFilterTemplate> computeCacheControlHeaderTemplates(Config config) {
        Map<String, ClientCacheFilterTemplate> values = new HashMap<>();
        values.put(ClientCacheFilterTemplate.PRIVATE,
                new ClientCacheFilterTemplate(ClientCacheFilterTemplate.PRIVATE, configureCacheControlHeaderTemplate(config.cache_header_template_private(), config)));
        values.put(ClientCacheFilterTemplate.PUBLIC_MEDIUM,
                new ClientCacheFilterTemplate(ClientCacheFilterTemplate.PUBLIC_MEDIUM, configureCacheControlHeaderTemplate(config.cache_header_template_public_medium(), config)));
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
        configuredValue = configuredValue.replace("##short.ttl##", config.short_ttl());
        configuredValue = configuredValue.replace("##medium.ttl##", config.medium_ttl());
        configuredValue = configuredValue.replace("##immutable.ttl##", config.immutable_ttl());
        return configuredValue;
    }

}
