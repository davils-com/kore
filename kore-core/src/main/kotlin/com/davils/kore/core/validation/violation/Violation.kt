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

package com.davils.kore.core.validation.violation

import com.davils.kore.core.Limits
import com.davils.kore.core.text.internal.SafeDisplay

/**
 * One problem a validation found: where it is, what kind of problem it is, how severe it is and what is wrong.
 *
 * Kore creates a violation for every problem reported to a
 * [ValidationScope][com.davils.kore.core.validation.ValidationScope]; its constructor is internal, so Kotlin
 * code outside Kore cannot create one. A violation is immutable and therefore safe to share between threads.
 *
 * The [message] is kept exactly as the validation wrote it, so it may contain text that came from the validated
 * input. Whatever Kore shows of a violation, [toString] and every rendered report included, escapes the message and
 * bounds its length first; code that writes the [message] anywhere itself must escape it as well. The message is
 * escaped but never masked, so a validation must not quote a secret in it.
 *
 * @property path where in the validated value the problem was found.
 * @property code the stable name of the kind of problem.
 * @property severity whether the problem makes the value invalid.
 * @property message what is wrong, as the validation wrote it.
 * @since 1.0.0
 */
public class Violation internal constructor(
    public val path: ViolationPath,
    public val code: ViolationCode,
    public val severity: Severity,
    public val message: String,
) {
    private val identity: List<Any> get() = listOf(path, code, severity, message)

    internal fun under(prefix: ViolationPath): Violation =
        Violation(path = prefix + path, code = code, severity = severity, message = message)

    /**
     * Compares two violations by their path, code, severity and message.
     *
     * @param other the object to compare with.
     * @return whether [other] is a violation with an equal path, code, severity and message.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is Violation && identity == other.identity

    /**
     * Returns a hash code derived from the path, code, severity and message, consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = identity.hashCode()

    /**
     * Returns the violation as one line, such as `Error at 'name' (kore.validation.invalid): must not be blank`.
     *
     * The path is escaped as [ViolationPath.toString] describes, and the message is escaped and shortened to
     * [Limits.MAX_MESSAGE_LENGTH] UTF-16 code units, so the line is safe to log and to show on a terminal and never
     * breaks into several lines.
     *
     * @return the violation as one line of text.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shown = SafeDisplay.text(message, Limits.MAX_MESSAGE_LENGTH)
        return "$severity at '$path' ($code): $shown"
    }
}
