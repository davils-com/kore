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

package com.davils.kore.core.arguments.report

import com.davils.kore.core.arguments.conversion.maskedUserInfo
import com.davils.kore.core.text.internal.SafeDisplay

private const val LIST_START: String = "["
private const val LIST_END: String = "]"
private const val NULL_TEXT: String = "null"

internal fun displayedValue(value: Any?, isSensitive: Boolean): String {
    if (isSensitive) return SafeDisplay.REDACTED
    return SafeDisplay.text(plainText(value))
}

internal fun originSuffix(origin: ValueOrigin): String = when (origin) {
    is ValueOrigin.CommandLine -> "(from the command line)"
    is ValueOrigin.Default -> "(default)"
    is ValueOrigin.Environment -> "(from ${SafeDisplay.text(origin.variable)})"
    is ValueOrigin.External -> "(from ${SafeDisplay.text(origin.sourceName)})"
}

private fun plainText(value: Any?): String {
    if (value !is List<*>) return elementText(value)
    return value.joinToString(prefix = LIST_START, postfix = LIST_END) { element -> elementText(element) }
}

private fun elementText(value: Any?): String {
    val maskedValue = maskedByPlatform(value)
    if (maskedValue != null) return maskedValue
    return maskedUserInfo(value?.toString() ?: NULL_TEXT)
}
