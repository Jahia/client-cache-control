package org.jahia.bundles.cache.client.api;

import org.jahia.services.render.filter.cache.ClientCachePolicy;

import java.util.Objects;

/**
 * The Cache-Control a rule presets for a request, and the policy of the template the rule names. A rule that
 * states a literal value names no template, and its policy is {@link ClientCachePolicy#DEFAULT}.
 *
 * @author Jerome Blanchard
 */
public final class ClientCachePreset {

    private final String cacheControl;
    private final ClientCachePolicy policy;

    public ClientCachePreset(String cacheControl, ClientCachePolicy policy) {
        this.cacheControl = cacheControl;
        this.policy = policy;
    }

    public String getCacheControl() {
        return cacheControl;
    }

    public ClientCachePolicy getPolicy() {
        return policy;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClientCachePreset that = (ClientCachePreset) o;
        return Objects.equals(cacheControl, that.cacheControl) && Objects.equals(policy, that.policy);
    }

    @Override public int hashCode() {
        return Objects.hash(cacheControl, policy);
    }

    @Override public String toString() {
        return "ClientCachePreset{" + "cacheControl='" + cacheControl + '\'' + ", policy=" + policy + '}';
    }
}
