# Jahia Client Cache Control Bundle

This bundle manage configuration for Cache-Control header and propose optimisation for CDN integration

### Overview

![Client Cache Bundle Overview](./doc/ClientCacheBundle.jpg)

1. The HTTP request is received and intercepted by the ClientCacheFilter
    1. Depending on the configured ClientCacheFilterRule and the request URL some specific Cache-Control header is preset on the response
2. The request continues over a specific Servlet or to the RenderChain
    1. If a specific servlet take the request, it can override the header if needed (done in the CsrfServlet for exemple)
    2. If the RenderChain is involved, when the RenderContext is created, a default 'public' ClientCachePolicy is populated in the RenderContext
3. For each fragment involved in the rendering :
    1. If the content is no already in cache, the AggregateCacheFilter call a method on each CacheKeyPartGenerator that is relevant for the fragment (those that don't have an empty value) to ensure is the existing part requires a 'private' level of caching. The result is a ClientCacheFragmentPolicy that is stored in the properties of the CacheEntry
    2. If the content is already in cache, the already calculated ClientCacheFragmentPolicy is retrieved from the CacheEntry properties, avoiding a call to each key part generator again.
4. For each fragment involved in the rendering, the AggregateCacheFilter enforce the ClientCacheFragmentPolicy on the RenderContext. If any encountered fragment policy is stringer than the already existing one, it is replaced
5. At the end of the rendering chain, the ClientCacheRenderFilter apply the RenderContext ClientCachePolicy to the response header using the values configured in the ClientCacheService.
6. The Client get the response with a Cache-Control header that reflect specificity of the resource.

### Configuration options

All the configurations are done in the org.jahia.bundles.cache.client.config.cfg file.

Policies are defined using the following format:

```properties
## Policy processing index : policies with the lowest index are treated first
policies.<policyname>.index=
## Policy name : a relevant name for the policy (used for logging)
policies.<policyname>.name=
## Policy description : a description of the policy 
policies.<policyname>.description=Default cache strategy for render chain, JCR, files and modules (could be override in RenderChain)
## Policy methods pattern : a regular expression to match the HTTP methods that should be concerned by the policy
policies.<policyname>.methodsPattern=^(GET|HEAD)$
## Policy URL pattern : a regular expression to match the URL that should be concerned by the policy
policies.<policyname>.urlPattern=^/(cms/render/live|files|repository|modules)/.*$
## Policy Cache-Control header value : the cache control strategy to apply for that policy
policies.<policyname>.clientCachePolicy=public
```

Policies are applied in the order of their index, and the first one that match the request is applied. 

4 levels of caching are available: 
- private: the content is not supposed to be cached and should be revalidated each time
- public: the content can be cached for a small amount of time and may need revalidation. Caching delay is configured and balanced between performance and freshness regarding intermediate caches (CDN, proxy, ...)
- custom: the content can be cached but may need revalidation. Caching delay is defined by user template caching properties
- immutable: the content is not supposed to change and can be cached for a long time (1 year)

Default caching duration for those levels are defined in the org.jahia.bundles.cache.client.config.cfg file.

### Header override

Due to impossibility to change a response header once some content is written to the response, the ClientCacheFilter can't override a Cache-Control header that is already set by a servlet or a filter.
The Client Cache Filter aims to preset the Cache-Control Header according to configured policies but if any other servlet/filter override that header after, it will override the preset value. 

### CDN consideration

CDN (intermediate proxy that cache content to speed up delivery) can be used to cache content and reduce server load.
The bundle is designed to work with CDN and propose a generic Cache-Control header that common CDN can understand natively without any specific configuration. 
Thus, any CDN should work more or less out of the box with Jahia.

### History of changes

In version of Jahia prior to 8.2.2.0, Cache-Control headers was defined using URLRewrite for those regarding Rendered content and in each specific servlets/filters for others (Files, Generated Assets, ...) Thus, if you had custom caching strategy defined in the url-rewrite-rules.xml, you will have to migrate them to the new configuration. 
