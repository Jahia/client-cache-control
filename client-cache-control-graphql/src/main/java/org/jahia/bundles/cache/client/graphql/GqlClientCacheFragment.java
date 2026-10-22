package org.jahia.bundles.cache.client.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.services.render.filter.cache.ClientCachePolicy;
import org.jahia.services.render.filter.cache.ClientCacheTrace;

import java.util.List;

@GraphQLName("ClientCacheFragment")
@GraphQLDescription("A fragment whose client cache policy is stricter than public")
public class GqlClientCacheFragment {

    private final ClientCacheTrace.Decision decision;

    public GqlClientCacheFragment(ClientCacheTrace.Decision decision) {
        this.decision = decision;
    }

    @GraphQLField
    @GraphQLDescription("Path of the rendered node")
    public String getPath() {
        return decision.getPath();
    }

    @GraphQLField
    @GraphQLDescription("UUID of the rendered node")
    public String getUuid() {
        return decision.getUuid();
    }

    @GraphQLField
    @GraphQLDescription("Primary node type of the rendered node")
    public String getNodeType() {
        return decision.getNodeType();
    }

    @GraphQLField
    @GraphQLDescription("Key of the view that rendered the node")
    public String getView() {
        return decision.getView();
    }

    @GraphQLField
    @GraphQLDescription("Module of the view that rendered the node")
    public String getModule() {
        return decision.getModule();
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Client cache policy of the fragment: private or custom")
    public String getPolicy() {
        return decision.getPolicy().getLevel().getValue();
    }

    @GraphQLField
    @GraphQLDescription("TTL in seconds of a custom policy")
    public Integer getTtl() {
        return decision.getPolicy().getLevel() == ClientCachePolicy.Level.CUSTOM ? decision.getPolicy().getTtl() : null;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Reason of the policy: CACHE_PRIVATE, CACHE_EXPIRATION, KEY_PART, NOT_CACHEABLE, BYPASS, ERROR or UNKNOWN")
    public String getReason() {
        return decision.getReason().name();
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("For the reason KEY_PART, the parts of the cache key that return a stricter policy")
    public List<String> getKeyParts() {
        return decision.getKeyParts();
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("True when the fragment came from the HTML cache")
    public boolean isFromCache() {
        return decision.isFromCache();
    }
}
