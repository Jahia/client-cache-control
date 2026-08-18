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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

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
        Optional<String> header = resolveHeader(method, uri, params);
        String canonical = canonicalize(uri);
        if (canonical.equals(uri)) {
            return header;
        }
        // One resource can be requested under several spellings of the same path: a character written
        // percent-encoded, a duplicated separator, a dot segment. They all reach the same servlet, so they
        // all resolve to the same policy, and a rule needs to name the path only once to cover them.
        //
        // Keeping the stricter of the two answers is what makes that safe. Canonicalizing widens every
        // rule, permissive ones included, so the comparison below is what guarantees that resolving the
        // canonical form can only ever remove shared caching, never grant it.
        Optional<String> canonicalHeader = resolveHeader(method, canonical, params);
        if (canonicalHeader.isPresent() && !allowsSharedCaching(canonicalHeader.get())
                && (!header.isPresent() || allowsSharedCaching(header.get()))) {
            LOGGER.debug("[{} - {}] canonical form [{}] resolves to a stricter policy, applying it: [{}]", method, uri,
                    canonical, canonicalHeader.get());
            return canonicalHeader;
        }
        return header;
    }

    private Optional<String> resolveHeader(String method, String uri, Map<String, String> params) {
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

    private static final Pattern DUPLICATE_SEPARATOR = Pattern.compile("/{2,}");
    private static final Pattern NON_ZERO_S_MAXAGE = Pattern.compile("s-maxage\\s*=\\s*0*[1-9]");

    /**
     * Rewrites a request URI to the single form that stands for every spelling of the same path:
     * percent-encoding decoded once, duplicate separators collapsed, dot segments removed.
     *
     * <p>Returns the argument unchanged when it is already canonical, which is the common case.</p>
     */
    static String canonicalize(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        String canonical = removeDotSegments(DUPLICATE_SEPARATOR.matcher(decodeOnce(uri)).replaceAll("/"));
        return canonical.equals(uri) ? uri : canonical;
    }

    /**
     * Percent-decodes once. A {@code %} that is not followed by two hexadecimal digits is left as it
     * stands, so a path that carries a literal percent sign is not corrupted. Decoding runs over the
     * collected bytes rather than character by character, so a multi-byte UTF-8 sequence written as
     * several percent groups decodes to the character it stands for.
     */
    private static String decodeOnce(String uri) {
        if (uri.indexOf('%') < 0) {
            return uri;
        }
        StringBuilder decoded = new StringBuilder(uri.length());
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        for (int i = 0; i < uri.length(); ) {
            if (uri.charAt(i) == '%' && i + 2 < uri.length() && isHex(uri.charAt(i + 1)) && isHex(uri.charAt(i + 2))) {
                pending.write(Integer.parseInt(uri.substring(i + 1, i + 3), 16));
                i += 3;
                continue;
            }
            flush(pending, decoded);
            decoded.append(uri.charAt(i));
            i++;
        }
        flush(pending, decoded);
        return decoded.toString();
    }

    private static void flush(ByteArrayOutputStream pending, StringBuilder out) {
        if (pending.size() > 0) {
            out.append(new String(pending.toByteArray(), StandardCharsets.UTF_8));
            pending.reset();
        }
    }

    private static boolean isHex(char c) {
        return Character.digit(c, 16) >= 0;
    }

    /**
     * Removes {@code .} and {@code ..} segments, following RFC 3986 section 5.2.4. A {@code ..} that
     * would climb above the root is dropped rather than applied.
     */
    private static String removeDotSegments(String path) {
        if (path.indexOf('.') < 0) {
            return path;
        }
        boolean rooted = path.startsWith("/");
        Deque<String> kept = new ArrayDeque<>();
        for (String segment : (rooted ? path.substring(1) : path).split("/", -1)) {
            if (".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                // A rooted path has no parent above its root, so a climb that would leave it is dropped
                // rather than applied. Leaving it in would make the canonical form name a path the
                // request could not reach.
                kept.pollLast();
                continue;
            }
            kept.addLast(segment);
        }
        String rebuilt = (rooted ? "/" : "") + String.join("/", kept);
        // A path whose last segment was a dot segment loses its trailing separator when it is rebuilt.
        if (path.endsWith("/") && !rebuilt.endsWith("/")) {
            rebuilt = rebuilt + "/";
        }
        return rebuilt.isEmpty() ? "/" : rebuilt;
    }

    /**
     * Whether a Cache-Control value lets a cache shared between users store the response. This is the
     * property the two candidate policies are compared on, and it reads the header itself rather than
     * the name of a template, so a rule that carries a literal header value is ranked too.
     */
    static boolean allowsSharedCaching(String header) {
        if (header == null) {
            return false;
        }
        String value = header.toLowerCase(Locale.ROOT);
        if (value.contains("no-store") || value.contains("private")) {
            return false;
        }
        return value.contains("public") || NON_ZERO_S_MAXAGE.matcher(value).find();
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
