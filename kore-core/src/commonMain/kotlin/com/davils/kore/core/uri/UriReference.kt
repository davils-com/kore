/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kore.core.uri

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.asciiLowercase

private const val HASH_FACTOR: Int = 31

/**
 * A URI reference as RFC 3986 defines it: a URI, such as `https://example.com/a/b?q#f`, or a relative reference,
 * such as `../c.dtd`, for the Davils libraries that build on Kore.
 *
 * A reference is read with [parseOrNull], which splits it into its five components as Appendix B of RFC 3986 does and
 * then checks each component against the grammar of section 3: the scheme, the user information, the host (a
 * registered name, an IPv4 address, an IPv6 address or an IPvFuture literal in brackets), the port, the path, the
 * query and the fragment. Every percent sign must start a percent-encoded octet. Characters outside that grammar, such
 * as spaces, non-ASCII characters and control characters, are refused rather than encoded, so text that may come from
 * an attacker never becomes a reference it does not spell. Text longer than [Limits.MAX_URI_REFERENCE_LENGTH] is
 * refused before it is read.
 *
 * [resolve] turns a reference into its target URI against a base by section 5.2 of RFC 3986, and [normalize] applies
 * the syntax-based normalization of section 6.2.2. Both run in time linear in the length of the references, however
 * many dot segments a path holds, and both return references that meet the same grammar, so the [toString] of every
 * reference of Kore reads back through [parseOrNull] to an equal reference.
 *
 * [equals] compares the five components as text, which is the simple string comparison of section 6.2.1 of RFC 3986.
 * Two references that differ only in what [normalize] changes are equivalent but not equal; compare their normalized
 * forms to find them equivalent.
 *
 * A reference is immutable and safe to share between threads. Its text consists only of the ASCII characters RFC 3986
 * allows, which are all printable, so it is safe to show without escaping, but it may be as long as
 * [Limits.MAX_URI_REFERENCE_LENGTH]: a message that shows it should still bound its length with
 * [SafeText][com.davils.kore.core.text.SafeText].
 *
 * @property scheme the scheme, such as `https`, without its colon, as it was written; `null` for a relative
 * reference.
 * @property authority the authority, such as `user@example.com:8080`, without its leading `//`; `null` when the
 * reference has none, and empty for the empty authority of `file:///etc`.
 * @property path the path, such as `/a/b`; empty when the reference has none, never `null`.
 * @property query the query, without its `?`; `null` when the reference has none, and empty for a lone `?`.
 * @property fragment the fragment, without its `#`; `null` when the reference has none, and empty for a lone `#`.
 * @since 2.3.0
 */
