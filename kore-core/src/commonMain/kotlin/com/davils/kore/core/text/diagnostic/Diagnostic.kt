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
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.text.internal.SafeDisplay
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * A problem found in a text, such as a document that could not be read, at the [range] of the text it concerns.
 *
 * A diagnostic is what a parser of YAML, XML or any other format reports for a place in its input; [renderDiagnostics]
 * shows a list of them with the line and column of each. The [code] is a stable key for the kind of problem, such as
 * `acme.yaml.unknown-key`, and the [message] says what is wrong in words. The message may contain parts of the text
 * and is escaped wherever Kore shows it. Diagnostics are immutable, safe to share between threads, and equal when
 * all four parts are equal.
 *
 * @property code the stable key of the kind of problem.
 * @property severity whether the problem is an error or a warning.
 * @property range the part of the text the problem concerns, as UTF-16 offsets; empty for a position between two
 * characters.
 * @property message what is wrong, in words.
 * @since 2.1.0
 */
public class Diagnostic(
    public val code: ViolationCode,
    public val severity: Severity,
    public val range: TextRange,
    public val message: String,
) {
    private val identity: List<Any> get() = listOf(code, severity, range, message)

    /**
     * Tells whether [other] is a diagnostic with the same code, severity, range and message.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal diagnostic.
     * @since 2.1.0
     */
    override fun equals(other: Any?): Boolean = other is Diagnostic && identity == other.identity

    /**
     * Returns a hash code derived from the code, severity, range and message, consistent with [equals].
     *
     * @return the hash code.
     * @since 2.1.0
     */
    override fun hashCode(): Int = identity.hashCode()

    /**
     * Returns the diagnostic as one line, such as `Error at 12..15 (acme.yaml.unknown-key): unknown key`.
     *
     * The message is escaped and shortened to [Limits.MAX_MESSAGE_LENGTH] UTF-16 code units, so the line is safe to
     * log and to show on a terminal and never breaks into several lines.
     *
     * @return the diagnostic as one line of text.
     * @since 2.1.0
     */
    override fun toString(): String {
        val shown = SafeDisplay.text(message, Limits.MAX_MESSAGE_LENGTH)
        return "$severity at $range ($code): $shown"
    }
}
