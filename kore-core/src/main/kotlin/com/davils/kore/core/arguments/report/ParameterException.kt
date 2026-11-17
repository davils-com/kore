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

import com.davils.kore.core.immutableList
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.report.ValidationReport

private const val SUBJECT: String = "program parameters"

/**
 * Thrown when a load of [com.davils.kore.core.arguments.Parameters] finds at least one error; it names every
 * problem of that load at once.
 *
 * When it is thrown, no parameter of the schema has been assigned. Its message is the rendered report of the
 * [violations], escaped and bounded in size, so it is safe to log and to show to the person who started the program; it
 * does not include the [usage], which a program prints separately when it wants to. The values of sensitive parameters
 * appear in neither.
 *
 * @property usage the listing of the parameters of the schema, as [com.davils.kore.core.arguments.Parameters.usage]
 * returns it.
 * @since 1.0.0
 */
public class ParameterException internal constructor(
    violations: List<ParameterViolation>,
    public val usage: String,
) : ValidationException(reportOf(violations)) {
    /**
     * Every violation of the failed load in the order it was found, warnings included; the list cannot be changed.
     *
     * The messages and paths of the violations may contain text from the input and are escaped only when a violation is
     * rendered, such as by its [ParameterViolation.toString].
     *
     * @since 1.0.0
     */
    public val violations: List<ParameterViolation> = violations.immutableList()
}

private fun reportOf(violations: List<ParameterViolation>): ValidationReport =
    ValidationReport(SUBJECT, violations.map { violation -> violation.toViolation() })
