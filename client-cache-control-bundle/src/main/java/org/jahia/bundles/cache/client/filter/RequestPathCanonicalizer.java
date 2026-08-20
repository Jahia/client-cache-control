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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;

/**
 * Rewrites a request path to the single form that stands for every way of writing it.
 *
 * <p>A servlet container does this itself and exposes the result, so this is for the one case where it
 * exposes nothing: the operations are the container's, in the container's order — percent-decode, then
 * collapse duplicate separators, then remove dot segments. Decoding first is what makes an encoded dot
 * segment resolve like a written one.</p>
 *
 * <p>Percent-decoding here is the one a path calls for, and only that. A {@code +} stays a {@code +},
 * because a path is not a form. A {@code %} that is not followed by two hexadecimal digits stays as it
 * is, so a name carrying a literal percent sign survives. Decoding runs over the collected bytes, so a
 * multi-byte UTF-8 character written as several groups yields the character it stands for.</p>
 *
 * @author Jerome Blanchard
 */
public final class RequestPathCanonicalizer {

    private RequestPathCanonicalizer() {
        // Utility class
    }

    private static final Pattern DUPLICATE_SEPARATOR = Pattern.compile("/{2,}");

    /**
     * Rewrites a request URI to the single form that stands for every spelling of the same path:
     * percent-encoding decoded once, duplicate separators collapsed, dot segments removed.
     *
     * <p>Returns the argument unchanged when it is already canonical, which is the common case.</p>
     */
    public static String canonicalize(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        String canonical = removeDotSegments(DUPLICATE_SEPARATOR.matcher(decodeOnce(uri)).replaceAll("/"));
        return canonical.equals(uri) ? uri : canonical;
    }

    /**
     * Percent-decodes once. A {@code %} that is not followed by two hexadecimal digits is left as it
     * stands, so a path that carries a literal percent sign is not corrupted. Decoding runs over the
     * collected bytes rather than character by character, so a multi-byte UTF-8 sequence written as
     * several percent groups decodes to the character it stands for.
     */
    private static String decodeOnce(String uri) {
        if (uri.indexOf('%') < 0) {
            return uri;
        }
        StringBuilder decoded = new StringBuilder(uri.length());
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        int i = 0;
        while (i < uri.length()) {
            if (uri.charAt(i) == '%' && i + 2 < uri.length() && isHex(uri.charAt(i + 1)) && isHex(uri.charAt(i + 2))) {
                pending.write(Integer.parseInt(uri.substring(i + 1, i + 3), 16));
                i += 3;
            } else {
                flush(pending, decoded);
                decoded.append(uri.charAt(i));
                i++;
            }
        }
        flush(pending, decoded);
        return decoded.toString();
    }

    private static void flush(ByteArrayOutputStream pending, StringBuilder out) {
        if (pending.size() > 0) {
            out.append(new String(pending.toByteArray(), StandardCharsets.UTF_8));
            pending.reset();
        }
    }

    private static boolean isHex(char c) {
        return Character.digit(c, 16) >= 0;
    }

    /**
     * Removes {@code .} and {@code ..} segments, following RFC 3986 section 5.2.4. A {@code ..} that
     * would climb above the root is dropped rather than applied.
     */
    private static String removeDotSegments(String path) {
        if (path.indexOf('.') < 0) {
            return path;
        }
        boolean rooted = path.startsWith("/");
        Deque<String> kept = new ArrayDeque<>();
        for (String segment : (rooted ? path.substring(1) : path).split("/", -1)) {
            if ("..".equals(segment)) {
                // A rooted path has no parent above its root, so a climb that would leave it is dropped
                // rather than applied. Leaving it in would make the canonical form name a path the
                // request could not reach.
                kept.pollLast();
            } else if (!".".equals(segment)) {
                kept.addLast(segment);
            }
        }
        String rebuilt = (rooted ? "/" : "") + String.join("/", kept);
        // A path whose last segment was a dot segment loses its trailing separator when it is rebuilt.
        if (path.endsWith("/") && !rebuilt.endsWith("/")) {
            rebuilt = rebuilt + "/";
        }
        return rebuilt.isEmpty() ? "/" : rebuilt;
    }
}
