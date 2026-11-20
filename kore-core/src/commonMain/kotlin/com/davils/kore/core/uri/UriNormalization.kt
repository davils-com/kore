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

import com.davils.kore.core.text.asciiLowercase
import com.davils.kore.core.text.asciiUppercase

private const val HEX_RADIX: Int = 16
private const val NO_AUTHORITY_GUARD: String = "/."

internal fun normalizedEncoding(text: String, isCaseInsensitive: Boolean = false): String {
    val normalized = StringBuilder(text.length)
    var at = 0
    while (at < text.length) {
        at += appendNormalized(text = text, at = at, normalized = normalized, isCaseInsensitive = isCaseInsensitive)
    }
    return normalized.toString()
}

internal fun normalizedAuthority(authority: String): String {
    val hostStart = authority.indexOf(USER_INFO_SEPARATOR) + 1
    val hostEnd = hostEndOf(authority, hostStart)
    val userInfo = normalizedEncoding(authority.substring(startIndex = 0, endIndex = hostStart))
    val writtenHost = authority.substring(startIndex = hostStart, endIndex = hostEnd)
    val host = normalizedEncoding(writtenHost, isCaseInsensitive = true)
    return userInfo + host + authority.substring(startIndex = hostEnd)
}

internal fun guardedPath(path: String, authority: String?): String {
    val isGuardNeeded = authority == null && path.startsWith(AUTHORITY_PREFIX)
    return NO_AUTHORITY_GUARD.takeIf { isGuardNeeded }.orEmpty() + path
}

private fun hostEndOf(authority: String, hostStart: Int): Int {
    val isIpLiteral = hostStart < authority.length && authority[hostStart] == IP_LITERAL_START
    val separator = IP_LITERAL_END.takeIf { isIpLiteral } ?: PORT_SEPARATOR
    val literalEnd = 1.takeIf { isIpLiteral } ?: 0
    val found = authority.indexOf(separator, hostStart)
    return found.takeIf { at -> at >= 0 }?.plus(literalEnd) ?: authority.length
}

private fun appendNormalized(text: String, at: Int, normalized: StringBuilder, isCaseInsensitive: Boolean): Int {
    if (!isPercentTriplet(text, at)) {
        normalized.append(caseOf(text[at], isCaseInsensitive))
        return 1
    }
    appendTriplet(text.substring(startIndex = at + 1, endIndex = at + TRIPLET_LENGTH), normalized, isCaseInsensitive)
    return TRIPLET_LENGTH
}

private fun appendTriplet(hex: String, normalized: StringBuilder, isCaseInsensitive: Boolean) {
    val decoded = hex.toInt(HEX_RADIX).toChar()
    if (isUnreserved(decoded)) {
        normalized.append(caseOf(decoded, isCaseInsensitive))
        return
    }
    normalized.append(PERCENT).append(asciiUppercase(hex))
}

private fun caseOf(character: Char, isCaseInsensitive: Boolean): Char =
    asciiLowercase(character).takeIf { isCaseInsensitive } ?: character
