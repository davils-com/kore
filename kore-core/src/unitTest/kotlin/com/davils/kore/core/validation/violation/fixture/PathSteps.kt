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

package com.davils.kore.core.validation.violation.fixture

import com.davils.kore.core.validation.violation.KoreViolationPath

private val token = Regex("""\["([^"]*)"]|\[(\d+)]|([^.\[]+)""")

internal fun pathOf(steps: List<PathStep>): KoreViolationPath =
    steps.fold(KoreViolationPath.ROOT) { path, step -> step.appendTo(path) }

internal fun stepsOf(rendered: String): List<PathStep> {
    val matches = token.findAll(rendered)
    return matches.map(::stepOf).toList()
}

private fun stepOf(match: MatchResult): PathStep {
    val (key, index, name) = match.destructured
    val candidates = listOfNotNull(
        key.takeIf { it.isNotEmpty() }?.let(PathStep::Keyed),
        index.takeIf { it.isNotEmpty() }?.let { PathStep.At(it.toInt()) },
        name.takeIf { it.isNotEmpty() }?.let(PathStep::Named),
    )
    return candidates.single()
}
