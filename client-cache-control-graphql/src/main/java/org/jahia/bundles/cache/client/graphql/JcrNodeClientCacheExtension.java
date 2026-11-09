package org.jahia.bundles.cache.client.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.bundles.cache.client.api.ClientCacheService;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.render.RenderException;

import javax.jcr.RepositoryException;

@GraphQLTypeExtension(GqlJcrNode.class)
public class JcrNodeClientCacheExtension {

    private static final String PERMISSION = "jContentAccess";

    private final GqlJcrNode node;

    public JcrNodeClientCacheExtension(GqlJcrNode node) {
        this.node = node;
    }

    @GraphQLField
    @GraphQLName("clientCacheStatus")
    @GraphQLDescription("Cache-Control that an anonymous visitor gets for the published version of this page, and the fragments that make it stricter than public. The page is rendered as guest in the live workspace.")
    public GqlClientCacheStatus getClientCacheStatus(@GraphQLName("language") @GraphQLNonNull @GraphQLDescription("Language of the rendered page") String language,
                                                     DataFetchingEnvironment environment) {
        try {
            if (!node.getNode().getResolveSite().hasPermission(PERMISSION)) {
                throw new DataFetchingException("Permission denied: " + PERMISSION);
            }
            ClientCacheService service = BundleUtils.getOsgiService(ClientCacheService.class, null);
            return new ClientCacheStatusRenderer(service).render(node.getNode().getIdentifier(), language,
                    ContextUtil.getHttpServletRequest(environment.getGraphQlContext()),
                    ContextUtil.getHttpServletResponse(environment.getGraphQlContext()));
        } catch (RepositoryException | RenderException e) {
            throw new DataFetchingException(e);
        }
    }
}
