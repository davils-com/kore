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

import com.davils.kore.core.arguments.ParameterInfo
import com.davils.kore.core.arguments.declaration.ParameterSpec
import com.davils.kore.core.limit.Limits
import com.davils.kore.core.text.escape.SafeDisplay

private const val COLUMN_LIMIT: Int = 34
private const val COLUMN_GAP: Int = 2
private const val INDENT: String = "  "
private const val LINE_BREAK: String = "\n"
private const val NO_PARAMETERS: String = "This program takes no parameters."

internal fun renderUsage(infos: List<ParameterInfo>): String {
    if (infos.isEmpty()) return NO_PARAMETERS
    val entries = infos.map { info -> renderSignature(info) to renderAnnotations(info) }
    val width = columnWidth(entries.map { (signature, _) -> signature })
    val lines = entries.joinToString(separator = LINE_BREAK) { (signature, annotations) ->
        "$INDENT${column(signature, width)}$annotations".trimEnd()
    }
    return "Supported parameters:$LINE_BREAK$lines"
}

internal fun renderValues(specs: List<ParameterSpec<*, *>>): String {
    if (specs.isEmpty()) return NO_PARAMETERS
    val entries = specs.map { spec -> spec.info.primaryName to spec.describeValue() }
    val width = columnWidth(entries.map { (name, _) -> name })
    val lines = entries.joinToString(separator = LINE_BREAK) { (name, value) -> "$INDENT${column(name, width)}$value" }
    return "Parameters:$LINE_BREAK$lines"
}

private fun renderSignature(info: ParameterInfo): String {
    val names = info.names.joinToString(", ")
    if (info.isFlag) return names
    return "$names <${SafeDisplay.text(info.typeName)}>"
}

private fun renderAnnotations(info: ParameterInfo): String {
    val parts = listOfNotNull(
        info.description?.let { description -> SafeDisplay.text(description, Limits.MAX_MESSAGE_LENGTH) },
        "required".takeIf { info.isRequired },
        "repeatable".takeIf { info.isRepeatable },
        info.defaultDisplay?.let { display -> "default: $display" },
        info.environmentVariable?.let { variable -> "env: ${SafeDisplay.text(variable)}" },
        "deprecated".takeIf { info.deprecation != null },
    )
    return parts.joinToString(INDENT)
}

private fun columnWidth(labels: List<String>): Int {
    val longest = labels.maxOf { label -> label.length }
    return minOf(a = longest, b = COLUMN_LIMIT) + COLUMN_GAP
}

private fun column(label: String, width: Int): String {
    if (label.length >= width) return "$label "
    return label.padEnd(width)
}
