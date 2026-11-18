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

private const val SCHEME_SEPARATOR: String = "://"
private const val USER_INFO_END: Char = '@'
private const val NOT_FOUND: Int = -1

private val leadingAuthority: Regex =
    Regex("""^[\x00-\x20]*(?:[A-Za-z][A-Za-z0-9+.\-\t\n\r]*:|[/\\][\t\n\r]*[/\\])[/\\\t\n\r]*""")

internal fun maskedUserInfo(text: String): String {
    val authorityStart = authorityStartIn(text)
    val end = text.lastIndexOf(USER_INFO_END)
    if (authorityStart < 0 || end < authorityStart) return text
    return text.substring(startIndex = 0, endIndex = authorityStart) + SafeDisplay.REDACTED + text.substring(end)
}

private fun authorityStartIn(text: String): Int {
    val leading = leadingAuthority.find(text) ?: return afterSchemeSeparator(text.indexOf(SCHEME_SEPARATOR))
    return leading.value.length
}

private fun afterSchemeSeparator(separator: Int): Int {
    if (separator < 0) return NOT_FOUND
    return separator + SCHEME_SEPARATOR.length
}
