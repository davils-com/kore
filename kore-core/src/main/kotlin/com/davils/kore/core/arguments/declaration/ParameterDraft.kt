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

import com.davils.kore.core.arguments.KoreParameterInfo
import com.davils.kore.core.arguments.KoreParameterLimits
import com.davils.kore.core.arguments.report.ParameterPresentation
import com.davils.kore.core.arguments.resolution.ParameterSlot
import com.davils.kore.core.arguments.resolution.ValueReading

internal class ParameterDraft<T : Any, V>(
    val names: List<String>,
    val traits: ParameterTraits,
    private val rules: ValueRules<T>,
    private val arity: Arity<T, V>,
) {
    fun infoFor(environmentVariable: String?): KoreParameterInfo = KoreParameterInfo(
        names = names,
        environmentVariable = environmentVariable,
        typeName = rules.converter.typeName,
        kind = arity.kind,
        presentation = ParameterPresentation(
            isSensitive = traits.isSensitive,
            defaultDisplay = arity.defaultDisplay(traits.isSensitive),
            description = traits.description,
            deprecation = traits.deprecation,
        ),
    )

    fun toSpec(info: KoreParameterInfo, propertyName: String, limits: KoreParameterLimits): ParameterSpec<T, V> =
        ParameterSpec(
            info = info,
            propertyName = propertyName,
            negatedNames = negatedNamesOf(names, traits.isNegatable),
            reading = ValueReading(info, rules, limits),
            arity = arity,
            slot = ParameterSlot(),
        )
}
