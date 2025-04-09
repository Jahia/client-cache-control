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
package org.jahia.bundles.cache.client.karaf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jahia.bundles.cache.client.api.ClientCacheRule;
import org.jahia.bundles.cache.client.api.ClientCacheService;

import java.util.List;

/**
 * Karaf command to list current configured filtering rules.
 *
 * @author Jerome Blanchard
 * @date 09/04/2025
 */
@Service
@Command(
        scope = "client-cache-control",
        name = "list-rules",
        description = "List configured client cache control rules")
public class ClientCacheListRulesAction implements Action {

    @Reference
    private ClientCacheService service;

    @Argument(description = "format")
    private String format;

    @Override
    public Object execute() throws Exception {
        if (format != null && format.equals("json")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(System.out, service.listRules());
            return null;
        }
        ShellTable table = new ShellTable();
        table.column("Priority");
        table.column("Methods");
        table.column("URL Regexp");
        table.column("Header");
        List<ClientCacheRule> rules = service.listRules();
        for (ClientCacheRule rule : rules) {
            table.addRow().addContent(rule.getPriority(), rule.getMethods(), rule.getUrlRegexp(), rule.getHeader());
        }
        table.print(System.out);
        return null;
    }

}
