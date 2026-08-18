/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.bundles.cache.client.filter;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * The path the filter hands to the service for rule matching.
 *
 * @author Jerome Blanchard
 */
public class ClientCacheFilterTest {

    @Test
    public void theResolvedPathIsTheOneTheContainerChoseTheServletWith() {
        // A prefix mapping splits the path in two: the mapped prefix, then the remainder. The container
        // decoded and normalized both, so they are taken as they stand however the request was written.
        assertEquals("/modules/healthcheck",
                ClientCacheFilter.resolvedPath(request("/modules", "/healthcheck", "/modules/healthchec%6b")));
        assertEquals("/modules/tools/index.jsp",
                ClientCacheFilter.resolvedPath(request("/modules", "/tools/index.jsp", "/modules//tools/index.jsp")));
    }

    @Test
    public void anExactMappingCarriesTheWholePathInTheServletPath() {
        assertEquals("/start", ClientCacheFilter.resolvedPath(request("/start", null, "/start")));
    }

    @Test
    public void theRequestUriAnswersWhenTheContainerExposesNeither() {
        // Nothing upstream has decoded or normalized this one, so the filter does it here. Each of these
        // reaches the same resource, so each yields the one path the rules are matched on.
        assertEquals("/modules/healthcheck", ClientCacheFilter.resolvedPath(request("", null, "/modules/healthchec%6b")));
        assertEquals("/modules/healthcheck", ClientCacheFilter.resolvedPath(request(null, null, "/modules/%68ealthcheck")));
        assertEquals("/modules/healthcheck", ClientCacheFilter.resolvedPath(request("", null, "/modules//healthcheck")));
        assertEquals("/modules/tools/index.jsp", ClientCacheFilter.resolvedPath(request("", null, "/modules//tools/index.jsp")));
        assertEquals("/modules/healthcheck", ClientCacheFilter.resolvedPath(request("", null, "/modules/./healthcheck")));
        assertEquals("/modules/healthcheck", ClientCacheFilter.resolvedPath(request("", null, "/modules/x/../healthcheck")));
    }

    @Test
    public void canonicalizeRewritesEverySpellingToOneForm() {
        assertEquals("/modules/healthcheck", ClientCacheFilter.canonicalize("/modules/healthchec%6b"));
        assertEquals("/modules/healthcheck", ClientCacheFilter.canonicalize("/modules//healthcheck"));
        assertEquals("/modules/healthcheck", ClientCacheFilter.canonicalize("/modules/./healthcheck"));
        assertEquals("/modules/healthcheck", ClientCacheFilter.canonicalize("/modules/x/../healthcheck"));
        assertEquals("/modules/tools/index.jsp", ClientCacheFilter.canonicalize("/modules/tool%73//index.jsp"));
        // Decoding runs before dot segments are removed, which is the order the container applies, so an
        // encoded dot segment resolves the same way a written one does.
        assertEquals("/healthcheck", ClientCacheFilter.canonicalize("/modules/%2e%2e/healthcheck"));
        // A multi-byte character written as several percent groups decodes to the character it stands for.
        assertEquals("/files/été.pdf", ClientCacheFilter.canonicalize("/files/%C3%A9t%C3%A9.pdf"));
    }

    @Test
    public void canonicalizeLeavesAPathThatIsAlreadyCanonicalUntouched() {
        String uri = "/modules/ckeditor/javascript/ckeditor.js";
        assertSame(uri, ClientCacheFilter.canonicalize(uri));
        // A percent sign that is not followed by two hexadecimal digits belongs to the name and is kept as
        // it stands. An encoded one is decoded, like any other encoded byte.
        assertEquals("/files/100% done.pdf", ClientCacheFilter.canonicalize("/files/100% done.pdf"));
        assertEquals("/files/50% off.pdf", ClientCacheFilter.canonicalize("/files/50%25 off.pdf"));
        // A plus sign is a plus sign in a path, whatever form encoding makes of it.
        assertSame("/files/a+b.pdf", ClientCacheFilter.canonicalize("/files/a+b.pdf"));
    }

    @Test
    public void canonicalizeDoesNotClimbAboveTheRoot() {
        assertEquals("/etc/passwd", ClientCacheFilter.canonicalize("/../../etc/passwd"));
        assertEquals("/", ClientCacheFilter.canonicalize("/"));
    }

    /** An {@link HttpServletRequest} that answers only the three methods this reads. */
    private static HttpServletRequest request(String servletPath, String pathInfo, String requestUri) {
        Map<String, String> answers = new HashMap<>();
        answers.put("getServletPath", servletPath);
        answers.put("getPathInfo", pathInfo);
        answers.put("getRequestURI", requestUri);
        return (HttpServletRequest) Proxy.newProxyInstance(
                ClientCacheFilterTest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> {
                    if (answers.containsKey(method.getName())) {
                        return answers.get(method.getName());
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
