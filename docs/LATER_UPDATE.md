### CloudFront integration

The `client-cache-control-cloudfront` module provides a service that integrates with **AWS CloudFront** using the AWS SDK v2 (`software.amazon.awssdk:cloudfront`).

- `CloudFrontClientService` – OSGi service for interacting with CloudFront (e.g. triggering invalidations, mapping Jahia paths to CloudFront distribution paths).
- `CloudFrontClientServiceException` – Custom exception for CloudFront-related failures (authentication, rate limiting, invalid parameters, etc.).

This integration is meant to complement the client cache control logic by:

- letting CloudFront **respect** the origin `Cache-Control` headers produced by the module, and
- allowing Jahia to request **explicit invalidations** in CloudFront when content is updated.

To use it, you must:

- configure AWS credentials and CloudFront distribution IDs via OSGi configuration,
- ensure your CloudFront behaviors are set to consider the `Cache-Control` header from the origin,
- possibly implement a `ClientCacheInvalidationProvider` that delegates to `CloudFrontClientService`.

