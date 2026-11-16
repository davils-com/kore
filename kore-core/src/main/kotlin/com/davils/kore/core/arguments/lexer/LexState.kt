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

package com.davils.kore.core.arguments.lexer

import com.davils.kore.core.arguments.KoreUnknownArgumentPolicy
import com.davils.kore.core.arguments.declaration.ParameterSpec
import com.davils.kore.core.arguments.declaration.readOnlyCopy
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.closestName
import com.davils.kore.core.arguments.resolution.SchemaOptions

internal class LexState(private val knownNames: Set<String>, private val options: SchemaOptions) {
    private val occurrences: MutableMap<ParameterSpec<*, *>, Occurrences> = mutableMapOf()
    private val unknown: MutableList<String> = mutableListOf()
    private val unknownNames: MutableList<String> = mutableListOf()
    private val violations: MutableList<KoreParameterViolation> = mutableListOf()
    private var suggestionsLeft: Int = options.limits.maxSuggestedArguments
    private val isCollecting: Boolean get() = options.unknownArguments == KoreUnknownArgumentPolicy.Collect

    fun record(spec: ParameterSpec<*, *>, raw: String) {
        val given = occurrences.getOrPut(spec) { Occurrences(options.limits) }
        given.add(raw)
    }

    fun reject(name: String, token: String) {
        if (isCollecting) {
            collect(token, name)
            return
        }
        violations += KoreParameterViolation.UnknownArgument(
            token = name,
            suggestion = suggestionFor(name),
            bundlePosition = null,
        )
    }

    fun rejectBundle(letter: String, token: String, position: Int) {
        if (isCollecting) {
            collect(token, letter)
            return
        }
        val bundlePosition = position.takeIf { letter.length != token.length }
        violations += KoreParameterViolation.UnknownArgument(
            token = letter,
            suggestion = null,
            bundlePosition = bundlePosition,
        )
    }

    fun rejectValue(token: String, position: Int) {
        if (isCollecting) {
            collect(token, name = null)
            return
        }
        violations += KoreParameterViolation.StrayValue(position)
    }

    fun malformed(token: String, reason: String) {
        violations += KoreParameterViolation.MalformedArgument(token, reason)
    }

    fun missingValue(spec: ParameterSpec<*, *>, token: String) {
        violations += KoreParameterViolation.MissingValue(spec.info, token)
    }

    fun result(rest: List<String>): LexResult = LexResult(
        occurrences = occurrences.mapValues { (_, given) -> given.rawValues() },
        unknown = readOnlyCopy(unknown),
        unknownNames = readOnlyCopy(unknownNames),
        rest = readOnlyCopy(rest),
        violations = readOnlyCopy(violations),
    )

    private fun collect(token: String, name: String?) {
        unknown += token
        name?.let { collected -> unknownNames += collected }
    }

    private fun suggestionFor(name: String): String? {
        if (suggestionsLeft <= 0) return null
        suggestionsLeft--
        return closestName(name, knownNames)
    }
}
