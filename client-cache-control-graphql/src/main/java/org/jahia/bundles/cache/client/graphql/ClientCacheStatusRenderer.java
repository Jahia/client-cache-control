package org.jahia.bundles.cache.client.graphql;

import org.jahia.api.Constants;
import org.jahia.bin.Render;
import org.jahia.bundles.cache.client.api.ClientCacheService;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.RenderException;
import org.jahia.services.render.RenderService;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.render.URLResolverFactory;
import org.jahia.services.render.filter.cache.ClientCachePolicy;
import org.jahia.services.render.filter.cache.ClientCacheTrace;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders the live version of a page as guest with a {@link ClientCacheTrace}, and gives the client cache status of the page.
 */
class ClientCacheStatusRenderer {

    private static final String URL_RESOLVER_ATTRIBUTE = "urlResolver";

    private final ClientCacheService service;

    ClientCacheStatusRenderer(ClientCacheService service) {
        this.service = service;
    }

    GqlClientCacheStatus render(String uuid, String language, HttpServletRequest request, HttpServletResponse response) throws RepositoryException, RenderException {
        JCRSessionFactory sessionFactory = JCRSessionFactory.getInstance();
        JahiaUser guest = JahiaUserManagerService.getInstance().lookupUser(Constants.GUEST_USERNAME).getJahiaUser();
        Locale locale = LanguageCodeConverters.languageCodeToLocale(language);
        JahiaUser currentUser = sessionFactory.getCurrentUser();
        Locale currentLocale = sessionFactory.getCurrentLocale();
        String currentServletPath = sessionFactory.getCurrentServletPath();
        sessionFactory.setCurrentUser(guest);
        sessionFactory.setCurrentLocale(locale);
        sessionFactory.setCurrentServletPath(Render.getRenderServletPath());
        try {
            JCRSessionWrapper session = sessionFactory.getCurrentUserSession(Constants.LIVE_WORKSPACE, locale);
            JCRNodeWrapper page;
            try {
                page = session.getNodeByIdentifier(uuid);
            } catch (ItemNotFoundException e) {
                return GqlClientCacheStatus.notPublished();
            }
            // the GraphQL servlet wraps the request, and a view include needs the request of the container
            HttpServletRequest containerRequest = request instanceof HttpServletRequestWrapper
                    ? (HttpServletRequest) ((HttpServletRequestWrapper) request).getRequest() : request;
            return render(page, language, guest, new VisitorRequest(containerRequest), new DiscardedResponse(response));
        } finally {
            sessionFactory.setCurrentUser(currentUser);
            sessionFactory.setCurrentLocale(currentLocale);
            sessionFactory.setCurrentServletPath(currentServletPath);
        }
    }

    private GqlClientCacheStatus render(JCRNodeWrapper page, String language, JahiaUser guest, VisitorRequest request, DiscardedResponse response) throws RepositoryException, RenderException {
        ClientCacheTrace trace = new ClientCacheTrace();
        // a visitor on the server name of the site gets this resolver, which the site part of the cache key reads
        URLResolverFactory urlResolverFactory = (URLResolverFactory) SpringContextSingleton.getBean("urlResolverFactory");
        URLResolver urlResolver = urlResolverFactory.createURLResolver("/" + Constants.LIVE_WORKSPACE + "/" + language + page.getPath() + ".html",
                page.getResolveSite().getServerName(), Constants.LIVE_WORKSPACE, request);
        RenderContext renderContext = new RenderContext(request, response, guest);
        renderContext.setSiteInfo(urlResolver.getSiteInfo());
        Resource resource = new Resource(page, "html", "default", Resource.CONFIGURATION_PAGE);
        renderContext.setMainResource(resource);
        renderContext.setServletPath(Render.getRenderServletPath());
        renderContext.setWorkspace(Constants.LIVE_WORKSPACE);
        renderContext.setSite(page.getResolveSite());

        Object graphqlUrlResolver = request.getAttribute(URL_RESOLVER_ATTRIBUTE);
        request.setAttribute(URL_RESOLVER_ATTRIBUTE, urlResolver);
        request.setAttribute(ClientCacheTrace.REQUEST_ATTRIBUTE, trace);
        try {
            RenderService.getInstance().render(resource, renderContext);
        } finally {
            request.removeAttribute(ClientCacheTrace.REQUEST_ATTRIBUTE);
            request.setAttribute(URL_RESOLVER_ATTRIBUTE, graphqlUrlResolver);
        }

        ClientCachePolicy policy = renderContext.getClientCachePolicy();
        String header = service.getCacheControlHeader(policy.getLevel().getValue(),
                Map.of(ClientCacheService.CC_CUSTOM_TTL_ATTR, Integer.toString(policy.getTtl()))).orElse(null);
        List<GqlClientCacheFragment> fragments = trace.getDecisions().stream()
                .sorted((a, b) -> a.getPolicy().isStronger(b.getPolicy()) ? -1 : (b.getPolicy().isStronger(a.getPolicy()) ? 1 : 0))
                .map(GqlClientCacheFragment::new)
                .collect(Collectors.toList());
        return new GqlClientCacheStatus(policy, header, fragments);
    }

