---
page:
  $path: /sites/academy/home/documentation/jahia/8_2/developer/specific/about-caching
  jcr:title: About caching
  j:templateName: documentation
content:
  $subpath: document-area/text
---

## Client (browser) and Intermediates (CDN, proxy) cache layer

While integrated in the browser rather than Jahia, the client (and intermediates) cache plays a critical role in guaranteeing good performance for the end-user. 
For example, Jahia's usage of the GWT framework makes it possible for AJAX source code to be aggressively cached in the browser cache. 
This ensures that unchanged script code is not reloaded. Jahia also properly manages the client cache to make sure it doesn't cache page content that has changed. 
Jahia also controls expiration times for cached content so that the browser doesn't request content that is rarely changed. 
In the end Jahia is able to set up dedicated intermediate cache rules for content URLs, which allows the use of CDNs to further improve performance for end-users.

A dedicated module centralizes browser cache rules according to Jahia content URLs. 
The client caching strategy is preset by a filter according to matching rule. 
Custom rules can be included to customize caching behavior for specific module content URLs. 
Using the filter's 'strict' mode ensures that other code (modules) cannot update defined header values.

More information can be found in the [Browser Caching Control](/documentation/jahia-cms/jahia-8-2/developer/rendering-pages-and-content/browser-client-caching-control).
