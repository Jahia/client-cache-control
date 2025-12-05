---
page:
  $path: /sites/academy/home/documentation/jahia/8_2/developer/rendering-pages-and-content/browser-caching-strategies
  jcr:title: Browser caching strategies
  j:templateName: documentation
content:
  $subpath: document-area/text-3
---

Installed by default, Jahia Client Cache Control module ensures that browsers and intermediates will have adapted information from Jahia 
to cache content on their side. This information is commonly inserted in HTTP response headers. Depending on the Jahia content that is 
return to the client's browser, the headers to setup browser cache behavior will be finely tuned to ensure the best strategy and performances.

### Overview

The Client Cache Control feature manages the HTTP `Cache-Control` header for all resources served by Jahia. It makes sure that browsers, 
proxies and CDNs receive cache directives that are consistent with how Jahia actually caches and personalizes content.

In practice, the module lets you:

- Define Cache-Control response header **templates**. 
- Define Client Cache Policies per **URL pattern** and **HTTP method** using a YAML **ruleset**.
- Define **Client Cache Fragment Policy** for fine grain content that will be computed in the global Jahia page to determine the page **Client Cache Policy**.  
- At the end of request processing, apply defined **templates** on the response headers according the resulted **Client Cache Policy**.
- Choose between different **modes** of operation (strict enforcement vs allow overrides).
- Expose cache ruleset and templates through a **Java service** and **GraphQL API**

### Architecture

A high-level architecture diagram shows principles of integration in the current version:

//TODO add the image link

### Request flow and runtime behavior

The high-level request flow is:

1. **HTTP request received – ClientCacheFilter**
   - The request is intercepted early by `ClientCacheFilter` (a servlet filter applied on `pattern=/*`).
   - The filter enforces a response's wrapper to ensure complete control on the response header.
   - The filter looks up a matching rule based on the HTTP method and request URI.
   - If a rule matches, it **pre‑sets** a `Cache-Control` header on the response using either:
     - a **template** (e.g. `template:public`, `template:private`), or
     - a **literal** header value (e.g. `no-store,no-cache,must-revalidate`).
   - If no rule matches and the response has no `Cache-Control` header yet, a **default header** is applied.

2. **Request continues to a servlet or to the RenderChain**
   - For servlet URLs, preset header won't change if **strict mode** is set.
   - For page rendering, the request enters Jahia's **RenderChain**.
   - At RenderContext creation time, a default **public client cache policy** is set on the `RenderContext`.

3. **Fragment rendering and fragment policies**
   - For each fragment rendered, the `AggregateCacheFilter`:
     - evaluates **cache key parts** via `CacheKeyPartGenerator`s to determine if the fragment requires a more restrictive level of caching (e.g. user‑specific fragments).
     - computes a `ClientCacheFragmentPolicy` and stores it as a property of the fragment's `CacheEntry`.
   - On fragment cache hits, the computed fragment policy is retrieved from the `CacheEntry` instead of recomputing it.

4. **Enforcing fragment policies on the RenderContext**
   - While rendering all fragments, `AggregateCacheFilter` merges fragment policies into the **global client cache policy** on the `RenderContext`.
   - If any fragment requires a stricter policy (e.g. `private` instead of `public`), it **upgrades** the global policy accordingly.

5. **Render filter applies final policy – ClientCacheRenderFilter**
   - At the end of the RenderChain, `ClientCacheRenderFilter` reads the final `ClientCachePolicy` from the `RenderContext` (level + TTL).
   - It then calls `ClientCacheService.getCacheControlHeader(...)` with:
     - the policy **level** (e.g. `public`, `public-medium`, `custom`, `private`), and
     - a custom TTL parameter (`jahiaClientCacheCustomTTL`) if needed.
   - If a suitable template exists, the filter sets a special `Force-Cache-Control` header on the response.
     - This is used to **enforce** the RenderChain policy even when the service is in strict mode (see below).

6. **Client receives a coherent Cache-Control header**
   - The browser / CDN finally sees a `Cache-Control` header that reflects:
     - the URL / method based rules,
     - the actual fragment cache policy (public vs private),
     - the effective TTL.

### Client Cache Control templates

Client cache control defines 5 levels of Cache-Control header templates that can be used in the ruleset. Even if it is not needed to change
the default values of those templates, as it is defined using OSGI configuration it can be modified using the `tools` interface of Jahia to 
access OSGI configuration with pid `org.jahia.bundles.cache.client`

For a complete description of options that can be setup in the Cache-Control response header, please consult a [online reference documentation](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Cache-Control)

#### Template 'private'

```
Cache-Control = "private, no-cache, no-store, must-revalidate, proxy-revalidate, max-age=0";
```

Private value is the most restrictive template. 
It is dedicated to resources that are not cacheable by either intermediates (proxy, cdn) and client's browsers. 
It must be applied on resources that could contain personal or sensitive information.

#### Template 'custom'

```
"public, must-revalidate, max-age=1, s-maxage=%%jahiaClientCacheCustomTTL%%, stale-while-revalidate=15";
```

