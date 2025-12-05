### CloudFront integration

For caching behavior, Jahia Client-Cache-Control will integrate silently with any CDN as it uses standard Cache-Control header options to 
control intermediates behavior (s-max-age). 
Anyway, in specific cases, it can be needed to **invalidate** all or a part of the cache. 

As the invalidation is specific to a CDN provider, a dedicated bundle is used: The `client-cache-control-cloudfront` module provides a 
service that integrates with **AWS CloudFront** using the AWS SDK v2 (`software.amazon.awssdk:cloudfront`).

- `CloudFrontClientService` – OSGi service for interacting with CloudFront (e.g. triggering invalidations, mapping Jahia paths to CloudFront distribution paths).
- `CloudFrontClientServiceException` – Custom exception for CloudFront-related failures (authentication, rate limiting, invalid parameters, etc.).

This integration is meant to complement the client cache control logic by:

- allowing Jahia to request **explicit invalidations** in CloudFront when content is updated.
- integrate invalidation for specific URLs to the Jahia publication process allowing longer TTL for CDN cache (Work In Progress)

To use it, you must:

- configure AWS credentials and CloudFront distribution IDs via OSGi configuration,
- ensure your CloudFront behaviors are set to consider the `Cache-Control` header from the origin,
- possibly implement a `ClientCacheInvalidationProvider` that delegates to `CloudFrontClientService`.

