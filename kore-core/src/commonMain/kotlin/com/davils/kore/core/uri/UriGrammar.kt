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

import com.davils.kore.core.text.isAsciiDigit
import com.davils.kore.core.text.isAsciiLetter
import com.davils.kore.core.text.isAsciiLetterOrDigit

internal const val SCHEME_SEPARATOR: Char = ':'
internal const val AUTHORITY_PREFIX: String = "//"
internal const val SEGMENT_SEPARATOR: Char = '/'
internal const val QUERY_SEPARATOR: Char = '?'
internal const val FRAGMENT_SEPARATOR: Char = '#'
internal const val USER_INFO_SEPARATOR: Char = '@'
internal const val PORT_SEPARATOR: Char = ':'
internal const val IP_LITERAL_START: Char = '['
internal const val IP_LITERAL_END: Char = ']'
internal const val PERCENT: Char = '%'
internal const val TRIPLET_LENGTH: Int = 3

private const val SCHEME_ENDS: String = ":/?#"
private const val SUB_DELIMITERS: String = "!$&'()*+,;="
private const val UNRESERVED_MARKS: String = "-._~"
private const val SCHEME_MARKS: String = "+-."
private const val SEGMENT_MARKS: String = ":@"
private const val HEX_LETTERS: String = "abcdefABCDEF"

internal fun splitReference(text: String): ReferenceParts {
    val schemeEnd = text.indexOfFirst { character -> character in SCHEME_ENDS }
    val hasScheme = schemeEnd > 0 && text[schemeEnd] == SCHEME_SEPARATOR
    val scheme = text.takeIf { hasScheme }?.substring(startIndex = 0, endIndex = schemeEnd)
    val hierarchyStart = scheme?.let { name -> name.length + 1 } ?: 0
    val fragmentStart = text.indexOf(FRAGMENT_SEPARATOR, hierarchyStart).takeIf { at -> at >= 0 } ?: text.length
    val queryStart = text.indexOf(QUERY_SEPARATOR, hierarchyStart).takeIf { at -> at in 0..<fragmentStart }
        ?: fragmentStart
    val hasAuthority = text.startsWith(AUTHORITY_PREFIX, hierarchyStart)
    val authorityStart = hierarchyStart + AUTHORITY_PREFIX.length
    val authorityEnd = text.indexOf(SEGMENT_SEPARATOR, authorityStart).takeIf { at -> at in 0..<queryStart }
        ?: queryStart
    val pathStart = authorityEnd.takeIf { hasAuthority } ?: hierarchyStart
    return ReferenceParts(
        scheme = scheme,
        authority = text.takeIf { hasAuthority }?.substring(startIndex = authorityStart, endIndex = authorityEnd),
        path = text.substring(startIndex = pathStart, endIndex = queryStart),
        query = queryOf(text, queryStart, fragmentStart),
        fragment = text.takeIf { fragmentStart < text.length }?.substring(startIndex = fragmentStart + 1),
    )
}

private fun queryOf(text: String, queryStart: Int, fragmentStart: Int): String? {
    if (queryStart == fragmentStart) return null
    return text.substring(startIndex = queryStart + 1, endIndex = fragmentStart)
}

internal fun isWellFormed(parts: ReferenceParts): Boolean {
    val isSchemeWellFormed = parts.scheme?.let(::isScheme) != false
    val isAuthorityWellFormed = parts.authority?.let(::isAuthority) != false
    val isQueryWellFormed = parts.query?.let(::isQueryOrFragment) != false
    val isFragmentWellFormed = parts.fragment?.let(::isQueryOrFragment) != false
    val areOuterPartsWellFormed = isSchemeWellFormed && isQueryWellFormed && isFragmentWellFormed
    return areOuterPartsWellFormed && isAuthorityWellFormed && isPathOf(parts)
}

internal fun isPercentTriplet(text: String, at: Int): Boolean {
    val isTripletInside = at + TRIPLET_LENGTH <= text.length && text[at] == PERCENT
    return isTripletInside && isHexDigit(text[at + 1]) && isHexDigit(text[at + 2])
}

