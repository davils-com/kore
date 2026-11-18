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

private const val SCHEME_SEPARATOR: String = "://"
private const val AUTHORITY_PREFIX: String = "//"
private const val USER_INFO_END: Char = '@'
private const val SCHEME_END: Char = ':'
private const val QUERY_START: Char = '?'
private const val FRAGMENT_START: Char = '#'

internal fun maskedUserInfo(uri: URI): String {
    val authority = uri.rawAuthority
    if (uri.isOpaque || authority == null) return maskedUserInfo(uri.toString())
    return maskedAuthority(uri, authority)
}

internal fun hasUserInfo(uri: URI): Boolean {
    if (uri.isOpaque) return hasNestedUserInfo(uri.toString())
    return uri.rawUserInfo != null || uri.rawAuthority?.contains(USER_INFO_END) == true
}

private fun hasNestedUserInfo(text: String): Boolean {
    val separator = text.indexOf(SCHEME_SEPARATOR)
    return separator >= 0 && text.lastIndexOf(USER_INFO_END) > separator
}

private fun maskedAuthority(uri: URI, authority: String): String {
    if (USER_INFO_END !in authority) return uri.toString()
    return buildString {
        uri.scheme?.let { scheme -> append(scheme).append(SCHEME_END) }
        append(AUTHORITY_PREFIX).append(SafeDisplay.REDACTED)
        append(authority.substring(authority.lastIndexOf(USER_INFO_END)))
        append(uri.rawPath.orEmpty())
        uri.rawQuery?.let { query -> append(QUERY_START).append(query) }
        uri.rawFragment?.let { fragment -> append(FRAGMENT_START).append(fragment) }
    }
}
