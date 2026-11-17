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

package com.davils.kore.core.validation

import com.davils.kore.core.validation.report.ValidationReport
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.validation.violation.ViolationPath

/**
 * Validates this value and returns every problem found as a report.
 *
 * Runs [Validatable.validate] once, on the calling thread, at the root path and with
 * [ViolationCode.INVALID] as the default code. The validation collects at most
 * [Limits.MAX_VIOLATIONS][com.davils.kore.core.limit.Limits.MAX_VIOLATIONS] violations and counts the rest, and
 * it stops descending below [Limits.MAX_PATH_DEPTH][com.davils.kore.core.limit.Limits.MAX_PATH_DEPTH] levels,
 * as [ValidationScope] describes. It never throws because the value is invalid; an exception thrown by the rules
 * of the value reaches the caller unchanged. The function is as thread-safe as the value: it keeps no state of its own,
 * but the value must not change while it is validated.
 *
 * @param subject what is validated, such as `server configuration`, named in the headline of the rendered report.
 * It is kept as given in [ValidationReport.subject] and escaped and shortened wherever the report is shown, so it
 * may come from input, but it is never masked and must not hold a secret.
 * @return the report, which is valid when no error was reported.
 * @since 1.0.0
 */
public fun Validatable.validate(subject: String): ValidationReport {
    val scope = ValidationScope(ViolationPath.ROOT, ViolationCode.INVALID)
    with(this) { scope.validate() }
    return ValidationReport(subject, scope.violations())
}

/**
 * Validates this value and returns it when it has no errors, so a boundary checks and takes over a value in one step.
 *
 * Validates exactly as [validate][com.davils.kore.core.validation.validate] does, with the same limits. Warnings alone
 * never make it throw, unless the validation finds more than
 * [Limits.MAX_VIOLATIONS][com.davils.kore.core.limit.Limits.MAX_VIOLATIONS] problems: the
 * [TOO_MANY][ViolationCode.TOO_MANY] violation that then counts the rest is an error, whatever the severity of the
 * problems it counts.
 *
 * @param T the type of the validated value.
 * @param subject what is validated, such as `server configuration`, named in the headline of the rendered report.
 * It is kept as given and escaped and shortened wherever the report is shown, so it may come from input, but it is
 * never masked and must not hold a secret.
 * @return this value, unchanged.
 * @throws com.davils.kore.core.validation.report.ValidationException if the validation found at least one error;
 * its report holds every violation, and its message is the rendered report, escaped and bounded in size.
 * @since 1.0.0
 */
public fun <T : Validatable> T.validateOrThrow(subject: String): T {
    val report = validate(subject)
    report.throwIfInvalid()
    return this
}
