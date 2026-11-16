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

package com.davils.kore.core.version

import com.davils.kore.core.validation.KoreValidationScope
import com.davils.kore.core.validation.violation.KoreViolationCode
import com.davils.kore.core.version.grammar.VersionReading
import com.davils.kore.core.version.grammar.readVersion
import kotlin.reflect.KProperty

/**
 * Reads [text] as a semantic version inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [KoreSemanticVersion.parse] reads it, with the same limits. A text that is not a version is
 * reported as an error with the code [KoreVersionViolationCodes.INVALID_VERSION] and a message such as `is not a
 * semantic version: it has a number with a leading zero`. The message names the problem in fixed words and never
 * quotes [text], so a hostile text cannot place anything in a report.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the version, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun KoreValidationScope.semanticVersion(property: KProperty<*>, text: String): KoreSemanticVersion? =
    when (val reading = readVersion(text)) {
        is VersionReading.Read -> reading.version

        is VersionReading.Refused -> refused(
            property = property,
            message = "is not a semantic version: it ${reading.reason}",
            code = KoreVersionViolationCodes.INVALID_VERSION,
        )
    }

/**
 * Reads [text] as a range of semantic versions inside a validation, reporting a violation at [property] instead of
 * throwing.
 *
 * The text is read as [KoreSemanticVersionRange.parse] reads it, with the same grammar and the same limits. A text
 * that is not a range is reported as an error with the code [KoreVersionViolationCodes.INVALID_RANGE] and a message
 * such as `is not a version range: it has an operator with no version after it`. The message names the problem in
 * fixed words and never quotes [text], so a hostile text cannot place anything in a report.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the range, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun KoreValidationScope.semanticVersionRange(property: KProperty<*>, text: String): KoreSemanticVersionRange? =
    KoreSemanticVersionRange.read(
        text = text,
        read = { range -> range },
        refused = { reason ->
            refused(
                property = property,
                message = "is not a version range: it $reason",
                code = KoreVersionViolationCodes.INVALID_RANGE,
            )
        },
    )

private fun KoreValidationScope.refused(property: KProperty<*>, message: String, code: KoreViolationCode): Nothing? {
    fail(property = property, message = message, code = code)
    return null
}
