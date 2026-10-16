package org.jahia.bundles.cache.client.filter;

import org.apache.http.HttpHeaders;
import org.jahia.bundles.cache.client.api.ClientCacheMode;
import org.jahia.bundles.cache.client.api.ClientCachePreset;
import org.jahia.bundles.cache.client.api.ClientCacheRule;
import org.jahia.bundles.cache.client.api.ClientCacheService;
import org.jahia.bundles.cache.client.api.ClientCacheTemplate;
import org.jahia.services.render.filter.cache.ClientCachePolicy;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

/**
 * The policy of the preset bounds what a servlet contributes: a request can make a response stricter than its
 * rule, never more permissive.
 *
 * @author Jerome Blanchard
 */
public class ClientCacheFilterPolicyTest {

    private static final String PRIVATE = "private, no-cache, no-store, must-revalidate, proxy-revalidate, max-age=0";
    private static final String PUBLIC = "public, must-revalidate, max-age=1, s-maxage=60, stale-while-revalidate=15";
    private static final String PUBLIC_MEDIUM = "public, must-revalidate, max-age=1, s-maxage=600, stale-while-revalidate=15";
    private static final String CUSTOM = "public, must-revalidate, max-age=1, s-maxage=%%jahiaClientCacheCustomTTL%%, stale-while-revalidate=15";

    @Test
    public void aContributedPrivatePolicyMakesAPublicRuleStricter() throws Exception {
        assertEquals(PRIVATE, serve(new ClientCachePreset(PUBLIC_MEDIUM, ClientCachePolicy.PUBLIC), ClientCachePolicy.PRIVATE));
    }

    @Test
    public void aPrivateRuleStaysPrivateWhateverIsContributed() throws Exception {
        // A custom policy is stricter than a public one, so only the policy of the rule keeps it out.
        assertEquals(PRIVATE, serve(new ClientCachePreset(PRIVATE, ClientCachePolicy.PRIVATE),
                new ClientCachePolicy(ClientCachePolicy.Level.CUSTOM, 30)));
    }

    @Test
    public void aRequestThatContributesNothingKeepsItsRule() throws Exception {
        assertEquals(PUBLIC_MEDIUM, serve(new ClientCachePreset(PUBLIC_MEDIUM, ClientCachePolicy.PUBLIC), null));
    }

    @Test
    public void anErrorOnAPublicRuleIsPrivate() throws Exception {
        ClientCacheFilter filter = new ClientCacheFilter();
        filter.setService(new FakeService(new ClientCachePreset(PUBLIC_MEDIUM, ClientCachePolicy.PUBLIC)));
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        filter.doFilter(request(new HashMap<>()), response(headers),
                (request, response) -> ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND));

        assertEquals(PRIVATE, headers.get(HttpHeaders.CACHE_CONTROL));
    }

    /** The Cache-Control of a response whose servlet contributes {@code contributed} and then writes its body. */
    private static String serve(ClientCachePreset preset, ClientCachePolicy contributed) throws Exception {
        ClientCacheFilter filter = new ClientCacheFilter();
        filter.setService(new FakeService(preset));
        Map<String, Object> attributes = new HashMap<>();
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        FilterChain servlet = (request, response) -> {
            if (contributed != null) {
                request.setAttribute(ClientCacheService.CC_REQUEST_POLICY_ATTR, contributed);
            }
            response.getOutputStream();
        };
        filter.doFilter(request(attributes), response(headers), servlet);
        return headers.get(HttpHeaders.CACHE_CONTROL);
    }

    /** A GET on a file, with the request attributes kept in {@code attributes}. */
    private static HttpServletRequest request(Map<String, Object> attributes) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                ClientCacheFilterPolicyTest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMethod":
                            return "GET";
                        case "getRequestURI":
                        case "getPathInfo":
                            return "/live/sites/mysite/files/doc.pdf";
                        case "getServletPath":
                            return "/files";
                        case "getAttribute":
                            return attributes.get((String) args[0]);
                        case "setAttribute":
                            attributes.put((String) args[0], args[1]);
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /** A response that keeps its headers by case-insensitive name, and is never committed. */
    private static HttpServletResponse response(Map<String, String> headers) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                ClientCacheFilterPolicyTest.class.getClassLoader(),
                new Class<?>[] { HttpServletResponse.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setHeader":
                            headers.put((String) args[0], (String) args[1]);
                            return null;
                        case "getHeader":
                            return headers.get((String) args[0]);
                        case "containsHeader":
                            return headers.containsKey((String) args[0]);
                        case "isCommitted":
                            return false;
                        case "getOutputStream":
                        case "sendError":
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /** One rule, whose preset is given, and the templates of the default configuration. */
    private static final class FakeService implements ClientCacheService {
        private static final Map<String, String> TEMPLATES = Map.of("private", PRIVATE, "public", PUBLIC, "public-medium", PUBLIC_MEDIUM, "custom", CUSTOM);
        private final ClientCachePreset preset;

        FakeService(ClientCachePreset preset) {
            this.preset = preset;
        }

        @Override public ClientCacheMode getMode() {
            return ClientCacheMode.STRICT;
        }

        @Override public List<ClientCacheRule> listRules() {
            return Collections.emptyList();
        }

        @Override public Collection<ClientCacheTemplate> listHeaderTemplates() {
            return Collections.emptyList();
        }

        @Override public String getDefaultCacheControlHeader() {
            return PUBLIC_MEDIUM;
        }

        @Override public ClientCachePreset getDefaultPreset() {
            return new ClientCachePreset(PUBLIC_MEDIUM, ClientCachePolicy.PUBLIC);
        }

        @Override public Optional<String> getCacheControlHeader(String method, String uri, Map<String, String> templateParams) {
            return Optional.of(preset.getCacheControl());
        }

        @Override public Optional<ClientCachePreset> getPreset(String method, String uri, Map<String, String> templateParams) {
            return Optional.of(preset);
        }

        @Override public Optional<String> getCacheControlHeader(String template, Map<String, String> templateParams) {
            String value = TEMPLATES.get(template);
            if (value == null) {
                return Optional.empty();
            }
            for (Map.Entry<String, String> param : templateParams.entrySet()) {
                value = value.replace("%%" + param.getKey() + "%%", param.getValue());
            }
            return Optional.of(value);
        }
    }
}
