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

import com.davils.kore.core.text.internal.SafeDisplay

internal class MaskedEnvironment(source: Map<String, String>) : AbstractMap<String, String>() {
    private val variables: Map<String, String> = source.immutableMap()

    override val entries: Set<Map.Entry<String, String>> =
        variables.map { (name, value) -> MaskedEntry(key = name, value = value) }.immutableSet()

    override fun get(key: String): String? = variables[key]

    override fun containsKey(key: String): Boolean = key in variables

    override fun toString(): String {
        val shown = variables.keys.sorted().map { name -> "${SafeDisplay.text(name)}=${SafeDisplay.REDACTED}" }
        return shown.joinToString(separator = ", ", prefix = "{", postfix = "}")
    }
}
