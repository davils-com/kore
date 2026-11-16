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
package com.davils.kore.core.arguments.lexer.fixture

import com.davils.kore.core.arguments.KoreParameterLimits
import com.davils.kore.core.arguments.KoreUnknownArgumentPolicy
import com.davils.kore.core.arguments.conversion.flagConverter
import com.davils.kore.core.arguments.declaration.Arity
import com.davils.kore.core.arguments.declaration.KoreFlagConfiguration
import com.davils.kore.core.arguments.declaration.ParameterDraft
import com.davils.kore.core.arguments.declaration.ValueRules
import com.davils.kore.core.arguments.lexer.ArgumentLexer
import com.davils.kore.core.arguments.lexer.RawValues
import com.davils.kore.core.arguments.resolution.ParameterRegistry
import com.davils.kore.core.arguments.resolution.SchemaOptions
import com.davils.kore.core.validation.violation.KoreSeverity

internal fun lexedFlag(limits: KoreParameterLimits, argv: List<String>): RawValues {
    val registry = ParameterRegistry(schemaName = "Flags", environmentPrefix = null, limits = limits)
    val draft = ParameterDraft(
        names = listOf("--verbose", "-v"),
        traits = KoreFlagConfiguration().flagTraits(),
        rules = ValueRules(converter = flagConverter, validators = emptyList(), splitDelimiter = null),
        arity = Arity.Flag(isOnByDefault = false),
    )
    registry.register(draft, "isVerbose")
    val options = SchemaOptions(
        unknownArguments = KoreUnknownArgumentPolicy.Reject,
        limits = limits,
        sensitiveOnCommandLine = KoreSeverity.Warning,
    )
    val lexed = ArgumentLexer(registry.specs, options).lex(argv)
    return lexed.occurrences.values.single()
}