@DavilsInternal
public class UriReference private constructor(
    public val scheme: String?,
    public val authority: String?,
    public val path: String,
    public val query: String?,
    public val fragment: String?,
) {
    /**
     * Whether this reference has a scheme, which makes it a URI rather than a relative reference.
     *
     * Such a reference may still have a fragment, which the `absolute-URI` of section 4.3 of RFC 3986 excludes. Only a
     * reference with a scheme can serve as the base of [resolve], which ignores its fragment.
     *
     * @since 2.3.0
     */
    public val isAbsolute: Boolean get() = scheme != null

    /**
     * Returns the target URI of [reference] with this reference as its base, by the strict algorithm of section 5.2.2
     * of RFC 3986.
     *
     * The dot segments of the merged path are removed as section 5.2.4 describes, so a reference cannot climb above the
     * root of the base: `../../../g` against `http://a/b/c/d;p?q` gives `http://a/g`. The fragment of this base is
     * ignored. A scheme in [reference] wins even where it equals the scheme of the base, so `http:g` stays
     * `http:g`. Where the target has no authority and its path would start with `//`, the path starts with `/.`
     * instead, so the target does not read back with an authority.
     *
     * The work is linear in the length of both references.
     *
     * @param reference the reference to resolve; it may come from an attacker.
     * @return the target URI, or `null` if this reference has no scheme, which RFC 3986 requires of a base, or if
     * the text of the target would be longer than [Limits.MAX_URI_REFERENCE_LENGTH].
     * @since 2.3.0
     */
    public fun resolve(reference: UriReference): UriReference? {
        val baseScheme = scheme ?: return null
        val target = targetOf(reference, baseScheme)
        return target.takeIf { target.textLength() <= Limits.MAX_URI_REFERENCE_LENGTH }
    }

    /**
     * Returns this reference in the normal form of the syntax-based normalization of section 6.2.2 of RFC 3986.
     *
     * The scheme and the host become lowercase, and so do the letters of a decoded unreserved character in the host.
     * A percent-encoded octet of an unreserved character, a letter, a digit, `-`, `.`, `_` or `~`, is decoded in
     * every component, and every other percent-encoded octet is written with uppercase hexadecimal digits. Dot
     * segments are removed from the path where [resolve] would remove them as well: when the reference has a scheme
     * or an authority or its path starts with `/`. A relative path such as `../a` keeps its dot segments, because
     * they change its meaning. The user information, the port, the query and the fragment keep their case, and no
     * normalization of a particular scheme, such as dropping a default port, is applied.
     *
     * The normal form is never longer than this reference, and normalizing it again changes nothing.
     *
     * @return the normalized reference.
     * @since 2.3.0
     */
    public fun normalize(): UriReference {
        val normalizedPath = normalizedEncoding(path)
        val hasDotSegmentsRemoved = scheme != null || authority != null || normalizedPath.startsWith(SEGMENT_SEPARATOR)
        val resolvedPath = removeDotSegments(normalizedPath).takeIf { hasDotSegmentsRemoved } ?: normalizedPath
        return UriReference(
            scheme = scheme?.let(::asciiLowercase),
            authority = authority?.let(::normalizedAuthority),
            path = guardedPath(resolvedPath, authority),
            query = query?.let(::normalizedEncoding),
            fragment = fragment?.let(::normalizedEncoding),
        )
    }

    /**
     * Whether [other] is a reference with the same five components, compared as text.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal reference.
     * @since 2.3.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is UriReference) return false
        val areStartsEqual = scheme == other.scheme && authority == other.authority && path == other.path
        return areStartsEqual && query == other.query && fragment == other.fragment
    }

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of the five components.
     * @since 2.3.0
     */
    override fun hashCode(): Int {
        val start = (scheme.hashCode() * HASH_FACTOR + authority.hashCode()) * HASH_FACTOR + path.hashCode()
        return (start * HASH_FACTOR + query.hashCode()) * HASH_FACTOR + fragment.hashCode()
    }

    /**
     * Returns the text of this reference, recomposed from its components by section 5.3 of RFC 3986, which
     * [parseOrNull] reads back to an equal reference.
     *
     * @return the text of this reference, at most [Limits.MAX_URI_REFERENCE_LENGTH] characters long.
     * @since 2.3.0
     */
    override fun toString(): String = buildString(textLength()) {
        scheme?.let { name -> append(name).append(SCHEME_SEPARATOR) }
        authority?.let { part -> append(AUTHORITY_PREFIX).append(part) }
        append(path)
        query?.let { part -> append(QUERY_SEPARATOR).append(part) }
        fragment?.let { part -> append(FRAGMENT_SEPARATOR).append(part) }
    }

    private fun textLength(): Int {
        val authorityLength = authority?.let { part -> part.length + AUTHORITY_PREFIX.length } ?: 0
        val start = scheme.withSeparatorLength() + authorityLength
        return start + path.length + query.withSeparatorLength() + fragment.withSeparatorLength()
    }

    private fun targetOf(reference: UriReference, baseScheme: String): UriReference {
        val hasOwnAuthority = reference.scheme != null || reference.authority != null
        if (hasOwnAuthority) {
            return reference.withPath(scheme = reference.scheme ?: baseScheme, path = removeDotSegments(reference.path))
        }
        return targetWithBaseAuthority(reference, baseScheme)
    }

    private fun targetWithBaseAuthority(reference: UriReference, baseScheme: String): UriReference {
        if (reference.path.isEmpty()) {
            return UriReference(
                scheme = baseScheme,
                authority = authority,
                path = path,
                query = reference.query ?: query,
                fragment = reference.fragment,
            )
        }
        val isPathAbsolute = reference.path.startsWith(SEGMENT_SEPARATOR)
        val merged = reference.path.takeIf { isPathAbsolute } ?: mergedPath(reference.path)
        val target = removeDotSegments(merged)
        return UriReference(
            scheme = baseScheme,
            authority = authority,
            path = guardedPath(target, authority),
            query = reference.query,
            fragment = reference.fragment,
        )
    }

    private fun withPath(scheme: String, path: String): UriReference = UriReference(
        scheme = scheme,
        authority = authority,
        path = guardedPath(path, authority),
        query = query,
        fragment = fragment,
    )

    private fun mergedPath(relativePath: String): String {
        if (authority != null && path.isEmpty()) return SEGMENT_SEPARATOR + relativePath
        val directoryEnd = path.lastIndexOf(SEGMENT_SEPARATOR) + 1
        return path.substring(startIndex = 0, endIndex = directoryEnd) + relativePath
    }

    /**
     * Reads URI references from text.
     *
     * @since 2.3.0
     */
    public companion object {
        /**
         * Reads [text] as a URI reference of RFC 3986, a URI or a relative reference, and returns `null` for
         * anything else.
         *
         * The text is split as Appendix B of RFC 3986 does and each component is checked against the grammar of
         * section 3, so the reference keeps every component exactly as it was written; [UriReference.normalize]
         * gives its normal form. The empty text is the empty relative reference, which refers to the base itself.
         * Text with a character RFC 3986 does not allow, such as a space, a non-ASCII character or a control
         * character, with a percent sign that does not start a percent-encoded octet, with a scheme that does not
         * start with a letter, with a port that is not a number, with a malformed IP literal or with a colon in the
         * first segment of a relative path is refused, and so is text longer than [Limits.MAX_URI_REFERENCE_LENGTH].
         *
         * This function never throws for any text, and its work is linear in the length of [text].
         *
         * @param text the text to read; it may come from an attacker.
         * @return the reference, or `null` if [text] is not a URI reference or is too long.
         * @since 2.3.0
         */
        public fun parseOrNull(text: String): UriReference? {
            if (text.length > Limits.MAX_URI_REFERENCE_LENGTH) return null
            val parts = splitReference(text)
            return parts.takeIf(::isWellFormed)?.let(::referenceOf)
        }

        private fun referenceOf(parts: ReferenceParts): UriReference = UriReference(
            scheme = parts.scheme,
            authority = parts.authority,
            path = parts.path,
            query = parts.query,
            fragment = parts.fragment,
        )
    }
}

private fun String?.withSeparatorLength(): Int = this?.let { part -> part.length + 1 } ?: 0
