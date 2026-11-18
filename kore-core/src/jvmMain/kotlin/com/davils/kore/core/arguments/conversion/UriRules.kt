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

package com.davils.kore.core.arguments.conversion

import com.davils.kore.core.text.internal.SafeDisplay
import java.net.URI
import java.net.URISyntaxException

private const val NOT_ABSOLUTE_REASON: String = "is not an absolute URI"
private const val USER_INFO_REASON: String = "carries user information, which this parameter does not accept"
private const val NO_AUTHORITY_REASON: String = "does not name its host after //, which its scheme requires"
private const val SCHEME_SEPARATOR: String = ", "

private val schemeGrammar: Regex = Regex("[A-Za-z][A-Za-z0-9+.-]*")
private val hostSchemes: Set<String> = setOf("ftp", "http", "https", "ws", "wss")

internal class UriRules(
    schemes: Set<String>,
    private val isAbsoluteRequired: Boolean,
    private val isUserInfoAllowed: Boolean,
) {
    private val accepted: Set<String> = buildSet { schemes.forEach { scheme -> add(asciiLowercase(scheme)) } }
    private val schemeReason: String = schemeReasonFor(accepted)

    init {
        schemes.forEach { scheme ->
            require(schemeGrammar.matches(scheme)) {
                "A URI scheme starts with an ASCII letter followed by letters, digits, '+', '-' or '.', " +
                    "but was ${SafeDisplay.quoted(scheme)}"
            }
        }
    }

    fun read(raw: String): URI {
        val uri = parsed(raw)
        problemWith(uri)?.let { problem -> throw ConversionException(problem) }
        return uri
    }

    private fun problemWith(uri: URI): String? {
        if (isAbsoluteRequired && !uri.isAbsolute) return NOT_ABSOLUTE_REASON
        val schemeProblem = schemeReason.takeUnless { isSchemeAccepted(uri.scheme) }
        return schemeProblem ?: authorityProblem(uri) ?: userInfoProblem(uri)
    }

    private fun userInfoProblem(uri: URI): String? = USER_INFO_REASON.takeIf { !isUserInfoAllowed && hasUserInfo(uri) }

    private fun isSchemeAccepted(scheme: String?): Boolean {
        if (scheme == null || accepted.isEmpty()) return true
        return asciiLowercase(scheme) in accepted
    }
}

private fun authorityProblem(uri: URI): String? =
    NO_AUTHORITY_REASON.takeIf { uri.rawAuthority == null && needsHost(uri.scheme) }

private fun needsHost(scheme: String?): Boolean = scheme != null && asciiLowercase(scheme) in hostSchemes

private fun schemeReasonFor(accepted: Set<String>): String {
    val names = accepted.sorted()
    if (names.size == 1) return "does not use the scheme ${names.single()}"
    return "does not use one of the schemes ${names.joinToString(SCHEME_SEPARATOR)}"
}

private fun parsed(raw: String): URI = try {
    URI(raw)
} catch (expected: URISyntaxException) {
    throw ConversionException("is not a valid URI: ${expected.reason}")
}
