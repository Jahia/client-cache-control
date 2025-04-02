# Jahia Client Cache Control Bundle

### Overview

![Client Cache Bundle Overview](./doc/ClientCacheBundle.jpg)

1. The HTTP request is received and intercepted by the ClientCacheFilter
    1. Depending on the configured ClientCacheFilterRule and the request URL some specific Cache-Control header is preset on the response
2. The request continues over a specific Servlet or to the RenderChain
    1. If a specific servlet take the request, it can override the header if needed (done in the CsrfServlet for exemple)
    2. If the RenderChain is involved, when the RenderContext is created, a default 'public' ClientCachePolicy is populated in the RenderContext
3. For each fragment involved in the rendering :
    1. If the content is not already in cache, the AggregateCacheFilter call a method on each CacheKeyPartGenerator that is relevant for the fragment (those that don't have an empty value) to ensure is the existing part requires a 'private' level of caching. The result is a ClientCacheFragmentPolicy that is stored in the properties of the CacheEntry
    2. If the content is already in cache, the already calculated ClientCacheFragmentPolicy is retrieved from the CacheEntry properties, avoiding a call to each key part generator again.
4. For each fragment involved in the rendering, the AggregateCacheFilter enforce the ClientCacheFragmentPolicy on the RenderContext. If any encountered fragment policy is stringer than the already existing one, it is replaced
5. At the end of the rendering chain, the ClientCacheRenderFilter apply the RenderContext ClientCachePolicy to the response header using the values configured in the ClientCacheService.
6. The Client get the response with a Cache-Control header that reflect specificity of the resource.
