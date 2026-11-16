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

import com.davils.kore.core.immutableList
import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.validation.violation.KoreSeverity
import com.davils.kore.core.validation.violation.KoreViolation

/**
 * The outcome of a validation: every violation it found, split into errors and warnings.
 *
 * Kore creates a report at the end of every validation, such as one run by
 * [validate][com.davils.kore.core.validation.validate]; its constructor is internal, so Kotlin code outside Kore cannot
 * create one. A report is immutable, its lists cannot be changed even through a cast, and it is safe to share between
 * threads. A value is valid when the report holds no errors; warnings never make it invalid.
 *
 * A validation through a [KoreValidationScope][com.davils.kore.core.validation.KoreValidationScope] collects at most
 * [KoreLimits.MAX_VIOLATIONS] violations; when it finds more, its report ends with one further error with the code
 * [KoreViolationCode.TOO_MANY][com.davils.kore.core.validation.violation.KoreViolationCode.TOO_MANY] that counts the
 * rest.
 *
 * The text of a report, as [render] writes it, escapes everything that came from the validated input: the subject is
 * shortened to [KoreLimits.MAX_DISPLAY_LENGTH] UTF-16 code units, every message to [KoreLimits.MAX_MESSAGE_LENGTH],
 * and at most [KoreLimits.MAX_RENDERED_VIOLATIONS] violations per severity are listed before the text names how many it
 * left out. The text is therefore safe to log and to show on a terminal. It shows the listed messages escaped but
 * never masked, so it is free of secrets only when the messages are.
 *
 * @property subject what was validated, such as `server configuration`, exactly as the caller of the validation gave
 * it; it is escaped only in the rendered text.
 * @since 1.0.0
 */
public class KoreValidationReport internal constructor(
    public val subject: String,
    violations: List<KoreViolation>,
    private val footer: String?,
) {
    /**
     * Every violation, errors and warnings alike, in the order the validation found them.
     *
     * @since 1.0.0
     */
    public val violations: List<KoreViolation> = violations.immutableList()

    /**
     * The violations with the severity [KoreSeverity.Error], which make the value invalid, in the order they were
     * found.
     *
     * @since 1.0.0
     */
    public val errors: List<KoreViolation> =
        this.violations.filter { it.severity == KoreSeverity.Error }.immutableList()

    /**
     * The violations with the severity [KoreSeverity.Warning], which only point out something questionable, in the
     * order they were found.
     *
     * @since 1.0.0
     */
    public val warnings: List<KoreViolation> =
        this.violations.filter { it.severity == KoreSeverity.Warning }.immutableList()

    /**
     * Whether the validated value has no errors; warnings do not count.
     *
     * @since 1.0.0
     */
    public val isValid: Boolean get() = errors.isEmpty()

    internal constructor(subject: String, violations: List<KoreViolation>) : this(subject, violations, null)

    /**
     * Throws a [KoreValidationException] that carries this report when the validated value has errors, and does
     * nothing otherwise.
     *
     * @throws KoreValidationException if [isValid] is false; its message is the text of [render].
     * @since 1.0.0
     */
    public fun throwIfInvalid() {
        if (isValid) return
        throw KoreValidationException(this)
    }

    /**
     * Returns the report as text for a person to read, with every value from the input escaped.
     *
     * A report without violations renders as one line, such as `Valid server configuration.`. Otherwise the text lists
     * the errors, if there are any, under a headline that names the subject and counts them, and then the warnings, if
     * there are any, under a headline that counts them; each listed violation takes one line with its path and its
     * message. The limits of the class description apply, and the text is built anew on every call.
     *
     * @return the rendered report.
     * @since 1.0.0
     */
    public fun render(): String = renderReport(subject = subject, errors = errors, warnings = warnings, footer = footer)

    /**
     * Returns the same text as [render].
     *
     * @return the rendered report.
     * @since 1.0.0
     */
    override fun toString(): String = render()
}
