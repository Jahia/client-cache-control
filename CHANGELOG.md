# client-cache-control Changelog

## 9.0.3

* Added maven.jahia.com to the Maven repositories client-cache-control uses to download its dependencies.

* Changed cache headers so content can make a response less cacheable than its URL rule, and never more cacheable.

  Error responses and temporary redirects are never stored by browsers or shared caches. In strict mode, the configured cache headers hold whatever form a component uses to set them.

## 9.0.2

* Hardened client cache policy handling: a resource is served with the same Cache-Control policy whichever equivalent form of its URL is requested.
