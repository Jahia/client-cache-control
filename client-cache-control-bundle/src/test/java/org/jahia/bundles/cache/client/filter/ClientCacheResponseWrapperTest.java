package org.jahia.bundles.cache.client.filter;

import org.apache.http.HttpHeaders;
import org.jahia.bundles.cache.client.api.ClientCacheMode;
import org.jahia.bundles.cache.client.api.ClientCachePreset;
import org.jahia.bundles.cache.client.api.ClientCacheRule;
import org.jahia.bundles.cache.client.api.ClientCacheService;
import org.jahia.bundles.cache.client.api.ClientCacheTemplate;
import org.jahia.services.render.filter.cache.ClientCachePolicy;
import org.junit.Test;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * How the wrapper guards the cache headers, and applies the policy a request contributes.
 *
 * @author Jerome Blanchard
 */
public class ClientCacheResponseWrapperTest {

    private static final String PRIVATE = "private, no-cache, no-store, must-revalidate, proxy-revalidate, max-age=0";
    private static final String PUBLIC = "public, must-revalidate, max-age=1, s-maxage=60, stale-while-revalidate=15";
    private static final String PUBLIC_MEDIUM = "public, must-revalidate, max-age=1, s-maxage=600, stale-while-revalidate=15";
    private static final String CUSTOM = "public, must-revalidate, max-age=1, s-maxage=%%jahiaClientCacheCustomTTL%%, stale-while-revalidate=15";

