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

package com.davils.kore.core.validation.report

import com.davils.kore.core.Limits
import com.davils.kore.core.text.internal.SafeDisplay
import com.davils.kore.core.validation.violation.Violation

private const val PATH_COLUMN_LIMIT: Int = 28
private const val PATH_GAP: Int = 2
private const val INDENT: String = "  "
private const val LINE_BREAK: String = "\n"
private const val PARAGRAPH_BREAK: String = "\n\n"

internal fun renderReport(
    subject: String,
    errors: List<Violation>,
    warnings: List<Violation>,
    footer: String?,
): String {
    val shownSubject = SafeDisplay.text(subject)
    if (errors.isEmpty() && warnings.isEmpty()) return "Valid $shownSubject."
    val blocks = buildList {
        if (errors.isNotEmpty()) addAll(listOf(errorHeadline(shownSubject, errors.size), renderLines(errors)))
        if (warnings.isNotEmpty()) addAll(listOf(warningHeadline(warnings.size), renderLines(warnings)))
        footer?.let(::add)
    }
    return blocks.joinToString(PARAGRAPH_BREAK)
}

private fun errorHeadline(subject: String, count: Int): String {
    if (count == 1) return "Invalid $subject (1 problem):"
    return "Invalid $subject ($count problems):"
}

private fun warningHeadline(count: Int): String {
    if (count == 1) return "1 warning:"
    return "$count warnings:"
}

private fun renderLines(violations: List<Violation>): String {
    val shown = violations.take(Limits.MAX_RENDERED_VIOLATIONS)
    val width = columnWidth(shown)
    val lines = shown.map { violation -> renderLine(violation, width) }
    val omitted = violations.size - shown.size
    if (omitted == 0) return lines.joinToString(separator = LINE_BREAK)
    return (lines + "$INDENT… and $omitted more").joinToString(separator = LINE_BREAK)
}

private fun renderLine(violation: Violation, width: Int): String {
    val message = SafeDisplay.text(violation.message, Limits.MAX_MESSAGE_LENGTH)
    return "$INDENT${column(violation.path.toString(), width)}$message"
}

private fun columnWidth(violations: List<Violation>): Int {
    val longest = violations.maxOf { it.path.toString().length }
    return minOf(a = longest, b = PATH_COLUMN_LIMIT) + PATH_GAP
}

private fun column(label: String, width: Int): String {
    if (label.length >= width) return "$label "
    return label.padEnd(width)
}
