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

package com.davils.kore.core

/**
 * Returns the environment variables of the running process, as a snapshot that no caller can change and whose
 * `toString` shows only the names.
 *
 * On the JVM the variables are those of `System.getenv`. On Wasm they are those of `process.env` under Node.js, and
 * there are none in a browser. The map is taken once, when the function is called, so later changes of the
 * environment do not show in it, and it is safe to share between threads.
 *
 * Environment variables often hold secrets, such as tokens and passwords. Printing `System.getenv()` reveals every one
 * of them, so the `toString` of this map, and of each of its entries, shows the names, in alphabetical order, and
 * `****` in place of every value. A value read with `get` is returned as it is, and whoever shows it keeps it out of
 * logs.
 *
 * @return the environment variables by name.
 * @since 2.1.0
 */
public fun processEnvironment(): Map<String, String> = MaskedEnvironment(platformEnvironment())

/**
 * Returns the current value of the environment variable [name] of the running process, or `null` if it is not set.
 *
 * Unlike [processEnvironment], it copies nothing but the one value, so it suits code that reads a few variables. On the
 * JVM it reads `System.getenv(name)`, which holds the environment the process started with; on Wasm it reads
 * `process.env` under Node.js on every call, so a change made there shows, and in a browser it always returns `null`.
 * It is safe to call from any thread.
 *
 * A name that is empty, longer than [Limits.MAX_NAME_LENGTH] or holds a NUL character is refused before the
 * environment is read. Any other name is looked up as it is, so a name with `=`, which only Windows uses for its hidden
 * variables such as `=C:`, returns `null` wherever no such variable is set. The value is returned as it is; it may be a
 * secret, so whoever shows it keeps it out of logs.
 *
 * @param name the name of the variable, case-sensitive on every platform but Windows.
 * @return the value, or `null` if the variable is not set.
 * @throws IllegalArgumentException if [name] is empty, too long or holds a NUL character. The message never repeats
 * the name.
 * @since 2.4.0
 */
public fun processVariable(name: String): String? {
    require(isVariableName(name)) {
        "The name of an environment variable must be 1 to ${Limits.MAX_NAME_LENGTH} characters without NUL"
    }
    return platformVariable(name)
}

/**
 * Returns the names of the environment variables of the running process, without any of their values.
 *
 * It suits code that looks for variables by a prefix, such as `APP_`, and then reads the few it needs with
 * [processVariable], so no value it does not need is ever copied. On the JVM the names are those of `System.getenv`; on
 * Wasm they are the own properties of `process.env` under Node.js, and there are none in a browser. The list is a
 * snapshot taken when the function is called, sorted by code unit, free of duplicates, unchangeable by a caller and
 * safe to share between threads.
 *
 * @return the names, in ascending order.
 * @since 2.5.0
 */
public fun processVariableNames(): List<String> {
    val names = platformVariableNames().distinct()
    return names.sorted().immutableList()
}

private fun isVariableName(name: String): Boolean {
    val isBounded = name.length in 1..Limits.MAX_NAME_LENGTH
    return isBounded && name.none { character -> character == '\u0000' }
}

internal expect fun platformEnvironment(): Map<String, String>

internal expect fun platformVariable(name: String): String?

internal expect fun platformVariableNames(): Collection<String>
