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
 * Transforms the value of a success with [transform] and keeps a failure as it is.
 *
 * @param T the type of the value of this outcome.
 * @param F the type of the failure.
 * @param R the type of the transformed value.
 * @param transform turns the value into a new value; called only for a success.
 * @return a success with the transformed value, or this failure.
 * @since 2.1.0
 */
public inline fun <T, F, R> Outcome<T, F>.map(transform: (T) -> R): Outcome<R, F> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

/**
 * Transforms the failure of a failed outcome with [transform] and keeps a success as it is.
 *
 * @param T the type of the value.
 * @param F the type of the failure of this outcome.
 * @param G the type of the transformed failure.
 * @param transform turns the failure into a new failure; called only for a failure.
 * @return a failure with the transformed failure, or this success.
 * @since 2.1.0
 */
public inline fun <T, F, G> Outcome<T, F>.mapFailure(transform: (F) -> G): Outcome<T, G> = when (this) {
    is Outcome.Success -> this
    is Outcome.Failure -> Outcome.Failure(transform(failure))
}

/**
 * Continues a success with the next operation [transform] and keeps a failure as it is.
 *
 * @param T the type of the value of this outcome.
 * @param F the type of the failure, shared by both operations.
 * @param R the type of the value of the next operation.
 * @param transform the next operation, given the value; called only for a success.
 * @return the outcome of [transform], or this failure.
 * @since 2.1.0
 */
public inline fun <T, F, R> Outcome<T, F>.flatMap(transform: (T) -> Outcome<R, F>): Outcome<R, F> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

/**
 * Turns a failure into a success with the value [transform] gives for it and keeps a success as it is.
 *
 * @param T the type of the value.
 * @param F the type of the failure.
 * @param transform gives a value in place of the failure; called only for a failure.
 * @return a success, either this one or one with the value [transform] gave.
 * @since 2.1.0
 */
public inline fun <T, F> Outcome<T, F>.recover(transform: (F) -> T): Outcome<T, Nothing> = when (this) {
    is Outcome.Success -> this
    is Outcome.Failure -> Outcome.Success(transform(failure))
}

/**
 * Turns this [Result] into an outcome whose failure is the throwable of a failed result.
 *
 * @param T the type of the value.
 * @return a success with the value of this result, or a failure with its throwable.
 * @since 2.1.0
 */
public fun <T> Result<T>.toOutcome(): Outcome<T, Throwable> = fold(
    onSuccess = { value -> Outcome.Success(value) },
    onFailure = { failure -> Outcome.Failure(failure) },
)