Custom value is a variant of the public caching strategy with some placeholders that allows customizing the cache duration in intermediates (proxy, cdn).
The client's browser max-age is set to 1s ensuring that the client is going to ask for a newer version of the resource mostly on each request. 
Anyway, for better performances, resource will be cached a little bit longer in intermediates (cdn) to absorb high load on that kind of resources. 
That intermediate cache duration is determine using the Jahia Cache Fragment 
It is used in Jahia pages with template that does not contain private content but a custom cache duration. 

#### Template 'public'

```
"public, must-revalidate, max-age=1, s-maxage=##short.ttl##, stale-while-revalidate=15";
```

Most commonly used strategy for content that can change (pages) but does not contains private or sensitive information. 
This type of resource will be cached in intermediates (CDN) with a duration that is tolerant to update. 
It means that if page is updated, users that queries that page over a CDN server may have a stale version of the page during a time < to the `short.ttl` value.
By default, the **short.ttl = 60s** meaning that it can take a minute for a page modification to be visible to all users.

#### Template 'public-medium'

```
"public, must-revalidate, max-age=1, s-maxage=##medium.ttl##, stale-while-revalidate=15";
```

This template is a very simple variant of the public one but with a longer caching time in CDN, about 10 minutes.
It is meant to be used with pages or site that does not requires a very fast page update propagation.
By default, the **medium.ttl = 600s** meaning that it can take 10 minutes for a page modification to be visible to all users.

#### Template 'immutable'

```
"public, max-age=##immutable.ttl##, s-maxage=##immutable.ttl##, stale-while-revalidate=15, immutable";
```

The `immutable` template is dedicated to resources that are never supposed to change (aka when the url is unique, and will change if the content changes).
All static resources included in pages using the Resource tag are stored and included using unique URLs that will change on any update. Thus, the content 
can be cached into client forever without even asking if content has changed.
It is the most efficient caching strategy but requires unique URLs or filenames.

All those templates can then be used in Client Cache Rules to enforce the expected Cache-Control header on Jahia's resources.

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

The default ruleset is packaged inside the implementation bundle and does not need to be changed unless you know exactly what you are doing.

For Jahia module's custom cache content behavior rules, you can provide another ruleset that it will be combined with the default one (and all other module's one)
Module's custom rule set must be placed in the `/resources/META-INF/configurations` and must follow naming convention: `org.jahia.bundles.cache.client.ruleset-<yourmodulename>.yml`

Rules are combined using a priority, thus, depending of which URLs you want to customize, you'll have to find the best priority to insert your rules in the default ruleset.
Rule priority is a **floating** number so you will always be able to insert your rule at the place you want using a classic ordering (8,99 < 9).

**Typical tuning examples**:

1. **Immutable module embedded resource**

   - Suppose your module contain angular js code that have a changing filename on each generation
   - Angular js code lives at `/modules/<yourmodulename>/js/uniquefilename.js`.
   - You can add a immutable rule with a dedicated template:

   ```yaml
   - "8.99;GET|HEAD;(?:/[^/]+)?/modules/<yourmodulename>/js/uniquefilename.js;template:immutable"
   ```


2. **Resource with cache need that is not available as a template**

   - For a really custom page caching strategy, you can use also use literal header value instead of a template:

   ```yaml
   - "0.2;GET|HEAD;(?:/[^/]+)?/cms/render/live/.*/sites/mysite/secure.*;no-store,no-cache,must-revalidate"
   ```

3. **Custom static assets directory**

   - If you use the media library to upload files with unique name and want to treat that subset of files as immutable:

   ```yaml
   - "6.5;GET|HEAD;(?:/[^/]+)?/files/static-assets/.*;template:immutable"
   ```

You must use a priority that is lower than the one that match a larger URL to ensure that it will be applied as expected: 
If a rule exists for /modules/.* with a priority of 9, you will have to place all your more specific /modules rules with a priority lower 
(8.5 for example). If any other specific rule exist in your Jahia instance, you may use a more precise number to place your rule at the expected
rank (8.5111). 

### GraphQL API

The `client-cache-control-graphql` module exposes a GraphQL API (via Jahia's `graphql-dxm-provider`) to inspect and possibly manage client cache rules and templates.

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

### Limitations, pitfalls and best practices

**Limitations**

- The module focuses on `Cache-Control` (and a special `Force-Cache-Control`) and does not manage `ETag`, `Last-Modified`. Those aspects are 
  dependent on the resource's content and are treated in each specific Servlet accordingly.
- It relies on Jahia internals (RenderChain, AggregateCacheFilter, `ClientCachePolicy`) and is not designed to be used standalone.

**Common pitfalls**

- **Overly broad or high-priority rules**:
  - A single, very generic high-priority rule can accidentally make many pages `public` or `private` when they should not be.
- **Misunderstanding strict mode**:
  - In `STRICT` mode, any component overriding the preset `Cache-Control` header will trigger error logs.

**Best practices**

- Start from the **default ruleset** and adjust gradually.
- Enable DEBUG logging for `ClientCacheFilter` and `ClientCacheRenderFilter` in non‑production environments to observe how rules and policies are applied.
- Use the **GraphQL API** to audit current rules, templates and mode before making major changes.
- Document your custom rules and templates so other teams (ops, integrators, developers) understand the cache strategy.

### Sample demonstration module

A sample module that demonstrate common specific Cache-Control header cache usage is available in the [OSGI-modules-samples project](https://github.com/Jahia/OSGI-modules-samples) 
in the `client-cache-sample` module.
