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

/**
 * Returns what [onSuccess] gives for the value of a success or what [onFailure] gives for the failure of a failure.
 *
 * @param T the type of the value.
 * @param F the type of the failure.
 * @param R the type of the result.
 * @param onSuccess gives the result for the value; called only for a success.
 * @param onFailure gives the result for the failure; called only for a failure.
 * @return the result of whichever function was called.
 * @since 2.1.0
 */
public inline fun <T, F, R> Outcome<T, F>.fold(onSuccess: (T) -> R, onFailure: (F) -> R): R = when (this) {
    is Outcome.Success -> onSuccess(value)
    is Outcome.Failure -> onFailure(failure)
}

/**
 * Runs [action] with the value of a success and returns this outcome unchanged.
 *
 * @param T the type of the value.
 * @param F the type of the failure.
 * @param action what to do with the value; called only for a success.
 * @return this outcome.
 * @since 2.1.0
 */
public inline fun <T, F> Outcome<T, F>.onSuccess(action: (T) -> Unit): Outcome<T, F> {
    if (this is Outcome.Success) action(value)
    return this
}

/**
 * Runs [action] with the failure of a failed outcome and returns this outcome unchanged.
 *
 * @param T the type of the value.
 * @param F the type of the failure.
 * @param action what to do with the failure; called only for a failure.
 * @return this outcome.
 * @since 2.1.0
 */
public inline fun <T, F> Outcome<T, F>.onFailure(action: (F) -> Unit): Outcome<T, F> {
    if (this is Outcome.Failure) action(failure)
    return this
}

/**
 * Returns the value of a success, or what [fallback] gives for the failure of a failure.
 *
 * @param T the type of the value.
 * @param F the type of the failure.
 * @param fallback gives a value in place of the failure; called only for a failure.
 * @return the value of this success or the value [fallback] gave.
 * @since 2.1.0
 */
public inline fun <T, F> Outcome<T, F>.getOrElse(fallback: (F) -> T): T = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> fallback(failure)
}

/**
 * Returns the value of a success, or `null` for a failure.
 *
 * A success whose value is `null` also returns `null`; [Outcome.isSuccess] tells both apart.
 *
 * @param T the type of the value.
 * @return the value of this success, or `null`.
 * @since 2.1.0
 */
public fun <T> Outcome<T, *>.getOrNull(): T? = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> null
}

/**
 * Returns the failure of a failed outcome, or `null` for a success.
 *
 * @param F the type of the failure.
 * @return the failure of this failure, or `null`.
 * @since 2.1.0
 */
public fun <F> Outcome<*, F>.failureOrNull(): F? = when (this) {
    is Outcome.Success -> null
    is Outcome.Failure -> failure
}

/**
 * Returns the value of a success, or throws the throwable of a failure as it is.
 *
 * @param T the type of the value.
 * @return the value of this success.
 * @throws Throwable the failure of this outcome, unchanged.
 * @since 2.1.0
 */
public fun <T> Outcome<T, Throwable>.getOrThrow(): T = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> throw failure
}
