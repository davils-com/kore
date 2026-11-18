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

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val ALREADY_LOADING_MESSAGE: String = "This schema was already loaded, or is being loaded on another " +
    "thread. Construct a new instance instead of loading twice."

@OptIn(ExperimentalAtomicApi::class)
internal class LoadGate {
    private val loading: AtomicBoolean = AtomicBoolean(false)

    @Volatile
    var loaded: LoadedArguments? = null
        private set

    val isStarted: Boolean get() = loading.load()

    fun begin() {
        check(loading.compareAndSet(expectedValue = false, newValue = true)) { ALREADY_LOADING_MESSAGE }
    }

    fun abandon() {
        loading.store(false)
    }

    fun publish(arguments: LoadedArguments) {
        loaded = arguments
    }
}

internal inline fun LoadGate.once(load: () -> LoadedArguments) {
    begin()
    val loaded = runCatching(load)
    loaded.onFailure { abandon() }
    publish(loaded.getOrThrow())
}
