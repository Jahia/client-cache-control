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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * The one form a request path is rewritten to.
 *
 * @author Jerome Blanchard
 */
public class RequestPathCanonicalizerTest {

    @Test
    public void canonicalizeRewritesEverySpellingToOneForm() {
        assertEquals("/modules/healthcheck", RequestPathCanonicalizer.canonicalize("/modules/healthchec%6b"));
        assertEquals("/modules/healthcheck", RequestPathCanonicalizer.canonicalize("/modules//healthcheck"));
        assertEquals("/modules/healthcheck", RequestPathCanonicalizer.canonicalize("/modules/./healthcheck"));
        assertEquals("/modules/healthcheck", RequestPathCanonicalizer.canonicalize("/modules/x/../healthcheck"));
        assertEquals("/modules/tools/index.jsp", RequestPathCanonicalizer.canonicalize("/modules/tool%73//index.jsp"));
        // Decoding runs before dot segments are removed, which is the order the container applies, so an
        // encoded dot segment resolves the same way a written one does.
        assertEquals("/healthcheck", RequestPathCanonicalizer.canonicalize("/modules/%2e%2e/healthcheck"));
        // A multi-byte character written as several percent groups decodes to the character it stands for.
        assertEquals("/files/été.pdf", RequestPathCanonicalizer.canonicalize("/files/%C3%A9t%C3%A9.pdf"));
    }

    @Test
    public void canonicalizeLeavesAPathThatIsAlreadyCanonicalUntouched() {
        String uri = "/modules/ckeditor/javascript/ckeditor.js";
        assertSame(uri, RequestPathCanonicalizer.canonicalize(uri));
        // A percent sign that is not followed by two hexadecimal digits belongs to the name and is kept as
        // it stands. An encoded one is decoded, like any other encoded byte.
        assertEquals("/files/100% done.pdf", RequestPathCanonicalizer.canonicalize("/files/100% done.pdf"));
        assertEquals("/files/50% off.pdf", RequestPathCanonicalizer.canonicalize("/files/50%25 off.pdf"));
        // A plus sign is a plus sign in a path, whatever form encoding makes of it.
        assertSame("/files/a+b.pdf", RequestPathCanonicalizer.canonicalize("/files/a+b.pdf"));
    }

    @Test
    public void canonicalizeDoesNotClimbAboveTheRoot() {
        assertEquals("/etc/passwd", RequestPathCanonicalizer.canonicalize("/../../etc/passwd"));
        assertEquals("/", RequestPathCanonicalizer.canonicalize("/"));
    }

    @Test
    public void aPlusSignIsAPlusSignInAPath() {
        // Form decoding reads a plus as a space. A path does not, so a name carrying one survives.
        assertSame("/files/a+b.pdf", RequestPathCanonicalizer.canonicalize("/files/a+b.pdf"));
        assertEquals("/files/C++ notes.pdf", RequestPathCanonicalizer.canonicalize("/files/C%2B%2B notes.pdf"));
    }

    @Test
    public void anEmptyOrNullPathIsReturnedAsItIs() {
        assertSame("", RequestPathCanonicalizer.canonicalize(""));
        assertEquals(null, RequestPathCanonicalizer.canonicalize(null));
    }
}
