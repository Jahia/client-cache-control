---
page:
  $path: /sites/academy/home/documentation/jahia/8_2/developer/rendering-pages-and-content/browser-caching-strategies
  jcr:title: Browser caching strategies
  j:templateName: documentation
content:
  $subpath: document-area/text-3
---

Installed by default, Jahia Client Cache Control module ensures that browsers and intermediates will have adapted information from Jahia (in response headers) to cache content on their side.

### Overview

The Client Cache Control feature manages the HTTP `Cache-Control` header for all resources served by Jahia. It makes sure that browsers, proxies and CDNs receive cache directives that are consistent with how Jahia actually caches and personalizes content.

In practice, the module lets you:

- Define cache policies per **URL pattern** and **HTTP method** using a YAML ruleset.
- Align the browser / CDN cache behavior with the **Jahia fragment cache** (RenderChain and AggregateCacheFilter).
- Choose between different **modes** of operation (strict enforcement vs allow overrides).
- Expose/cache rules and templates through a **Java service** and **GraphQL API**

### Architecture

A high-level architecture diagram shows principles of integration in the current version:

//TODO add the image link

### Request flow and runtime behavior

The high-level request flow is:

1. **HTTP request received – ClientCacheFilter**
   - The request is intercepted early by `ClientCacheFilter` (a servlet filter applied on `pattern=/*`).
   - The filter looks up a matching rule based on the HTTP method and request URI.
   - If a rule matches, it **pre‑sets** a `Cache-Control` header on the response using either:
     - a **template** (e.g. `template:public`, `template:private`), or
     - a **literal** header value (e.g. `no-store,no-cache,must-revalidate`).
   - If no rule matches and the response has no `Cache-Control` header yet, a **default header** is applied.

2. **Request continues to a servlet or to the RenderChain**
   - For some URLs, a dedicated servlet can still override the header when needed (e.g. `CsrfServlet`).
   - For page rendering, the request enters Jahia's **RenderChain**.
   - At RenderContext creation time, a default **public client cache policy** is set on the `RenderContext`.

3. **Fragment rendering and fragment policies**
   - For each fragment rendered, the `AggregateCacheFilter`:
     - evaluates **cache key parts** via `CacheKeyPartGenerator`s to determine if the fragment requires a `private` level of caching (e.g. user‑specific fragments).
     - computes a `ClientCacheFragmentPolicy` and stores it as a property of the fragment's `CacheEntry`.
   - On cache hits, the computed fragment policy is retrieved from the `CacheEntry` instead of recomputing it.

4. **Enforcing fragment policies on the RenderContext**
   - While rendering all fragments, `AggregateCacheFilter` merges fragment policies into the **global client cache policy** on the `RenderContext`.
   - If any fragment requires a stricter policy (e.g. `private` instead of `public`), it **upgrades** the global policy accordingly.

5. **Render filter applies final policy – ClientCacheRenderFilter**
   - At the end of the RenderChain, `ClientCacheRenderFilter` reads the final `ClientCachePolicy` from the `RenderContext` (level + TTL).
   - It then calls `ClientCacheService.getCacheControlHeader(...)` with:
     - the policy **level** (e.g. `public`, `private`), and
     - a custom TTL parameter (`jahiaClientCacheCustomTTL`).
   - If a suitable template exists, the filter sets a special `Force-Cache-Control` header on the response.
     - This is used to **enforce** the RenderChain policy even when the service is in strict mode (see below).

6. **Client receives a coherent Cache-Control header**
   - The browser / CDN finally sees a `Cache-Control` header that reflects:
     - the URL / method based rules,
     - the actual fragment cache policy (public vs private),
     - the effective TTL.

### Core Java API

All API classes live in `org.jahia.bundles.cache.client.api` (module `client-cache-control-api`). The key types are:

#### `ClientCacheService`

The central service used by filters and external integrations:

- **Constants**:
  - `CC_SET_ATTR` – Request attribute indicating that a client cache header was already set.
  - `CC_ORIGINAL_REQUEST_URI_ATTR` – Stores the original request URI (before rewrites).
  - `CC_CUSTOM_TTL_ATTR` – Request/template parameter key used to pass a custom TTL.
- **Configuration & inspection**:
  - `ClientCacheMode getMode()` – Returns the current mode (e.g. strict or allow overrides).
  - `List<ClientCacheRule> listRules()` – Lists all active rules.
  - `Collection<ClientCacheTemplate> listHeaderTemplates()` – Lists available header templates.
  - `String getDefaultCacheControlHeader()` – Returns the default header value used as fallback.
