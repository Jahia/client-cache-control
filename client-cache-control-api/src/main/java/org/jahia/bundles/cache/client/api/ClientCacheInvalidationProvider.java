package org.jahia.bundles.cache.client.api;

/**
 * @author Jerome Blanchard
 */
public interface ClientCacheInvalidationProvider {

    String getName();

    void purge() throws ClientCacheException;

    void invalidate(String path) throws ClientCacheException;

    void invalidate(String[] paths) throws ClientCacheException;

}
