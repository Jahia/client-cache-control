package org.jahia.qa.simpletemplatesset.filter;

import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = RenderFilter.class)
public class ClientCachePolicyRenderFilter extends AbstractFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCachePolicyRenderFilter.class);

    @Activate
    public void activate() {
        setDescription("Client Cache Policy RenderContext to Request Attribute");
        setPriority(-1.5f);
        setDisabled(false);
        setApplyOnEditMode(false);
        setSkipOnAjaxRequest(true);
        setApplyOnConfigurations("page");
        setApplyOnTemplateTypes("html,html-*");
        LOGGER.debug("Client Cache Policy Render Filter activated");
    }

    @Override
    public String prepare(RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        return super.prepare(renderContext, resource, chain);
    }

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        LOGGER.info("Setting Response Header 'x-jahia-client-cache-policy' to: {}", renderContext.getClientCachePolicy().getLevel().getValue());
        renderContext.getResponse().setHeader("x-jahia-client-cache-policy", renderContext.getClientCachePolicy().getLevel().getValue());
        return super.execute(previousOut, renderContext, resource, chain);
    }

}
