package org.jahia.bundles.cache.client.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.services.render.filter.cache.ClientCachePolicy;

import java.util.Collections;
import java.util.List;

@GraphQLName("ClientCacheStatus")
@GraphQLDescription("Client cache status of the published version of a page, for an anonymous visitor")
public class GqlClientCacheStatus {

    private final boolean published;
    private final ClientCachePolicy policy;
    private final String header;
    private final List<GqlClientCacheFragment> fragments;

    public GqlClientCacheStatus(ClientCachePolicy policy, String header, List<GqlClientCacheFragment> fragments) {
        this.published = true;
        this.policy = policy;
        this.header = header;
        this.fragments = fragments;
    }

    private GqlClientCacheStatus() {
        this.published = false;
        this.policy = null;
        this.header = null;
        this.fragments = Collections.emptyList();
    }

    public static GqlClientCacheStatus notPublished() {
        return new GqlClientCacheStatus();
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("False when a guest cannot read the page in the live workspace. The other fields are then empty.")
    public boolean isPublished() {
        return published;
    }

    @GraphQLField
    @GraphQLDescription("Client cache policy of the page: private, custom, public or immutable")
    public String getPolicy() {
        return policy != null ? policy.getLevel().getValue() : null;
    }

    @GraphQLField
    @GraphQLDescription("TTL in seconds of a custom policy")
    public Integer getTtl() {
        return policy != null && policy.getLevel() == ClientCachePolicy.Level.CUSTOM ? policy.getTtl() : null;
    }

    @GraphQLField
    @GraphQLDescription("Cache-Control header that the policy of the page gives")
    public String getHeader() {
        return header;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Fragments whose policy is stricter than public, the strictest first")
    public List<GqlClientCacheFragment> getFragments() {
        return fragments;
    }
}
