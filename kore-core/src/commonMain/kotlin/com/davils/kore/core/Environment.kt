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

internal expect fun platformEnvironment(): Map<String, String>
