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
package org.jahia.bundles.cache.client;

import org.jahia.bundles.cache.client.impl.ClientCacheFilterRule;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheFilterRuleSetTest {

    private static final Logger LOGGER = Logger.getLogger(ClientCacheFilterRuleSetTest.class.getName());

    public static final List<ClientCacheFilterRule> rules = new LinkedList<>();
    static {
        rules.add(ClientCacheFilterRule.deserialize("3;GET|HEAD;(?:/[^/]+)?/files/.*;template:public"));
        rules.add(ClientCacheFilterRule.deserialize("4;GET|HEAD;(?:/[^/]+)?/repository/.*;template:public"));
        rules.add(ClientCacheFilterRule.deserialize("1;GET|HEAD;(?:/[^/]+)?/cms/render/live/.*;template:public"));
        rules.add(ClientCacheFilterRule.deserialize("2;GET|HEAD;(?:/[^/]+)?/cms/.*;template:private"));
        rules.add(ClientCacheFilterRule.deserialize("5;GET|HEAD;(?:/[^/]+)?/generated-resources;template:private"));
        rules.add(ClientCacheFilterRule.deserialize("6;GET|HEAD;/quiche;public, max-age=31536000, no-transform"));
        rules.add(ClientCacheFilterRule.deserialize("7;GET|HEAD;/.*;template:public"));
        rules.sort(ClientCacheFilterRule::compareTo);
    }

    @Test
    public void testSortRules() {
        List<ClientCacheFilterRule> orderedRules = List.of(
                ClientCacheFilterRule.deserialize("1;GET|HEAD;(?:/[^/]+)?/cms/render/live/.*;template:public"),
                ClientCacheFilterRule.deserialize("2;GET|HEAD;(?:/[^/]+)?/cms/.*;template:private"),
                ClientCacheFilterRule.deserialize("3;GET|HEAD;(?:/[^/]+)?/files/.*;template:public"),
                ClientCacheFilterRule.deserialize("4;GET|HEAD;(?:/[^/]+)?/repository/.*;template:public"),
                ClientCacheFilterRule.deserialize("5;GET|HEAD;(?:/[^/]+)?/generated-resources;template:private"),
                ClientCacheFilterRule.deserialize("6;GET|HEAD;/quiche;public, max-age=31536000, no-transform"),
                ClientCacheFilterRule.deserialize("7;GET|HEAD;/.*;template:public")
                );
        rules.forEach(ruleEntry -> LOGGER.info(ruleEntry.toString()));
        Assert.assertEquals("(?:/[^/]+)?/cms/render/live/.*", rules.get(0).getUrlRegexp());
        Assert.assertEquals("/.*", rules.get(6).getUrlRegexp());
        assertArrayEquals(orderedRules.toArray(), rules.toArray());

        rules.add(ClientCacheFilterRule.deserialize("3.99;GET|HEAD;/tagada;template:plop"));
        Assert.assertNotEquals("/tagada", rules.get(3).getUrlRegexp());
        rules.sort(ClientCacheFilterRule::compareTo);
        Assert.assertEquals("/tagada", rules.get(3).getUrlRegexp());
        List<ClientCacheFilterRule> orderedRules2 = List.of(
                ClientCacheFilterRule.deserialize("1;GET|HEAD;(?:/[^/]+)?/cms/render/live/.*;template:public"),
                ClientCacheFilterRule.deserialize("2;GET|HEAD;(?:/[^/]+)?/cms/.*;template:private"),
                ClientCacheFilterRule.deserialize("3;GET|HEAD;(?:/[^/]+)?/files/.*;template:public"),
                ClientCacheFilterRule.deserialize("3.99;GET|HEAD;/tagada;template:plop"),
                ClientCacheFilterRule.deserialize("4;GET|HEAD;(?:/[^/]+)?/repository/.*;template:public"),
                ClientCacheFilterRule.deserialize("5;GET|HEAD;(?:/[^/]+)?/generated-resources;template:private"),
                ClientCacheFilterRule.deserialize("6;GET|HEAD;/quiche;public, max-age=31536000, no-transform"),
                ClientCacheFilterRule.deserialize("7;GET|HEAD;/.*;template:public")
        );
        assertArrayEquals(orderedRules2.toArray(), rules.toArray());

    }

    @Test
    public void testMatchRule1() {
        Optional<ClientCacheFilterRule> firstMatchingRule = getFirstMatchingRule("GET", "/context/cms/logout");
        assertTrue(firstMatchingRule.isPresent());
        assertTrue(firstMatchingRule.get().getUrlRegexp().contains("cms"));
        Assert.assertEquals("template:private", firstMatchingRule.get().getHeader());
    }

    private Optional<ClientCacheFilterRule> getFirstMatchingRule(String method, String url) {
        return rules.stream()
                .filter(rule -> rule.getMethods().contains(method)
                        && rule.getUrlPattern().matcher(url).matches()).findFirst();
    }
}
