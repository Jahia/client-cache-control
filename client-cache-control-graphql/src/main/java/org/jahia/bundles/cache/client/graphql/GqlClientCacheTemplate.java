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
import org.jahia.bundles.cache.client.api.ClientCacheTemplate;

/**
 * @author Jerome Blanchard
 */
@GraphQLName("GqlClientCacheTemplate")
@GraphQLDescription("Client Cache-Control header template")
public class GqlClientCacheTemplate {

    private final ClientCacheTemplate template;

    public GqlClientCacheTemplate(ClientCacheTemplate template) {
        this.template = template;
    }

    @GraphQLField
    @GraphQLDescription("Template name")
    public String getName() {
        return template.getName();
    }

    @GraphQLField
    @GraphQLDescription("Template value")
    public String getValue() {
        return template.getTemplate();
    }

}
