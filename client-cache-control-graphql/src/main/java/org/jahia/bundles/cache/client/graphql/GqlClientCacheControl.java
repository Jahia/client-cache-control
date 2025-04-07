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
