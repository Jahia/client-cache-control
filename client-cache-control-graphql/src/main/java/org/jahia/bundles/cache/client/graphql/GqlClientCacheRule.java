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
