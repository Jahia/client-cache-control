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
