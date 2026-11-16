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

package com.davils.kore.core.platform

import com.davils.kore.core.annotation.DavilsInternal

/**
 * The outcome of a native call encoded in one [Long]: zero or more is a success carrying that value, a negative
 * number a failure carrying the negated error code of the operating system.
 *
 * A result comes from [fromEncoded], for what a native function returned, or from [success] and [failure]. A failure
 * always carries an error code above zero, up to [Long.MAX_VALUE], so codes above [Int.MAX_VALUE] such as an HRESULT
 * are kept whole, and zero is always a success. [Long.MIN_VALUE] is refused, because no error code negates to it. A
 * result is an immutable value and safe to share between threads; results are equal when they encode the same number.
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary: the package is exported, so any code on the class path can call it.
 *
 * @since 1.0.0
 */
@DavilsInternal
@JvmInline
public value class KoreNativeResult private constructor(private val encoded: Long) {
    /**
     * Whether the native call succeeded, that is, the encoded number is zero or more.
     *
     * @since 1.0.0
     */
    public val isSuccess: Boolean get() = encoded >= 0L

    /**
     * The error code of the operating system for a failed call, always above zero, or zero for a success.
     *
     * @since 1.0.0
     */
    public val errorCode: Long
        get() {
            if (isSuccess) return 0L
            return -encoded
        }

    /**
     * The value of a successful call, zero or more.
     *
     * Throws an [IllegalStateException] that names the [errorCode] if the call failed; check [isSuccess] first.
     *
     * @since 1.0.0
     */
    public val value: Long
        get() {
            check(isSuccess) { "A failed native result carries no value, only the operating system error $errorCode" }
            return encoded
        }

    /**
     * Returns `KoreNativeResult(value=n)` for a success and `KoreNativeResult(errorCode=n)` for a failure.
     *
     * @return the result as text, which holds only digits besides the fixed words.
     * @since 1.0.0
     */
    override fun toString(): String {
        if (isSuccess) return "KoreNativeResult(value=$encoded)"
        return "KoreNativeResult(errorCode=${-encoded})"
    }

    /**
     * Builds native results from what a native function returned or from a value or an error code.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the number a native function returned: zero or more as a success, a negative number as a failure
         * with the negated number as its error code.
         *
         * @param encoded the number the native function returned.
         * @return the result the number encodes.
         * @throws IllegalArgumentException if [encoded] is [Long.MIN_VALUE], whose negation is not an error code.
         * @since 1.0.0
         */
        public fun fromEncoded(encoded: Long): KoreNativeResult {
            require(encoded != Long.MIN_VALUE) {
                "An encoded native result cannot be ${Long.MIN_VALUE}, because no error code above zero negates to it"
            }
            return KoreNativeResult(encoded)
        }

        /**
         * Builds the result of a successful call that returned [value].
         *
         * @param value the value of the call, zero or more.
         * @return the successful result.
         * @throws IllegalArgumentException if [value] is negative.
         * @since 1.0.0
         */
        public fun success(value: Long): KoreNativeResult {
            require(value >= 0L) { "A successful native result must not be negative, but was $value" }
            return KoreNativeResult(value)
        }

        /**
         * Builds the result of a call that failed with the error code [code] of the operating system.
         *
         * @param code the error code, above zero.
         * @return the failed result.
         * @throws IllegalArgumentException if [code] is zero or negative, so a failure never looks like a success.
         * @since 1.0.0
         */
        public fun failure(code: Long): KoreNativeResult {
            require(code > 0L) { "An operating system error code must be above zero, but was $code" }
            return KoreNativeResult(-code)
        }
    }
}
