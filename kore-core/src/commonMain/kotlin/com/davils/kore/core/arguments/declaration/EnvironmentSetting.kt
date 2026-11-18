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

package com.davils.kore.core.arguments.declaration

private const val DASH: Char = '-'
private const val UNDERSCORE: Char = '_'

internal sealed interface EnvironmentSetting {
    fun variableFor(names: List<String>, prefix: String?): String?

    data object Derived : EnvironmentSetting {
        override fun variableFor(names: List<String>, prefix: String?): String? {
            val longName = names.firstOrNull { name -> name.startsWith(LONG_PREFIX) }
            if (prefix == null || longName == null) return null
            val body = longName.removePrefix(LONG_PREFIX).replace(oldChar = DASH, newChar = UNDERSCORE)
            return "${prefix.trimEnd(UNDERSCORE)}$UNDERSCORE${body.uppercase()}"
        }
    }

    data class Explicit(val variable: String) : EnvironmentSetting {
        override fun variableFor(names: List<String>, prefix: String?): String = variable
    }

    data object Disabled : EnvironmentSetting {
        override fun variableFor(names: List<String>, prefix: String?): String? = null
    }
}
