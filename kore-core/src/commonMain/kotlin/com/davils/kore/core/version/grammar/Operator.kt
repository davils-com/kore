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

package com.davils.kore.core.version.grammar

import com.davils.kore.core.version.interval.EVERYTHING
import com.davils.kore.core.version.interval.VersionInterval
import com.davils.kore.core.version.interval.floorAbove
import com.davils.kore.core.version.interval.interval
import com.davils.kore.core.version.interval.preReleaseFloor

internal enum class Operator(internal val symbol: String) {
    GreaterOrEqual(">="),
    LessOrEqual("<="),
    Greater(">"),
    Less("<"),
    Equal("="),
    Tilde("~"),
    Caret("^"),
}

internal fun intervalFor(operator: Operator, partial: Partial): VersionInterval? = when (operator) {
    Operator.Equal -> exactInterval(partial)
    Operator.GreaterOrEqual -> atLeastInterval(partial)
    Operator.Greater -> aboveInterval(partial)
    Operator.Less -> belowInterval(partial)
    Operator.LessOrEqual -> atMostInterval(partial)
    Operator.Tilde -> tildeInterval(partial)
    Operator.Caret -> caretInterval(partial)
}

private fun exactInterval(partial: Partial): VersionInterval? {
    if (partial.isAny) return EVERYTHING
    return interval(start = partial.lowest(), end = partial.firstAbove())
}

private fun atLeastInterval(partial: Partial): VersionInterval? {
    if (partial.isAny) return EVERYTHING
    return interval(start = partial.lowest(), end = null)
}

private fun aboveInterval(partial: Partial): VersionInterval? {
    val complete = partial.version ?: return aboveLine(partial)
    return interval(start = complete, end = null, isStartExclusive = true)
}

private fun aboveLine(partial: Partial): VersionInterval? {
    if (partial.isAny) return null
    return floorAbove(partial.numbers)?.let { next -> interval(start = next.release(), end = null) }
}

private fun belowInterval(partial: Partial): VersionInterval? {
    if (partial.isAny) return null
    return interval(start = null, end = partial.version ?: preReleaseFloor(partial.numbers))
}

private fun atMostInterval(partial: Partial): VersionInterval? {
    if (partial.isAny) return EVERYTHING
    return interval(start = null, end = partial.firstAbove())
}
