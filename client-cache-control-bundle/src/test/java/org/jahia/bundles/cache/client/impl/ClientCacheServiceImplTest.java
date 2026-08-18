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

import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Resolution of the Cache-Control policy for a request URI.
 *
 * <p>The rules loaded here are the shipped default ruleset plus the two rules modules contribute for
 * their own administration paths. The paths passed in are resolved paths, which is what the servlet
 * filter hands over; how a request URI becomes one is {@code ClientCacheFilterTest}.</p>
 *
 * @author Jerome Blanchard
 */
public class ClientCacheServiceImplTest {

    private static final String PRIVATE = "private, no-cache, no-store, must-revalidate, proxy-revalidate, max-age=0";
    private static final String PUBLIC_MEDIUM = "public, must-revalidate, max-age=1, s-maxage=600, stale-while-revalidate=15";
    private static final String PUBLIC = "public, must-revalidate, max-age=1, s-maxage=60, stale-while-revalidate=15";

    private ClientCacheServiceImpl service;

    @Before
    public void setUp() throws Exception {
        ClientCacheFilterRuleSetFactory factory = new ClientCacheFilterRuleSetFactory();
        Hashtable<String, Object> ruleset = new Hashtable<>();
        ruleset.put("name", "Test ruleset");
        String[] rules = {
                "1;GET|HEAD;(?:/[^/]+)?/cms/render/live/.*;template:public",
                "2;GET|HEAD;(?:/[^/]+)?/cms/.*;template:private",
                "7;GET|HEAD;(?:/[^/]+)?/files/.*;template:public-medium",
                "8.1;GET|HEAD;(?:/[^/]+)?/modules/tools(/.*)?;template:private",
                "8.2;GET|HEAD;(?:/[^/]+)?/modules/healthcheck(/.*)?;template:private",
                "9;GET|HEAD;(?:/[^/]+)?/modules/.*;template:public-medium",
                "14;POST|DELETE|PATCH;.*;template:private",
                "15;GET|HEAD;.*;template:public",
        };
        for (int i = 0; i < rules.length; i++) {
            ruleset.put("rule." + i, rules[i]);
        }
        factory.updated("org.jahia.bundles.cache.client.ruleset-test", ruleset);

        service = new ClientCacheServiceImpl();
        service.setRuleSetFactory(factory);
        service.setup(defaultConfig());
    }

    private String policyFor(String uri) {
        Optional<String> header = service.getCacheControlHeader("GET", uri, Collections.emptyMap());
        assertTrue("no rule matched " + uri, header.isPresent());
        return header.get();
    }

    @Test
    public void aPathResolvesToTheRuleThatNamesIt() {
        assertEquals(PRIVATE, policyFor("/modules/healthcheck"));
        assertEquals(PRIVATE, policyFor("/modules/tools/index.jsp"));
        // The rules open with an optional segment group, so they match with a context path too.
        assertEquals(PRIVATE, policyFor("/jahia/modules/healthcheck"));
    }




    @Test
    public void policiesForOrdinaryPathsAreThoseTheRulesetStates() {
        assertEquals(PUBLIC, policyFor("/cms/render/live/site/home.html"));
        assertEquals(PUBLIC_MEDIUM, policyFor("/modules/ckeditor/javascript/ckeditor.js"));
        assertEquals(PUBLIC_MEDIUM, policyFor("/files/default/site/image.png"));
        assertEquals(PUBLIC, policyFor("/sites/mysite/home.html"));
        assertEquals(PRIVATE, policyFor("/cms/edit/default/en/sites/mysite.html"));
        assertEquals(PRIVATE,
                service.getCacheControlHeader("POST", "/modules/graphql", Collections.emptyMap()).orElse(null));
    }





    /** The service configuration with every value left at the default the component declares. */
    private static ClientCacheServiceImpl.Config defaultConfig() {
        return new ClientCacheServiceImpl.Config() {
            @Override public Class<? extends Annotation> annotationType() {
                return ClientCacheServiceImpl.Config.class;
            }
            @Override public String mode() { return "overrides"; }
            @Override public String short_ttl() { return "60"; }
            @Override public String medium_ttl() { return "600"; }
            @Override public String immutable_ttl() { return "2678400"; }
            @Override public String cache_header_template_private() { return PRIVATE; }
            @Override public String cache_header_template_custom() {
                return "public, must-revalidate, max-age=1, s-maxage=%%jahiaClientCacheCustomTTL%%, stale-while-revalidate=15";
            }
            @Override public String cache_header_template_public() {
                return "public, must-revalidate, max-age=1, s-maxage=##short.ttl##, stale-while-revalidate=15";
            }
            @Override public String cache_header_template_public_medium() {
                return "public, must-revalidate, max-age=1, s-maxage=##medium.ttl##, stale-while-revalidate=15";
            }
            @Override public String cache_header_template_immutable() {
                return "public, max-age=##immutable.ttl##, s-maxage=##immutable.ttl##, stale-while-revalidate=15, immutable";
            }
        };
    }
}
