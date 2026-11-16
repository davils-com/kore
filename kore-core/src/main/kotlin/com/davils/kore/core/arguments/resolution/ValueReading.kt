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

package com.davils.kore.core.arguments.resolution

import com.davils.kore.core.arguments.KoreParameterInfo
import com.davils.kore.core.arguments.KoreParameterLimits
import com.davils.kore.core.arguments.conversion.NamedConverter
import com.davils.kore.core.arguments.declaration.ValueRules
import com.davils.kore.core.arguments.lexer.RawValues
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreParameterViolationCodes
import com.davils.kore.core.arguments.report.KoreValueOrigin
import com.davils.kore.core.validation.KoreValidationScope
import com.davils.kore.core.validation.violation.KoreSeverity
import com.davils.kore.core.validation.violation.KoreViolationPath

private const val FAILED_RULE_DETAIL: String = "could not be validated, because one of its rules failed unexpectedly"

internal class ValueReading<T : Any>(
    val info: KoreParameterInfo,
    private val rules: ValueRules<T>,
    private val limits: KoreParameterLimits,
) {
    private val isCheckingDefaults: Boolean = (rules.converter as? NamedConverter<*>)?.isCheckingDefaults == true

    fun read(given: RawValues, violations: MutableList<KoreParameterViolation>): List<T> {
        val pieces = admittedPieces(given, violations) ?: return emptyList()
        return pieces.mapNotNull { piece -> readOne(piece, given.origin, violations) }
    }

    fun admitsDefault(value: T, violations: MutableList<KoreParameterViolation>): Boolean {
        val isConvertible = !isCheckingDefaults ||
            converted(value.toString(), KoreValueOrigin.Default, violations) != null
        return isConvertible && isValid(value, KoreValueOrigin.Default, violations)
    }

    private fun admittedPieces(given: RawValues, violations: MutableList<KoreParameterViolation>): List<String>? {
        if (!given.raws.all(limits::admitsLength)) {
            violations += KoreParameterViolation.ValueTooLong(info, given.origin, limits.maxArgumentLength)
            return null
        }
        return countedPieces(given, violations)
    }

    private fun countedPieces(given: RawValues, violations: MutableList<KoreParameterViolation>): List<String>? {
        val admitted = given.raws.takeIf { limits.admitsValues(given.occurrences) }
        val pieces = admitted?.let(::piecesOf)
        if (pieces == null) {
            violations += KoreParameterViolation.TooManyValues(info, given.origin, limits.maxValuesPerParameter)
        }
        return pieces
    }

    private fun readOne(raw: String, origin: KoreValueOrigin, violations: MutableList<KoreParameterViolation>): T? {
        val value = converted(raw, origin, violations) ?: return null
        return value.takeIf { isValid(value, origin, violations) }
    }

    private fun converted(raw: String, origin: KoreValueOrigin, violations: MutableList<KoreParameterViolation>): T? {
        val result = catchingForeign { rules.converter.convert(raw) }
        result.exceptionOrNull()?.let { cause ->
            violations += KoreParameterViolation.ConversionFailure(
                parameter = info,
                origin = origin,
                given = raw,
                thrown = cause,
            )
        }
        return result.getOrNull()
    }

    private fun isValid(value: T, origin: KoreValueOrigin, violations: MutableList<KoreParameterViolation>): Boolean {
        val scope = KoreValidationScope(KoreViolationPath.ROOT, KoreParameterViolationCodes.VALIDATION_FAILED)
        val outcomes = rules.validators.map { rule -> catchingForeign { scope.rule(value) } }
        val reported = scope.violations()
        reported.forEach { failure ->
            violations += KoreParameterViolation.ValidationFailure(
                parameter = info,
                origin = origin,
                detail = failure.message,
                code = failure.code,
                severity = failure.severity,
            )
        }
        val hasFailedRule = outcomes.any { outcome -> outcome.isFailure }
        if (hasFailedRule) violations += failedRule(origin)
        return !hasFailedRule && reported.none { failure -> failure.severity == KoreSeverity.Error }
    }

    private fun failedRule(origin: KoreValueOrigin): KoreParameterViolation = KoreParameterViolation.ValidationFailure(
        parameter = info,
        origin = origin,
        detail = FAILED_RULE_DETAIL,
        code = KoreParameterViolationCodes.VALIDATION_FAILED,
        severity = KoreSeverity.Error,
    )

    private fun piecesOf(raws: List<String>): List<String>? {
        val delimiter = rules.splitDelimiter ?: return raws
        val pieces = ArrayList<String>()
        val isWithinLimit = raws.all { raw -> splitInto(raw, delimiter, pieces) }
        return pieces.takeIf { isWithinLimit }
    }

    private fun splitInto(raw: String, delimiter: String, pieces: MutableList<String>): Boolean {
        var start = 0
        while (start <= raw.length) {
            val found = raw.indexOf(delimiter, start)
            val end = found.takeIf { found >= 0 } ?: raw.length
            if (end > start) pieces += raw.substring(startIndex = start, endIndex = end)
            if (!limits.admitsValues(pieces.size)) return false
            start = end + delimiter.length
        }
        return true
    }
}
