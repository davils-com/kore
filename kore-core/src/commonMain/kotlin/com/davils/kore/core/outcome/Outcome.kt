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

package com.davils.kore.core.outcome

import com.davils.kore.core.Limits
import com.davils.kore.core.text.internal.SafeDisplay

private const val NULL_TEXT: String = "null"

/**
 * The result of an operation that either succeeded with a value of type [T] or failed with a typed failure of type
 * [F], such as a sealed type of the reasons an input was refused.
 *
 * `Outcome` is for failures that a caller is expected to handle, where [Result] only carries a [Throwable]: an
 * operation that can fail in a few known ways returns `Outcome<Value, Problem>`, and the caller handles every case
 * with an exhaustive `when` over [Success] and [Failure], without `else`. A failure that is a programming error stays
 * an exception.
 *
 * There are exactly two kinds of outcome, and there will never be a third, so a `when` over them stays exhaustive in
 * every later version. Both type parameters are covariant: a `Success<Int>` is an `Outcome<Int, Nothing>` and so an
 * `Outcome<Int, Problem>` for any `Problem`.
 *
 * Outcomes are immutable as far as their value and their failure are, and equal when they are of the same kind with
 * equal contents. The transformations such as [map] and [flatMap] and the accessors such as [fold] and [getOrElse]
 * are extension functions in this package.
 *
 * @param T the type of the value of a success.
 * @param F the type of the failure.
 * @since 2.1.0
 */
public sealed interface Outcome<out T, out F> {
    /**
     * Whether this outcome is a [Success].
     *
     * @since 2.1.0
     */
    public val isSuccess: Boolean get() = this is Success

    /**
     * Whether this outcome is a [Failure].
     *
     * @since 2.1.0
     */
    public val isFailure: Boolean get() = this is Failure

    /**
     * An outcome that succeeded with [value].
     *
     * @param T the type of the value.
     * @property value the value the operation produced.
     * @since 2.1.0
     */
    public class Success<out T>(public val value: T) : Outcome<T, Nothing> {
        /**
         * Tells whether [other] is a success with an equal value.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Success] whose value equals this value.
         * @since 2.1.0
         */
        override fun equals(other: Any?): Boolean = other is Success<*> && value == other.value

        /**
         * Returns a hash code derived from the value, consistent with [equals].
         *
         * @return the hash code.
         * @since 2.1.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Returns the success as text, such as `Success(42)`.
         *
         * The text of the value is escaped and shortened to [Limits.MAX_DISPLAY_LENGTH] UTF-16 code units, so the
         * line is safe to log and to show on a terminal. A value whose own `toString` reveals a secret reveals it
         * here as well: a type that holds a secret masks it in its own `toString`.
         *
         * @return the success as one line of text.
         * @since 2.1.0
         */
        override fun toString(): String = "Success(${SafeDisplay.text(value?.toString() ?: NULL_TEXT)})"
    }

    /**
     * An outcome that failed with [failure].
     *
     * @param F the type of the failure.
     * @property failure why the operation failed.
     * @since 2.1.0
     */
    public class Failure<out F>(public val failure: F) : Outcome<Nothing, F> {
        /**
         * Tells whether [other] is a failure with an equal failure.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Failure] whose failure equals this failure.
         * @since 2.1.0
         */
        override fun equals(other: Any?): Boolean = other is Failure<*> && failure == other.failure

        /**
         * Returns a hash code derived from the failure, consistent with [equals].
         *
         * @return the hash code.
         * @since 2.1.0
         */
        override fun hashCode(): Int = failure.hashCode()

        /**
         * Returns the failure as text, such as `Failure(TooLong)`.
         *
         * The text of the failure is escaped and shortened to [Limits.MAX_DISPLAY_LENGTH] UTF-16 code units, so the
         * line is safe to log and to show on a terminal. A failure whose own `toString` reveals a secret reveals it
         * here as well.
         *
         * @return the failure as one line of text.
         * @since 2.1.0
         */
        override fun toString(): String = "Failure(${SafeDisplay.text(failure?.toString() ?: NULL_TEXT)})"
    }
}
