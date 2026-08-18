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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Resolution of the Cache-Control policy for a request URI.
 *
 * <p>The rules loaded here are the shipped default ruleset plus the two rules modules contribute for
 * their own administration paths, because what this exercises is how a rule that names one path
 * behaves when the same path is requested under another spelling.</p>
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
    public void canonicalSpellingResolvesToTheRuleThatNamesIt() {
        assertEquals(PRIVATE, policyFor("/modules/healthcheck"));
        assertEquals(PRIVATE, policyFor("/modules/tools/index.jsp"));
    }

    @Test
    public void aRuleAlsoCoversTheOtherSpellingsOfThePathItNames() {
        // Each of these reaches the same servlet as the canonical URI above, so each is answered with
        // the same policy.
        assertEquals(PRIVATE, policyFor("/modules/healthchec%6b"));
        assertEquals(PRIVATE, policyFor("/modules/%68ealthcheck"));
        assertEquals(PRIVATE, policyFor("/modules//healthcheck"));
        assertEquals(PRIVATE, policyFor("/modules//tools/index.jsp"));
        assertEquals(PRIVATE, policyFor("/modules/tool%73/index.jsp"));
        assertEquals(PRIVATE, policyFor("/modules/./healthcheck"));
        assertEquals(PRIVATE, policyFor("/modules/x/../healthcheck"));
    }

    @Test
    public void theContextPathIsStillToleratedOnEverySpelling() {
        assertEquals(PRIVATE, policyFor("/jahia/modules/healthcheck"));
        assertEquals(PRIVATE, policyFor("/jahia/modules/healthchec%6b"));
    }

    @Test
    public void aRuleAppliesToEverySpellingWhicheverPolicyItCarries() {
        // The rule that names the live rendering path is a permissive one, and it covers the spellings of
        // that path exactly as a restrictive rule does. The policy follows the resource that answers, so it
        // is the same for both of these.
        assertEquals(PUBLIC, policyFor("/cms/render/live/site/home.html"));
        assertEquals(PUBLIC, policyFor("/cms/render/liv%65/site/home.html"));
    }

    @Test
    public void policiesForOrdinaryPathsAreUnchanged() {
        assertEquals(PUBLIC_MEDIUM, policyFor("/modules/ckeditor/javascript/ckeditor.js"));
        assertEquals(PUBLIC_MEDIUM, policyFor("/files/default/site/image.png"));
        assertEquals(PUBLIC, policyFor("/sites/mysite/home.html"));
        assertEquals(PRIVATE, policyFor("/cms/edit/default/en/sites/mysite.html"));
        assertEquals(PRIVATE,
                service.getCacheControlHeader("POST", "/modules/graphql", Collections.emptyMap()).orElse(null));
    }

    @Test
    public void canonicalizeRewritesEverySpellingToOneForm() {
        assertEquals("/modules/healthcheck", ClientCacheServiceImpl.canonicalize("/modules/healthchec%6b"));
        assertEquals("/modules/healthcheck", ClientCacheServiceImpl.canonicalize("/modules//healthcheck"));
        assertEquals("/modules/healthcheck", ClientCacheServiceImpl.canonicalize("/modules/./healthcheck"));
        assertEquals("/modules/healthcheck", ClientCacheServiceImpl.canonicalize("/modules/x/../healthcheck"));
        assertEquals("/modules/tools/index.jsp", ClientCacheServiceImpl.canonicalize("/modules/tool%73//index.jsp"));
        // A multi-byte character written as several percent groups decodes to the character it stands for.
        assertEquals("/files/été.pdf", ClientCacheServiceImpl.canonicalize("/files/%C3%A9t%C3%A9.pdf"));
    }

    @Test
    public void canonicalizeLeavesAPathThatIsAlreadyCanonicalUntouched() {
        String uri = "/modules/ckeditor/javascript/ckeditor.js";
        assertSame(uri, ClientCacheServiceImpl.canonicalize(uri));
        // A percent sign that is not followed by two hexadecimal digits belongs to the name, and is kept
        // as it stands. An encoded one is decoded, like any other encoded byte.
        assertEquals("/files/100% done.pdf", ClientCacheServiceImpl.canonicalize("/files/100% done.pdf"));
        assertEquals("/files/50% off.pdf", ClientCacheServiceImpl.canonicalize("/files/50%25 off.pdf"));
    }

    @Test
    public void canonicalizeDoesNotClimbAboveTheRoot() {
        assertEquals("/etc/passwd", ClientCacheServiceImpl.canonicalize("/../../etc/passwd"));
        assertEquals("/", ClientCacheServiceImpl.canonicalize("/"));
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
