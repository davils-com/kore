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

import com.davils.kore.core.arguments.declaration.ParameterSpec

internal class ParameterGroups(private val schemaName: String) {
    private val exclusiveGroups: MutableList<List<String>> = mutableListOf()
    private val dependencies: MutableList<Pair<String, List<String>>> = mutableListOf()

    fun exclusive(propertyNames: List<String>) {
        exclusiveGroups += propertyNames
    }

    fun requires(propertyName: String, required: List<String>) {
        dependencies += propertyName to required
    }

    fun resolve(specs: List<ParameterSpec<*, *>>): GroupCheck {
        val byName = specs.associateBy { spec -> spec.propertyName }
        val references = references()
        val missing = references.filterNot { name -> name in byName }.distinct()
        require(missing.isEmpty()) {
            "$schemaName declares a parameter group naming ${missing.joinToString(", ") { "'$it'" }}, which is " +
                "not a declared parameter. Group members are properties declared with optional(), required(), " +
                "defaulted(), multiple() or flag() on this schema."
        }
        return GroupCheck(
            exclusive = exclusiveGroups.map { group -> group.map(byName::getValue) },
            dependencies = dependencies.map { (name, required) ->
                byName.getValue(
                    name,
                ) to required.map(byName::getValue)
            },
        )
    }

    private fun references(): List<String> {
        val dependent = dependencies.flatMap { (name, required) -> listOf(name) + required }
        return exclusiveGroups.flatten() + dependent
    }
}
