package org.jahia.bundles.cache.client.api;/*
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
 */

import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Jerome Blanchard
 */
public interface ClientCacheRule {

    /*
     * The priority of the rule. Lower values have higher priority.
     * Default rule priorities are spaced about a hundred apart to allow for other rules to be interspersed.
     */
    int getPriority();

    /**
     * A set of methods that the rule can be applied.
     *
     * @return the set of methods names
     */
    Set<String> getMethods();

    /**
     * A URL pattern that the rule can be applied to.
     *
     * @return the pattern
     */
    Pattern getUrlPattern();

    /**
     * A cache control header template name
     * (cannot be used in conjunction with a headerValue)
     *
     * @return the template name
     */
    String getHeaderTemplate();

    /**
     * A cache control header template predefined value
     * (cannot be used in conjunction with a headerTemplate)
     *
     * @return the header value
     */
    String getHeaderValue();

}
