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
package org.jahia.bundles.cache.client.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.bundles.cache.client.api.ClientCacheRule;

import java.util.Set;

/**
 * @author Jerome Blanchard
 */
@GraphQLName("GqlClientCacheRule")
@GraphQLDescription("Client Cache-Control Filtering Rule")
public class GqlClientCacheRule {

    private final ClientCacheRule rule;

    public GqlClientCacheRule(ClientCacheRule rule) {
        this.rule = rule;
    }

    @GraphQLField
    @GraphQLDescription("Rule priority (lower number = higher priority)")
    public String getPriority() {
        return Float.toString(rule.getPriority());
    }

    @GraphQLField
    @GraphQLDescription("Concerned method's list")
    public Set<String> getMethods() {
        return rule.getMethods();
    }

    @GraphQLField
    @GraphQLDescription("URL regular expression")
    public String getUrlRegexp() {
        return rule.getUrlRegexp();
    }

    @GraphQLField
    @GraphQLDescription("Header")
    public String getHeader() {
        return rule.getHeader();
    }

}
