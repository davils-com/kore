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

package com.davils.kore.core.text.diagnostic

import com.davils.kore.core.Limits
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.internal.SafeDisplay
import com.davils.kore.core.validation.violation.Severity

private const val INDENT: String = "  "
private const val LINE_BREAK: String = "\n"
private const val PARAGRAPH_BREAK: String = "\n\n"

/**
 * Renders [diagnostics] of a text as a report for a person, with the line and column of each, such as:
 *
 * ```
 * Invalid settings.yaml (2 problems):
 *   3:7 unknown key [acme.yaml.unknown-key]
 *   9:1 the document ends inside a list [acme.yaml.unterminated]
 *
 * 1 warning:
 *   1:1 the file starts with a byte order mark [acme.yaml.bom]
 * ```
 *
 * Errors come before warnings, and each group is ordered by the position of the diagnostics in the text, keeping the
 * order of [diagnostics] for equal positions. The position is that of the start of the range, as
 * [com.davils.kore.core.text.TextPosition] shows it: the line and the column in UTF-16 code units, both counted from
 * 1. Each group shows at most [Limits.MAX_RENDERED_VIOLATIONS] diagnostics and ends with how many it left out. A
 * report without diagnostics is the single line `Valid settings.yaml.`.
 *
 * The [subject] and every message are escaped and shortened, the subject to [Limits.MAX_DISPLAY_LENGTH] and a message
 * to [Limits.MAX_MESSAGE_LENGTH] UTF-16 code units, so the report is safe to log and to show on a terminal however
 * hostile the text was; the codes are shown as they are.
 *
 * @param subject what the text is, such as the name of its file.
 * @param diagnostics the diagnostics of the text.
 * @param lines the line map of the text the diagnostics belong to.
 * @return the report, one line per diagnostic plus a headline per group.
 * @throws IllegalArgumentException if the range of a diagnostic starts beyond the end of the text of [lines].
 * @since 2.1.0
 */
public fun renderDiagnostics(subject: String, diagnostics: List<Diagnostic>, lines: LineMap): String {
    val shownSubject = SafeDisplay.text(subject)
    if (diagnostics.isEmpty()) return "Valid $shownSubject."
    val ordered = diagnostics.sortedBy { diagnostic -> diagnostic.range.start }
    val errors = ordered.filter { diagnostic -> diagnostic.severity == Severity.Error }
    val warnings = ordered.filter { diagnostic -> diagnostic.severity == Severity.Warning }
    val blocks = buildList {
        if (errors.isNotEmpty()) add(errorHeadline(shownSubject, errors.size) + LINE_BREAK + renderLines(errors, lines))
        if (warnings.isNotEmpty()) add(warningHeadline(warnings.size) + LINE_BREAK + renderLines(warnings, lines))
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

private fun renderLines(diagnostics: List<Diagnostic>, lines: LineMap): String {
    val shown = diagnostics.take(Limits.MAX_RENDERED_VIOLATIONS)
    val rendered = shown.map { diagnostic -> renderLine(diagnostic, lines) }
    val omitted = diagnostics.size - shown.size
    if (omitted == 0) return rendered.joinToString(separator = LINE_BREAK)
    return (rendered + "$INDENT… and $omitted more").joinToString(separator = LINE_BREAK)
}

private fun renderLine(diagnostic: Diagnostic, lines: LineMap): String {
    val position = lines.positionOf(diagnostic.range.start)
    val message = SafeDisplay.text(diagnostic.message, Limits.MAX_MESSAGE_LENGTH)
    return "$INDENT$position $message [${diagnostic.code}]"
}
