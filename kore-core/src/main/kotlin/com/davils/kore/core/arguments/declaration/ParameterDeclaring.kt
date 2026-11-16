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

import com.davils.kore.core.arguments.KoreParameter
import com.davils.kore.core.arguments.KoreParameterDeclarations
import com.davils.kore.core.arguments.conversion.KoreConverter
import com.davils.kore.core.arguments.conversion.flagConverter

internal fun <T : Any, V> valueParameter(
    owner: KoreParameterDeclarations,
    names: Array<out String>,
    type: KoreConverter<T>,
    configure: KoreValueConfiguration<T>.() -> Unit,
    arity: Arity<T, V>,
): KoreParameter<V> {
    val declared = checkedNames(names)
    val configuration = KoreValueConfiguration<T>()
    configuration.configure()
    val rules = ValueRules(converter = type, validators = configuration.validators, splitDelimiter = null)
    val draft = ParameterDraft(names = declared, traits = configuration.valueTraits(), rules = rules, arity = arity)
    return KoreParameter(draft, owner)
}

internal fun <T : Any> multipleParameter(
    owner: KoreParameterDeclarations,
    names: Array<out String>,
    type: KoreConverter<T>,
    configure: KoreMultipleConfiguration<T>.() -> Unit,
    defaults: List<T>,
): KoreParameter<List<T>> {
    val declared = checkedNames(names)
    val configuration = KoreMultipleConfiguration<T>()
    configuration.configure()
    val rules = ValueRules(
        converter = type,
        validators = configuration.validators,
        splitDelimiter = configuration.splitDelimiter,
    )
    val draft = ParameterDraft(
        names = declared,
        traits = configuration.valueTraits(),
        rules = rules,
        arity = Arity.Multiple(readOnlyCopy(defaults)),
    )
    return KoreParameter(draft, owner)
}

internal fun flagParameter(
    owner: KoreParameterDeclarations,
    names: Array<out String>,
    default: Boolean,
    configure: KoreFlagConfiguration.() -> Unit,
): KoreParameter<Boolean> {
    val declared = checkedNames(names)
    val configuration = KoreFlagConfiguration()
    configuration.configure()
    val rules = ValueRules(converter = flagConverter, validators = emptyList(), splitDelimiter = null)
    val draft = ParameterDraft(
        names = declared,
        traits = configuration.flagTraits(),
        rules = rules,
        arity = Arity.Flag(default),
    )
    return KoreParameter(draft, owner)
}
