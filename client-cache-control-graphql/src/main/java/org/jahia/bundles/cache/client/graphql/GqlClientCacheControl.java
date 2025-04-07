/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
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
import org.jahia.bundles.cache.client.api.ClientCacheService;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jerome Blanchard
 */
@GraphQLName("GqlClientCacheControl")
@GraphQLDescription("Client Cache Control GraphQL")
public class GqlClientCacheControl {

    @Inject
    @GraphQLOsgiService
    private ClientCacheService service;

    @GraphQLField()
    @GraphQLName("mode")
    @GraphQLDescription("Client Cache-Control configured mode")
    public String mode() {
        return service.getMode().name();
    }

    @GraphQLField
    @GraphQLName("templates")
    @GraphQLDescription("list of header templates, or empty list if no templates exist")
    public List<GqlClientCacheTemplate> listTemplates() {
        return service.listHeaderTemplates().stream()
                .map(GqlClientCacheTemplate::new)
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLName("rules")
    @GraphQLDescription("Get list of rules, or empty list if no rules exist")
    public List<GqlClientCacheRule> listRules() {
        return service.listRules().stream()
                .map(GqlClientCacheRule::new)
                .collect(Collectors.toList());
    }

}
