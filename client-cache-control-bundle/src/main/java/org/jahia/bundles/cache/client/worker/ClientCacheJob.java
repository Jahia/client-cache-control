/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */package org.jahia.bundles.cache.client.worker;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author Jerome Blanchard
 */
public class ClientCacheJob implements Delayed {

    private String id;
    private String type;
    private String action;
    private String target;
    private long timestamp;
    private boolean failed;
    private Map<String, String> parameters;

    public ClientCacheJob(String type, String action, String target, long timestamp, Map<String, String> args) {
        super();
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.action = action;
        this.target = target;
        this.timestamp = timestamp;
        this.parameters = args;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "ClientCacheJob{" + "id=" + id + ", type='" + type + '\'' + ", action='" + action + '\'' + ", target='" + target + '\'' + ", timestamp=" + timestamp + ", failed=" + failed + '}';
    }

    @Override
    public long getDelay(@Nonnull TimeUnit unit) {
        long diff = timestamp - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@Nonnull Delayed obj) {
        if (!(obj instanceof ClientCacheJob)) {
            throw new IllegalArgumentException("Illegal comparison to non-ClientCacheInvalidationJob");
        }
        ClientCacheJob other = (ClientCacheJob) obj;
        return (int) (this.getTimestamp() - other.getTimestamp());
    }
}
