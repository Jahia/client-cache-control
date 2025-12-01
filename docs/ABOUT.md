---
page:
  $path: /sites/academy/home/documentation/jahia/8_2/developer/specific/about-caching
  jcr:title: About caching
  j:templateName: documentation
content:
  $subpath: document-area/text
---

## The browser cache layer

While integrated in the browser rather than Jahia, the browser cache plays a critical role in guaranteeing good performance for the end-user. For example, Jahia's usage of the GWT framework makes it possible for AJAX source code to be aggressively cached in the browser cache. This ensures that unchanged script code is not reloaded. Jahia also properly manages the browser cache to make sure it doesn't cache page content that has changed. Jahia also controls expiration times for cached content so that the browser doesn't request content that is rarely changed.

A dedicated module centralize browser cache rules according to Jahia content URLs and ensure that it cannot be overwrite by other components. Anyway it possible to customize behavior for some specific module's URLs using a specific ruleset.

More information can be found in the [Browser Caching Control](/sites/academy/home/documentation/jahia/8_2/developer/rendering-pages-and-content/browser-caching-strategies).
