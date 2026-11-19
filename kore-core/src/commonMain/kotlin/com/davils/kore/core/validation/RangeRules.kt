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

import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty

/**
 * Reports an error at [property] if [value] is below [minimum].
 *
 * The violation has the code [ViolationCode.OUT_OF_RANGE] and a message that names [minimum], such as `must be at
 * least 1s`; it never quotes [value]. The bound is shown through its `toString` and escaped when the violation is
 * shown.
 *
 * @receiver the validation that collects the violation.
 * @param T the type of the value, which can be compared.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @param minimum the smallest valid value.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun <T : Comparable<T>> ValidationScope.atLeast(property: KProperty<*>, value: T, minimum: T): Boolean =
    checked(property = property, isValid = value >= minimum, message = "must be at least $minimum")

/**
 * Reports an error at [property] if [value] is above [maximum].
 *
 * The violation has the code [ViolationCode.OUT_OF_RANGE] and a message that names [maximum], such as `must be at
 * most 64 KiB`; it never quotes [value].
 *
 * @receiver the validation that collects the violation.
 * @param T the type of the value, which can be compared.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @param maximum the largest valid value.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun <T : Comparable<T>> ValidationScope.atMost(property: KProperty<*>, value: T, maximum: T): Boolean =
    checked(property = property, isValid = value <= maximum, message = "must be at most $maximum")

/**
 * Reports an error at [property] unless [value] lies in [range], both ends included.
 *
 * The violation has the code [ViolationCode.OUT_OF_RANGE] and a message that names both ends, such as `must lie
 * between 1 and 65535`; it never quotes [value].
 *
 * @receiver the validation that collects the violation.
 * @param T the type of the value, which can be compared.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @param range the valid values.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun <T : Comparable<T>> ValidationScope.inRange(
    property: KProperty<*>,
    value: T,
    range: ClosedRange<T>,
): Boolean {
    val message = "must lie between ${range.start} and ${range.endInclusive}"
    return checked(property = property, isValid = value in range, message = message)
}
