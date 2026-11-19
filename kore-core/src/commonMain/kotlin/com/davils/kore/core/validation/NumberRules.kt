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
import kotlin.time.Duration

private const val POSITIVE: String = "must be greater than 0"
private const val NOT_NEGATIVE: String = "must not be negative"
private const val POSITIVE_DURATION: String = "must be a positive, finite duration"
private const val NOT_NEGATIVE_DURATION: String = "must be zero or a positive, finite duration"

/**
 * Reports an error at [property] unless [value] is greater than zero.
 *
 * The violation has the code [ViolationCode.OUT_OF_RANGE] and the message `must be greater than 0`; it never quotes
 * [value], so the rule may check a secret.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.positive(property: KProperty<*>, value: Int): Boolean =
    checked(property = property, isValid = value > 0, message = POSITIVE)

/**
 * Reports an error at [property] unless [value] is greater than zero.
 *
 * It behaves like the [positive] for [Int].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.positive(property: KProperty<*>, value: Long): Boolean =
    checked(property = property, isValid = value > 0L, message = POSITIVE)

/**
 * Reports an error at [property] unless [value] is longer than zero and finite.
 *
 * An infinite duration is refused, because a timeout or an interval that never ends is a classic way to hang a
 * service; a value that may be infinite is checked by its own rule. The violation has the code
 * [ViolationCode.OUT_OF_RANGE] and the message `must be a positive, finite duration`.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.positive(property: KProperty<*>, value: Duration): Boolean =
    checked(property = property, isValid = value.isPositive() && value.isFinite(), message = POSITIVE_DURATION)

/**
 * Reports an error at [property] if [value] is below zero.
 *
 * The violation has the code [ViolationCode.OUT_OF_RANGE] and the message `must not be negative`; it never quotes
 * [value].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.nonNegative(property: KProperty<*>, value: Int): Boolean =
    checked(property = property, isValid = value >= 0, message = NOT_NEGATIVE)

/**
 * Reports an error at [property] if [value] is below zero.
 *
 * It behaves like the [nonNegative] for [Int].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.nonNegative(property: KProperty<*>, value: Long): Boolean =
    checked(property = property, isValid = value >= 0L, message = NOT_NEGATIVE)

/**
 * Reports an error at [property] if [value] is negative or infinite.
 *
 * The violation has the code [ViolationCode.OUT_OF_RANGE] and the message `must be zero or a positive, finite
 * duration`.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.nonNegative(property: KProperty<*>, value: Duration): Boolean =
    checked(property = property, isValid = !value.isNegative() && value.isFinite(), message = NOT_NEGATIVE_DURATION)

internal fun ValidationScope.checked(property: KProperty<*>, isValid: Boolean, message: String): Boolean {
    if (!isValid) fail(property = property, message = message, code = ViolationCode.OUT_OF_RANGE)
    return isValid
}