- **Header resolution**:
  - `Optional<String> getCacheControlHeader(String method, String uri, Map<String,String> templateParams)`
    - Finds the best matching rule for a method + URI, then resolves the appropriate header.
  - `Optional<String> getCacheControlHeader(String templateName, Map<String,String> templateParams)`
    - Resolves a header for a given template name (e.g. `public`, `private`, `immutable`).

#### `ClientCacheRule`

An abstract description of a cache rule:

- `float getPriority()` – Rule priority; lower numbers mean higher priority.
- `Set<String> getMethods()` – Allowed HTTP methods for the rule.
- `String getUrlRegexp()` – Java regular expression applied to `request.getRequestURI()`.
- `String getHeader()` – Either:
  - `template:<name>` to refer to a `ClientCacheTemplate`, or
  - a full `Cache-Control` header value to apply as‑is.

Implementations of this abstract class are created from configuration (e.g. YAML rules) in the implementation bundle.

#### Other API types

- `ClientCacheMode` – Enum representing the global mode (e.g. `STRICT`, `ALLOW_OVERRIDES`, possibly `DISABLED`).
- `ClientCacheTemplate` – Describes a named header template (e.g. `public`, `public-medium`, `private`, `immutable`) and how it expands into a concrete `Cache-Control` string (including TTL and directives).
- `ClientCacheAction` – Represents possible cache actions (invalidation, refresh, etc.).
- `ClientCacheInvalidationProvider` – SPI for services that propagate invalidations to external systems (e.g. CloudFront).
- `ClientCacheException` – Custom exception type for this module.

### Implementation details and filters

The implementation bundle (`client-cache-control-bundle`) contains the actual filters, service implementations and configuration logic.

#### Servlet filter: `ClientCacheFilter`

Package: `org.jahia.bundles.cache.client.filter`.

- Registered as an OSGi component:
  - Service type: `AbstractServletFilter`.
  - Properties: `pattern=/*`, `immediate = true`.
  - Order: `-3f` (runs early in the filter chain).
- Behavior:
  1. Wraps the response in `ClientCacheResponseWrapper` to control header changes.
  2. Stores `jahiaOriginalRequestURI` on the request.
  3. Queries `ClientCacheService.getCacheControlHeader(method, uri, emptyMap)`.
     - If a match is found:
       - Sets `Cache-Control` to the resolved value.
       - If mode is `STRICT`:
         - Marks filtered headers as read‑only in the wrapper.
         - Sets `jahiaCacheControlSet = "done"` (for legacy rewrite rules).
     - If no match and the response has no `Cache-Control` yet:
       - Uses `getDefaultCacheControlHeader()` and sets it.
     - If a `Cache-Control` header already exists and there is no matching rule, it is left untouched (with a warning log).
  4. Invokes the remaining filter chain.
  5. After the chain returns, it checks whether the header was modified compared to the preset value:
     - In `ALLOW_OVERRIDES` mode, this is logged as debug.
     - In `STRICT` mode, any override/removal is logged as an error.

This filter essentially enforces URL‑ and method‑based presets while tracking and optionally forbidding overrides from other components.

#### Render filter: `ClientCacheRenderFilter`

Package: `org.jahia.bundles.cache.client.render`.

- Registered as a Jahia `RenderFilter` and OSGi component.
- Activated with:
  - Priority `-1.5f`.
  - Disabled in edit mode.
  - Skipped on AJAX requests.
  - Applied only on configuration `page` and template types `html, html-*`.

Behavior:

- `prepare(...)` phase:
  - If the response already contains a `Cache-Control` header starting with `private`, it calls:
    - `renderContext.computeClientCachePolicy(ClientCachePolicy.PRIVATE)`.
  - This ensures that the internal fragment cache policy is aligned with the header already set earlier.

- `execute(...)` phase:
  1. Logs the final `ClientCachePolicy` (level + TTL) computed by Jahia for the page and all its fragments.
  2. Calls `ClientCacheService.getCacheControlHeader(level, Map.of(CC_CUSTOM_TTL_ATTR, ttl))`.
  3. If a suitable header is found, sets `Force-Cache-Control` on the response.
     - This bypasses strict mode rules for preset headers because the final RenderChain policy must always be enforced.
  4. If no header is found for the given level, logs a warning.

The result is that the final browser/ CDN caching behavior is determined both by URL rules and by actual fragment behavior.

### Configuration via YAML ruleset

The default ruleset lives in:

- `client-cache-control-bundle/src/main/resources/META-INF/configurations/org.jahia.bundles.cache.client.ruleset-default.yml`.

This file defines a **ruleset** with name, description and an ordered list of rules:

