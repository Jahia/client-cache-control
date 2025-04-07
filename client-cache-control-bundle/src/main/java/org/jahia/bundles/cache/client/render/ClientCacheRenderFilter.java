/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.bundles.cache.client.render;

import org.apache.http.HttpHeaders;
import org.jahia.bundles.cache.client.api.ClientCacheService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Apply the client cache policy for resources served by the render chain.
 *
 * @author Jerome Blanchard
 */
@Component(service = RenderFilter.class, immediate = true)
public class ClientCacheRenderFilter extends AbstractFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCacheRenderFilter.class);

    private ClientCacheService service;

    @Reference(service = ClientCacheService.class)
    public void setService(ClientCacheService service) {
        this.service = service;
    }

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
        LOGGER.debug("Client Cache Policy Level set to {} with a TTL of {}", renderContext.getClientCachePolicy().getLevel().getValue(), renderContext.getClientCachePolicy().getTtl());
        String cacheControl = service.getCacheControlHeader(renderContext.getClientCachePolicy().getLevel().getValue(),
                Map.of(ClientCacheService.CC_CUSTOM_TTL_ATTR, Integer.toString(renderContext.getClientCachePolicy().getTtl())));
        LOGGER.debug("Setting Response Cache-Control to: {}", cacheControl);
        // Use the Force-Cache-Control header to bypass strict mode because render chain cache control modification must be enforced whatever mode is used.
        renderContext.getResponse().setHeader("Force-".concat(HttpHeaders.CACHE_CONTROL), cacheControl);
        return super.execute(previousOut, renderContext, resource, chain);
    }

}
