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
 * Reports an error at [property] if [value] is empty or consists of white space only.
 *
 * The violation has the code [ViolationCode.REQUIRED] and the message `must not be blank`.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.notBlank(property: KProperty<*>, value: CharSequence): Boolean {
    val isValid = value.isNotBlank()
    if (!isValid) fail(property = property, message = "must not be blank", code = ViolationCode.REQUIRED)
    return isValid
}

/**
 * Reports an error at [property] if [value] is longer than [maximum] UTF-16 code units.
 *
 * The violation has the code [ViolationCode.TOO_LONG] and a message that names [maximum], such as `must be at most 64
 * characters long`; it never quotes [value], so the rule may check a secret.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the value belongs to; the violation is reported at it.
 * @param value the value to check.
 * @param maximum the most UTF-16 code units the value may have, zero or more.
 * @return `true` if [value] passed, `false` if a violation was reported.
 * @throws IllegalArgumentException if [maximum] is negative.
 * @since 2.1.0
 */
public fun ValidationScope.maxLength(property: KProperty<*>, value: CharSequence, maximum: Int): Boolean {
    require(maximum >= 0) { "The longest length of a text cannot be negative, but was $maximum" }
    val isValid = value.length <= maximum
    val message = "must be at most $maximum characters long"
    if (!isValid) fail(property = property, message = message, code = ViolationCode.TOO_LONG)
    return isValid
}