```yaml
name: "Default ruleset"
description: "Default ruleset"
rules:
  - "1;GET|HEAD;(?:/[^/]+)?/cms/render/live/.*;template:public"
  - "2;GET|HEAD;(?:/[^/]+)?/cms/.*;template:private"
  - "3;GET|HEAD;(?:/[^/]+)?/welcome.*;template:private"
  - "4;GET|HEAD;(?:/[^/]+)?/start;template:private"
  - "5;GET|HEAD;(?:/[^/]+)?/validateTicket;template:private"
  - "6;GET|HEAD;(?:/[^/]+)?/administration.*;template:private"
  - "7;GET|HEAD;(?:/[^/]+)?/files/.*;template:public-medium"
  - "8;GET|HEAD;(?:/[^/]+)?/repository/.*;template:public-medium"
  - "9;GET|HEAD;(?:/[^/]+)?/modules/.*;template:public-medium"
  - "10;GET|HEAD;(?:/[^/]+)?/engines/.*\\.jsp(\\?.*)?;template:private"
  - "11;GET|HEAD;(?:/[^/]+)?/tools(/.*)?;template:private"
  - "12;GET|HEAD;(?:/[^/]+)?/gwt/.*\\.nocache\\..*;template:private"
  - "13;GET|HEAD;(?:/[^/]+)?/generated-resources/.*;template:immutable"
  - "14;POST|DELETE|PATCH;.*;template:private"
  - "15;GET|HEAD;.*;template:public"
```

Each rule string has **four segments**, separated by semicolons (`;`):

1. **Priority** – numeric (int or float). Lower values mean the rule is evaluated first.
2. **Methods** – list of HTTP methods separated by `|` (e.g. `GET|HEAD`).
3. **URL regexp** – regular expression matched against the request URI.
4. **Header spec** – either a `template:<name>` reference or a literal `Cache-Control` header value.

Rules are loaded, merged (with rules coming from other modules, if any), ordered by priority and then evaluated to find the first match.

#### Default behavior encoded in the ruleset

- Page rendering in **live** mode:
  - `GET|HEAD` on `/cms/render/live/...` → `template:public`.
- Other `/cms/...` URLs:
  - `GET|HEAD` on `/cms/...` → `template:private`.
- Welcome and start pages, ticket validation, admin, tools, GWT, engines:
  - Treated as **private**.
- Static assets and modules:
  - `/files/.*`, `/repository/.*`, `/modules/.*` → `template:public-medium`.
- Generated resources:
  - `/generated-resources/.*` → `template:immutable` (strong caching).
- Mutating operations:
  - `POST|DELETE|PATCH;.*` → `template:private`.
- Fallback rule:
  - `GET|HEAD;.*` → `template:public`.

#### Where to place and how to customize the ruleset

- The default ruleset is packaged inside the implementation bundle.
- In a typical Jahia installation, you can:
  - copy this YAML file to Karaf's configuration directory (e.g. `/var/jahia/karaf/etc`), or
  - manage it via OSGi ConfigAdmin.
- Once externalized, you can safely adjust or add rules without touching the bundle itself.

**Typical tuning examples**:

1. **Highly cacheable marketing landing page**

   - Suppose your marketing homepage lives at `/cms/render/live/en/sites/mysite/landing`.
   - You can add a high‑priority rule with a dedicated template:

   ```yaml
   - "0.5;GET|HEAD;(?:/[^/]+)?/cms/render/live/.*/sites/mysite/landing.*;template:public-long"
   ```

   - Then define `public-long` in the `ClientCacheService` configuration to use a long TTL.

2. **Sensitive page with no cache at all**

   - For a secure page, you can use a literal header value instead of a template:

   ```yaml
   - "0.2;GET|HEAD;(?:/[^/]+)?/cms/render/live/.*/sites/mysite/secure.*;no-store,no-cache,must-revalidate"
   ```

3. **Custom static assets directory**

   - To treat a subset of files as immutable:

   ```yaml
   - "6.5;GET|HEAD;(?:/[^/]+)?/files/static-assets/.*;template:immutable"
   ```

### Installation and deployment

The feature is **included by default** in supported Jahia versions (>= 8.2.2.0). However, you may want to deploy a specific version or patch.

#### Build

From the project root:

```bash
mvn clean install
```

This produces:

- `client-cache-control-api/target/org.jahia.bundles.client-cache-control-api-<version>.jar`
- `client-cache-control-bundle/target/org.jahia.bundles.client-cache-control-impl-<version>.jar`
- `client-cache-control-feature/target/client-cache-control-<version>.kar`
- Additional bundles (`client-cache-control-graphql`, `client-cache-control-cloudfront`) if enabled.

