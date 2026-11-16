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

package com.davils.kore.core.arguments.declaration

import com.davils.kore.core.arguments.KoreParameterInfo
import com.davils.kore.core.arguments.lexer.RawValues
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreValueOrigin
import com.davils.kore.core.arguments.resolution.Assignment
import com.davils.kore.core.arguments.resolution.ParameterSlot
import com.davils.kore.core.arguments.resolution.SpecResolution
import com.davils.kore.core.arguments.resolution.ValueReading
import com.davils.kore.core.validation.violation.KoreSeverity

internal class ParameterSpec<T : Any, V>(
    val info: KoreParameterInfo,
    val propertyName: String,
    val negatedNames: List<String>,
    private val reading: ValueReading<T>,
    private val arity: Arity<T, V>,
    val slot: ParameterSlot<V>,
) {
    val matchableNames: List<String> get() = info.names + negatedNames

    fun resolve(given: RawValues?, exposure: KoreSeverity): SpecResolution {
        val violations = mutableListOf<KoreParameterViolation>()
        val advice = info.deprecation
        if (given != null && advice != null) violations += KoreParameterViolation.Deprecated(info, advice)
        val isExposed = info.isSensitive && given?.origin == KoreValueOrigin.CommandLine
        if (isExposed) violations += KoreParameterViolation.SensitiveOnCommandLine(info, exposure)
        val assignment = assignmentFor(given, violations)
        return SpecResolution(violations = violations, isProvided = given != null, assignment = assignment)
    }

    fun describeValue(): String = slot.describe(info.isSensitive)

    private fun assignmentFor(given: RawValues?, violations: MutableList<KoreParameterViolation>): Assignment? {
        if (given == null) return arity.absent(reading, slot, violations)
        val occurrences = given.occurrences
        if (!info.isRepeatable && occurrences > 1) violations += KoreParameterViolation.Repeated(info, occurrences)
        val values = reading.read(given, violations)
        return arity.given(values, given.origin, slot)
    }
}
