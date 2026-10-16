---
client-cache-control: patch
---

Changed cache headers so content can make a response less cacheable than its URL rule, and never more cacheable.

Error responses and temporary redirects are never stored by browsers or shared caches. In strict mode, the configured cache headers hold whatever form a component uses to set them.
