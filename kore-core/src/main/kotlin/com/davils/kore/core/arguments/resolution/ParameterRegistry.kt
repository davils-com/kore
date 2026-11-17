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

import com.davils.kore.core.arguments.ParameterInfo
import com.davils.kore.core.arguments.ParameterLimits
import com.davils.kore.core.arguments.declaration.ParameterDraft
import com.davils.kore.core.arguments.declaration.ParameterSpec
import com.davils.kore.core.text.SafeDisplay

private val variableName: Regex = Regex("[A-Za-z_][A-Za-z0-9_]*")

internal class ParameterRegistry(
    private val schemaName: String,
    private val environmentPrefix: String?,
    private val limits: ParameterLimits,
) {
    private val declared: MutableList<ParameterSpec<*, *>> = mutableListOf()
    private val claimedNames: MutableSet<String> = mutableSetOf()
    private val claimedVariables: MutableMap<String, String> = mutableMapOf()
    private val groups: ParameterGroups = ParameterGroups(schemaName)

    val specs: List<ParameterSpec<*, *>> get() = declared.toList()

    val infos: List<ParameterInfo> get() = declared.map { spec -> spec.info }

    fun <V> register(draft: ParameterDraft<*, V>, propertyName: String): ParameterSpec<*, V> {
        val variable = draft.traits.environment.variableFor(draft.names, environmentPrefix)
        val spec = draft.toSpec(draft.infoFor(variable), propertyName, limits)
        spec.matchableNames.forEach(::claimName)
        variable?.let { claimed -> claimVariable(propertyName, claimed) }
        declared += spec
        return spec
    }

    fun exclusive(propertyNames: List<String>) {
        groups.exclusive(propertyNames)
    }

    fun requires(propertyName: String, required: List<String>) {
        groups.requires(propertyName, required)
    }

    fun groupCheck(): GroupCheck = groups.resolve(declared)

    private fun claimName(name: String) {
        require(claimedNames.add(name)) { "Parameter name '$name' is declared more than once in $schemaName" }
    }

    private fun claimVariable(propertyName: String, variable: String) {
        require(variableName.matches(variable)) {
            "Parameter '$propertyName' reads the environment variable ${SafeDisplay.quoted(variable)}, which is " +
                "not a well-formed variable name"
        }
        val previous = claimedVariables.put(key = variable, value = propertyName) ?: return
        throw IllegalArgumentException(
            "Parameters '$previous' and '$propertyName' both read the environment variable '$variable'. " +
                "A variable belongs to one parameter; use env() or noEnv() to separate them.",
        )
    }
}
