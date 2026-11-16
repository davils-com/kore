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

import kotlin.coroutines.cancellation.CancellationException

internal inline fun <R> catchingForeign(block: () -> R): Result<R> {
    val result = runCatching(block)
    result.exceptionOrNull()?.let(::rethrowUncontainable)
    return result
}

internal fun rethrowUncontainable(failure: Throwable) {
    if (failure is InterruptedException) Thread.currentThread().interrupt()
    val isUncontainable = failure is InterruptedException ||
        failure is CancellationException ||
        failure is VirtualMachineError
    if (isUncontainable) throw failure
}
