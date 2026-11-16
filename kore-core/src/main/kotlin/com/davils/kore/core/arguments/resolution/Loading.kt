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

import com.davils.kore.core.arguments.KoreParameterLimits
import com.davils.kore.core.arguments.KoreValueSource
import com.davils.kore.core.arguments.declaration.ParameterSpec
import com.davils.kore.core.arguments.lexer.ArgumentLexer
import com.davils.kore.core.arguments.lexer.LexResult
import com.davils.kore.core.arguments.lexer.RawValues
import com.davils.kore.core.arguments.lexer.lexedCount
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreValueOrigin
import com.davils.kore.core.arguments.report.renderUsage
import com.davils.kore.core.immutableList
import com.davils.kore.core.validation.violation.KoreSeverity

internal fun loadArguments(
    registry: ParameterRegistry,
    options: SchemaOptions,
    argv: List<String>,
    environment: Map<String, String>,
): LoadedArguments {
    val groups = registry.groupCheck()
    refuseOversized(registry, options.limits, argv)
    val lexed = ArgumentLexer(registry.specs, options).lex(argv)
    val external = environmentValues(registry.specs, lexed, environment)
    return settle(
        registry = registry,
        options = options,
        groups = groups,
        lexed = lexed,
        external = external,
        failures = emptyList(),
    )
}

internal suspend fun loadArguments(
    registry: ParameterRegistry,
    options: SchemaOptions,
    argv: List<String>,
    sources: List<KoreValueSource>,
    environment: Map<String, String>,
): LoadedArguments {
    val groups = registry.groupCheck()
    refuseOversized(registry, options.limits, argv)
    val lexed = ArgumentLexer(registry.specs, options).lex(argv)
    val external = environmentValues(registry.specs, lexed, environment)
    val failures = readSources(specs = registry.specs, lexed = lexed, values = external, sources = sources)
    return settle(
        registry = registry,
        options = options,
        groups = groups,
        lexed = lexed,
        external = external,
        failures = failures,
    )
}

private fun refuseOversized(registry: ParameterRegistry, limits: KoreParameterLimits, argv: List<String>) {
    val violations = oversized(limits, argv)
    if (violations.isEmpty()) return
    throw KoreParameterException(violations, renderUsage(registry.infos))
}

private fun oversized(limits: KoreParameterLimits, argv: List<String>): List<KoreParameterViolation> {
    if (!limits.admitsArguments(argv.size)) {
        return listOf(KoreParameterViolation.TooManyArguments(count = argv.size, limit = limits.maxArguments))
    }
    val lexed = argv.subList(fromIndex = 0, toIndex = lexedCount(argv))
    val positions = lexed.indices.filterNot { index -> limits.admitsLength(lexed[index]) }
    return positions.map { index ->
        KoreParameterViolation.ArgumentTooLong(position = index + 1, limit = limits.maxArgumentLength)
    }
}

private fun environmentValues(
    specs: List<ParameterSpec<*, *>>,
    lexed: LexResult,
    environment: Map<String, String>,
): MutableMap<ParameterSpec<*, *>, RawValues> {
    val values = mutableMapOf<ParameterSpec<*, *>, RawValues>()
    for (spec in specs.filterNot { spec -> spec in lexed.occurrences }) {
        environmentValue(spec, environment)?.let { value -> values[spec] = value }
    }
    return values
}

private fun environmentValue(spec: ParameterSpec<*, *>, environment: Map<String, String>): RawValues? {
    val variable = spec.info.environmentVariable ?: return null
    val raw = environment[variable]?.takeIf { value -> value.isNotEmpty() }
    return raw?.let { value -> RawValues(listOf(value), KoreValueOrigin.Environment(variable)) }
}

private suspend fun readSources(
    specs: List<ParameterSpec<*, *>>,
    lexed: LexResult,
    values: MutableMap<ParameterSpec<*, *>, RawValues>,
    sources: List<KoreValueSource>,
): List<KoreParameterViolation> {
    val failures = mutableListOf<KoreParameterViolation>()
    for ((index, source) in sources.withIndex()) {
        val pending = specs.filterNot { spec -> spec in lexed.occurrences || spec in values }
        val origin = catchingForeign { KoreValueOrigin.External(source.name) }
        origin.exceptionOrNull()?.let { cause ->
            pending.mapTo(failures) { spec ->
                KoreParameterViolation.SourceUnavailable(spec.info, unnamedSource(index), cause)
            }
        }
        origin.getOrNull()?.let { named ->
            readFrom(source = source, origin = named, pending = pending, values = values, failures = failures)
        }
    }
    return failures
}

private suspend fun readFrom(
    source: KoreValueSource,
    origin: KoreValueOrigin.External,
    pending: List<ParameterSpec<*, *>>,
    values: MutableMap<ParameterSpec<*, *>, RawValues>,
    failures: MutableList<KoreParameterViolation>,
) {
    for (spec in pending) {
        val result = catchingForeign { source.read(spec.info) }
        result.exceptionOrNull()?.let { cause ->
            failures += KoreParameterViolation.SourceUnavailable(spec.info, origin.sourceName, cause)
        }
        result.getOrNull()?.let { raw -> values[spec] = RawValues(listOf(raw), origin) }
    }
}

private fun unnamedSource(index: Int): String = "source ${index + 1}"

private fun settle(
    registry: ParameterRegistry,
    options: SchemaOptions,
    groups: GroupCheck,
    lexed: LexResult,
    external: Map<ParameterSpec<*, *>, RawValues>,
    failures: List<KoreParameterViolation>,
): LoadedArguments {
    val resolutions = registry.specs.map { spec ->
        spec.resolve(givenFor(spec, lexed, external), options.sensitiveOnCommandLine) to spec
    }
    val provided = resolutions.filter { (resolution, _) -> resolution.isProvided }.map { (_, spec) -> spec }
    val reported = lexed.violations + failures + resolutions.flatMap { (resolution, _) -> resolution.violations }
    val violations = (reported + groups.violations(provided.toSet())).immutableList()
    val assignments = resolutions.mapNotNull { (resolution, _) -> resolution.assignment }
    if (violations.any { violation -> violation.severity == KoreSeverity.Error }) {
        throw KoreParameterException(violations, renderUsage(registry.infos))
    }
    assignments.forEach { assignment -> assignment.apply() }
    return LoadedArguments(
        rest = lexed.rest,
        unknown = lexed.unknown,
        unknownNames = lexed.unknownNames,
        warnings = violations,
    )
}

private fun givenFor(
    spec: ParameterSpec<*, *>,
    lexed: LexResult,
    external: Map<ParameterSpec<*, *>, RawValues>,
): RawValues? = lexed.occurrences[spec] ?: external[spec]