    /**
     * The request of a visitor: a GET without parameters.
     */
    private static class VisitorRequest extends HttpServletRequestWrapper {

        VisitorRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public String getParameter(String name) {
            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.emptyMap();
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String[] getParameterValues(String name) {
            return null;
        }

        @Override
        public String getQueryString() {
            return null;
        }
    }

    /**
     * A response that keeps no status, no header and no body, so that the render does not write on the GraphQL response.
     */
    private static class DiscardedResponse extends HttpServletResponseWrapper {

        private final PrintWriter writer = new PrintWriter(Writer.nullWriter());

        private final ServletOutputStream outputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // nothing is written
            }

            @Override
            public void write(int b) {
                // nothing is written
            }
        };

        DiscardedResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public boolean isCommitted() {
            return false;
        }

        @Override
        public boolean containsHeader(String name) {
            return false;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return Collections.emptyList();
        }

        @Override
        public Collection<String> getHeaderNames() {
            return Collections.emptyList();
        }

        @Override
        public void setHeader(String name, String value) {
            // no header reaches the GraphQL response
        }

        @Override
        public void addHeader(String name, String value) {
            // no header reaches the GraphQL response
        }

        @Override
        public void setDateHeader(String name, long date) {
            // no header reaches the GraphQL response
        }

        @Override
        public void addDateHeader(String name, long date) {
            // no header reaches the GraphQL response
        }

        @Override
        public void setIntHeader(String name, int value) {
            // no header reaches the GraphQL response
        }

        @Override
        public void addIntHeader(String name, int value) {
            // no header reaches the GraphQL response
        }

        @Override
        public void addCookie(Cookie cookie) {
            // no cookie reaches the GraphQL response
        }

        @Override
        public void setStatus(int sc) {
            // the status of the GraphQL response stays
        }

        @Override
        public void sendError(int sc) {
            // the status of the GraphQL response stays
        }

        @Override
        public void sendError(int sc, String msg) {
            // the status of the GraphQL response stays
        }

        @Override
        public void sendRedirect(String location) {
            // the status of the GraphQL response stays
        }

        @Override
        public void setContentType(String type) {
            // the content type of the GraphQL response stays
        }

        @Override
        public void setCharacterEncoding(String charset) {
            // the encoding of the GraphQL response stays
        }

        @Override
        public void setContentLength(int len) {
            // nothing is written
        }

        @Override
        public void setContentLengthLong(long len) {
            // nothing is written
        }

        @Override
        public void setLocale(Locale loc) {
            // the locale of the GraphQL response stays
        }

        @Override
        public void flushBuffer() {
            // nothing is written
        }

        @Override
        public void reset() {
            // nothing is written
        }

        @Override
        public void resetBuffer() {
            // nothing is written
        }
    }
}