internal fun isUnreserved(character: Char): Boolean = isAsciiLetterOrDigit(character) || character in UNRESERVED_MARKS

private fun isPathOf(parts: ReferenceParts): Boolean {
    val path = parts.path
    val isRelativePath = parts.scheme == null && parts.authority == null
    val firstSegment = path.substringBefore(SEGMENT_SEPARATOR)
    val hasSchemeLikeStart = isRelativePath && SCHEME_SEPARATOR in firstSegment
    return !hasSchemeLikeStart && isEncoded(path, ::isPathCharacter)
}

private fun isScheme(scheme: String): Boolean {
    val isFirstLetter = scheme.isNotEmpty() && isAsciiLetter(scheme.first())
    return isFirstLetter && scheme.all { character -> isAsciiLetterOrDigit(character) || character in SCHEME_MARKS }
}

private fun isAuthority(authority: String): Boolean {
    val userInfoEnd = authority.indexOf(USER_INFO_SEPARATOR)
    val userInfo = authority.substring(startIndex = 0, endIndex = userInfoEnd.coerceAtLeast(0))
    val isUserInfoWellFormed = isEncoded(userInfo, ::isUserInfoCharacter)
    return isUserInfoWellFormed && isHostAndPort(authority.substring(startIndex = userInfoEnd + 1))
}

private fun isHostAndPort(text: String): Boolean {
    if (text.startsWith(IP_LITERAL_START)) return isIpLiteralAndPort(text)
    val hostEnd = text.indexOf(PORT_SEPARATOR).takeIf { at -> at >= 0 } ?: text.length
    val host = text.substring(startIndex = 0, endIndex = hostEnd)
    return isEncoded(host, ::isRegisteredNameCharacter) && hasOnlyDigitsAfter(text, hostEnd)
}

private fun isIpLiteralAndPort(text: String): Boolean {
    val literalEnd = text.indexOf(IP_LITERAL_END)
    if (literalEnd < 0) return false
    val portStart = literalEnd + 1
    val isPortWellFormed = portStart == text.length || text[portStart] == PORT_SEPARATOR
    val literal = text.substring(startIndex = 1, endIndex = literalEnd)
    return isPortWellFormed && hasOnlyDigitsAfter(text, portStart) && isIpLiteral(literal)
}

private fun hasOnlyDigitsAfter(text: String, separator: Int): Boolean =
    (separator + 1..<text.length).all { at -> isAsciiDigit(text[at]) }

private fun isEncoded(text: String, isAllowed: (Char) -> Boolean): Boolean {
    var at = 0
    while (at < text.length) {
        val length = encodedLengthAt(text, at, isAllowed)
        if (length == 0) return false
        at += length
    }
    return true
}

private fun encodedLengthAt(text: String, at: Int, isAllowed: (Char) -> Boolean): Int {
    if (text[at] == PERCENT) return TRIPLET_LENGTH.takeIf { isPercentTriplet(text, at) } ?: 0
    return 1.takeIf { isAllowed(text[at]) } ?: 0
}

internal fun isHexDigit(character: Char): Boolean = isAsciiDigit(character) || character in HEX_LETTERS

internal fun isSubDelimiter(character: Char): Boolean = character in SUB_DELIMITERS

private fun isSegmentCharacter(character: Char): Boolean =
    isUnreserved(character) || isSubDelimiter(character) || character in SEGMENT_MARKS

private fun isPathCharacter(character: Char): Boolean = isSegmentCharacter(character) || character == SEGMENT_SEPARATOR

private fun isQueryOrFragment(text: String): Boolean = isEncoded(text, ::isQueryCharacter)

private fun isQueryCharacter(character: Char): Boolean = isPathCharacter(character) || character == QUERY_SEPARATOR

private fun isUserInfoCharacter(character: Char): Boolean =
    isRegisteredNameCharacter(character) || character == PORT_SEPARATOR

private fun isRegisteredNameCharacter(character: Char): Boolean = isUnreserved(character) || isSubDelimiter(character)
