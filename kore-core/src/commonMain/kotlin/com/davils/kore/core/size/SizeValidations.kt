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

package com.davils.kore.core.size

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.checked
import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty

/**
 * Reads [text] as a data size inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [DataSize.parse] reads it, with the same grammar and the same limit. A text that is no size is
 * reported as an error with the code [SizeViolationCodes.INVALID_SIZE] and a message such as `is not a data size such
 * as 64 MiB: it uses a unit that is decimal, abbreviated or in the wrong case; write B, KiB, MiB, GiB, TiB, PiB or
 * EiB`. The message never quotes [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the size, or `null` if [text] is no data size and a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.dataSize(property: KProperty<*>, text: String): DataSize? =
    when (val reading = readDataSize(text)) {
        is Outcome.Success -> reading.value

        is Outcome.Failure -> reject(
            property = property,
            message = "is not a data size such as 64 MiB: it ${reading.failure}",
            code = SizeViolationCodes.INVALID_SIZE,
        )
    }

/**
 * Reports an error at [property] unless [value] is larger than zero bytes.
 *
 * The violation has the code [ViolationCode.OUT_OF_RANGE] and the message `must be larger than 0 B`.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.positive(property: KProperty<*>, value: DataSize): Boolean =
    checked(property = property, isValid = value.bytes > 0L, message = "must be larger than 0 B")