    @Test
    public void aStrongerRequestPolicyReplacesThePresetWhenTheBodyIsOpened() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, ClientCachePolicy.PRIVATE, false);

        wrapper.getOutputStream();

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
        assertEquals(Optional.of(PRIVATE), wrapper.getRequestPolicyValue());
    }

    @Test
    public void aStrongerRequestPolicyAppliesInStrictMode() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, ClientCachePolicy.PRIVATE, true);

        wrapper.getWriter();

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    public void aRequestPolicyNeverWeakensAPrivatePreset() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PRIVATE, ClientCachePolicy.PUBLIC, true);

        wrapper.getOutputStream();

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
        assertEquals(Optional.empty(), wrapper.getRequestPolicyValue());
    }

    @Test
    public void aCustomRequestPolicyCarriesItsTtl() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM,
                new ClientCachePolicy(ClientCachePolicy.Level.CUSTOM, 30), false);

        wrapper.getOutputStream();

        assertEquals(CUSTOM.replace("%%jahiaClientCacheCustomTTL%%", "30"), response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    public void theRequestPolicyAppliesToAnErrorResponse() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, ClientCachePolicy.PRIVATE, true);

        wrapper.sendError(HttpServletResponse.SC_UNAUTHORIZED);

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    public void anErrorIsPrivateWhateverThePreset() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, null, false);

        wrapper.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
        assertEquals(Optional.of(PRIVATE), wrapper.getRequestPolicyValue());
    }

    @Test
    public void aTemporaryRedirectIsPrivateWhateverThePreset() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, null, true);

        wrapper.sendRedirect("/start");

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    public void anErrorIsPrivateEvenAfterTheRequestPolicyWasResolved() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, null, false);

        wrapper.getWriter();
        wrapper.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    public void theCacheHeadersOfAnErrorAreFinal() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, null, false);

        wrapper.sendError(HttpServletResponse.SC_NOT_FOUND);
        wrapper.setHeader("Force-Cache-Control", PUBLIC_MEDIUM);
        wrapper.setHeader(HttpHeaders.CACHE_CONTROL, PUBLIC_MEDIUM);
        wrapper.setDateHeader(HttpHeaders.EXPIRES, 4_070_908_800_000L);

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
        assertFalse(response.headers.containsKey(HttpHeaders.EXPIRES));
    }

    @Test
    public void aLaterWriteCannotChangeAPrivateRequestPolicy() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, ClientCachePolicy.PRIVATE, false);

        wrapper.getOutputStream();
        wrapper.setHeader(HttpHeaders.CACHE_CONTROL, PUBLIC_MEDIUM);
        wrapper.setHeader("Force-Cache-Control", PUBLIC_MEDIUM);

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    public void aLaterWriteCanChangeARequestPolicyThatIsNotPrivate() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM,
                new ClientCachePolicy(ClientCachePolicy.Level.CUSTOM, 30), false);

        wrapper.getOutputStream();
        wrapper.setHeader(HttpHeaders.CACHE_CONTROL, PRIVATE);

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    public void theValueAComponentSetIsKeptWhenThePrivateRequestPolicyReplacesIt() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, ClientCachePolicy.PRIVATE, false);

        wrapper.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");
        wrapper.getOutputStream();

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
        assertEquals(Optional.of("public, max-age=3600"), wrapper.getReplacedCacheControl());
    }

    @Test
    public void theRequestPolicyAppliesToAResponseWithNoBody() {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, ClientCachePolicy.PRIVATE, false);

        wrapper.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        wrapper.applyRequestPolicy();

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    public void aRequestWithNoPolicyKeepsThePreset() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, null, false);

        wrapper.getOutputStream();

        assertEquals(PUBLIC_MEDIUM, response.headers.get(HttpHeaders.CACHE_CONTROL));
        assertEquals(Optional.empty(), wrapper.getRequestPolicyValue());
    }

    @Test
    public void aCommittedResponseIsLeftAsItIs() throws IOException {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, ClientCachePolicy.PRIVATE, false);
        response.committed = true;

        wrapper.getOutputStream();

        assertEquals(PUBLIC_MEDIUM, response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    public void strictModeKeepsTheCacheHeadersWhateverTheCaseOfTheirName() {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PRIVATE, null, true);

        wrapper.setHeader("cache-control", PUBLIC_MEDIUM);
        wrapper.addHeader("CACHE-CONTROL", PUBLIC_MEDIUM);
        wrapper.setHeader("expires", "Thu, 01 Jan 2099 00:00:00 GMT");

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
        assertFalse(response.headers.containsKey(HttpHeaders.EXPIRES));
    }

    @Test
    public void strictModeKeepsTheCacheHeadersWhateverTheTypeOfTheirValue() {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PRIVATE, null, true);

        wrapper.setDateHeader(HttpHeaders.EXPIRES, 4_070_908_800_000L);
        wrapper.addDateHeader("expires", 4_070_908_800_000L);
        wrapper.setIntHeader(HttpHeaders.EXPIRES, 600);
        wrapper.addIntHeader(HttpHeaders.EXPIRES, 600);

        assertFalse(response.headers.containsKey(HttpHeaders.EXPIRES));
    }

    @Test
    public void overridesModeLetsTheCacheHeadersBeReplaced() {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PRIVATE, null, false);

        wrapper.setHeader("cache-control", PUBLIC_MEDIUM);
        wrapper.setDateHeader(HttpHeaders.EXPIRES, 0L);

        assertEquals(PUBLIC_MEDIUM, response.headers.get(HttpHeaders.CACHE_CONTROL));
        assertTrue(response.headers.containsKey(HttpHeaders.EXPIRES));
    }

    @Test
    public void theForcePrefixOverridesInStrictModeWhateverItsCase() {
        FakeResponse response = new FakeResponse();
        ClientCacheResponseWrapper wrapper = preset(response, PUBLIC_MEDIUM, null, true);

        wrapper.setHeader("force-Cache-Control", PRIVATE);

        assertEquals(PRIVATE, response.headers.get(HttpHeaders.CACHE_CONTROL));
    }

    /** A wrapper with its preset written the way the filter writes it, with the policy of its template. */
    private static ClientCacheResponseWrapper preset(FakeResponse response, String preset, ClientCachePolicy requestPolicy, boolean strict) {
        Map<String, Object> attributes = new HashMap<>();
        if (requestPolicy != null) {
            attributes.put(ClientCacheService.CC_REQUEST_POLICY_ATTR, requestPolicy);
        }
        ClientCacheResponseWrapper wrapper = new ClientCacheResponseWrapper(response.proxy(), request(attributes), new FakeService());
        wrapper.setHeader(HttpHeaders.CACHE_CONTROL, preset);
        wrapper.setReadOnlyFilteredHeaders(strict);
        wrapper.setPresetPolicy(PRIVATE.equals(preset) ? ClientCachePolicy.PRIVATE : ClientCachePolicy.PUBLIC);
        return wrapper;
    }

    /** A {@link ServletRequest} that answers only {@code getAttribute}. */
    private static ServletRequest request(Map<String, Object> attributes) {
        return (ServletRequest) Proxy.newProxyInstance(
                ClientCacheResponseWrapperTest.class.getClassLoader(),
                new Class<?>[] { ServletRequest.class },
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName())) {
                        return attributes.get((String) args[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    /** An {@link HttpServletResponse} that keeps its headers by case-insensitive name, as a container does. */
    private static final class FakeResponse {
        private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private boolean committed;

        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    ClientCacheResponseWrapperTest.class.getClassLoader(),
                    new Class<?>[] { HttpServletResponse.class },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setHeader":
                            case "addHeader":
                                headers.put((String) args[0], (String) args[1]);
                                return null;
                            case "setDateHeader":
                            case "addDateHeader":
                            case "setIntHeader":
                            case "addIntHeader":
                                headers.put((String) args[0], String.valueOf(args[1]));
                                return null;
                            case "getHeader":
                                return headers.get((String) args[0]);
                            case "containsHeader":
                                return headers.containsKey((String) args[0]);
                            case "isCommitted":
                                return committed;
                            case "setStatus":
                            case "sendError":
                            case "sendRedirect":
                            case "getOutputStream":
                            case "getWriter":
                                return null;
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }
    }

    /** The default templates of the service configuration, every level included, resolved by name. */
    private static final class FakeService implements ClientCacheService {
        private static final Map<String, String> TEMPLATES = Map.of("private", PRIVATE, "public", PUBLIC, "public-medium", PUBLIC_MEDIUM, "custom", CUSTOM);

        @Override public ClientCacheMode getMode() {
            return ClientCacheMode.ALLOW_OVERRIDES;
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

        @Override public Optional<ClientCachePreset> getPreset(String method, String uri, Map<String, String> templateParams) {
            return Optional.empty();
        }

        @Override public Optional<String> getCacheControlHeader(String method, String uri, Map<String, String> templateParams) {
            return Optional.empty();
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