#### Deploying using a Karaf feature (kar file)

> **Note:** This is not recommended for regular usage, but can be useful for development or testing.

```bash
docker cp ./client-cache-control-feature/target/client-cache-control-<version>.kar \
  jahia:/var/jahia/karaf/deploy
```

Karaf will install the feature and deploy the included bundles. Be aware that having multiple versions of the same bundles in the container can lead to unexpected behavior.

#### Deploying bundles individually

You can update the API and implementation bundles separately:

- API bundle:

```bash
docker cp ./client-cache-control-api/target/org.jahia.bundles.client-cache-control-api-<version>.jar \
  jahia:/var/jahia/karaf/deploy
```

- Implementation bundle:

```bash
docker cp ./client-cache-control-bundle/target/org.jahia.bundles.client-cache-control-impl-<version>.jar \
  jahia:/var/jahia/karaf/deploy
```

#### Deploying the configuration file

You can deploy or update the default ruleset configuration as well:

```bash
docker cp ./client-cache-control-bundle/target/classes/META-INF/configurations/org.jahia.bundles.cache.client.ruleset-default.yml \
  jahia:/var/jahia/karaf/deploy
```

In a typical setup, you would instead place (and maintain) this file under `karaf/etc` so that it participates cleanly in OSGi configuration management.

### GraphQL API

The `client-cache-control-graphql` module exposes a GraphQL API (via Jahia's `graphql-dxm-provider`) to inspect and possibly manage client cache rules and templates.

Key classes:

- `GqlClientCacheRule` – GraphQL type for cache rules (priority, methods, URL regexp, header/template).
- `GqlClientCacheTemplate` – GraphQL type for header templates.
- `GqlClientCacheControl` – Root object grouping cache‑related fields.
- `GqlClientCacheExtensionProvider` – Registers the extension into the GraphQL schema.
- `JahiaAdminQueryExtension` – Exposes the functionality under an admin namespace.

Typical usage (conceptual example):

- Query rules and templates:

```graphql
query {
  admin {
    clientCacheControl {
      rules {
        priority
        methods
        urlRegexp
        header
      }
      templates {
        name
        header
      }
      mode
    }
  }
}
```

- Depending on version, mutations may be provided to update templates or rules. These operations should be restricted to administrators.

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

### Testing and validation

The repository includes:

- A **Jahia test module** under `tests/jahia-module` that deploys content/templates designed to exercise various cache scenarios.
- **Cypress end-to-end tests** under `tests/cypress` that:
  - hit different URLs (live pages, preview/edit, admin, assets, etc.),
  - verify the produced `Cache-Control` headers.

The README in the root `tests` folder suggests:

```bash
cd tests/jahia-module
mvn clean package

docker cp ./target/client-cache-control-test-template-<version>.jar \
  jahia:/var/jahia/modules

cd ..
./set-env.sh
yarn install
yarn run e2e:debug
```

These tests are a good starting point to understand the expected default behavior and to validate your own tuning.

### Limitations, pitfalls and best practices

**Limitations**

- The module focuses on `Cache-Control` (and a special `Force-Cache-Control`) and does not manage `ETag`, `Last-Modified` or full CDN provisioning.
- It relies on Jahia internals (RenderChain, AggregateCacheFilter, `ClientCachePolicy`) and is not designed to be used standalone.

**Common pitfalls**

- **Multiple bundle versions**:
  - Deploying multiple versions of the same client cache bundles in Karaf can lead to unpredictable behavior.
- **Overly broad or high-priority rules**:
  - A single, very generic high-priority rule can accidentally make many pages `public` or `private` when they should not be.
- **Misunderstanding strict mode**:
  - In `STRICT` mode, any component overriding the preset `Cache-Control` header will trigger error logs.
- **Fragment vs. page policy mismatch**:
  - Setting long TTLs for pages that contain personalized fragments may cause user-specific content to be cached in shared caches if the fragment policy is not correctly propagated.

**Best practices**

- Start from the **default ruleset** and adjust gradually.
- Enable DEBUG logging for `ClientCacheFilter` and `ClientCacheRenderFilter` in non‑production environments to observe how rules and policies are applied.
- Use the **GraphQL API** to audit current rules, templates and mode before making major changes.
- When using **CloudFront**:
  - Configure it to honor origin `Cache-Control` headers.
  - Use invalidation mechanisms (possibly via `CloudFrontClientService`) for content that must be refreshed ahead of TTL expiry.
- Document your custom rules and templates so other teams (ops, integrators, developers) understand the cache strategy.
