---
client-cache-control: minor
---

Added the `clientCacheStatus` GraphQL field on a page. It returns the `Cache-Control` header that an anonymous visitor gets for the published page, and each component that makes this header stricter than public, with the reason. The field requires Jahia 8.2.4.
