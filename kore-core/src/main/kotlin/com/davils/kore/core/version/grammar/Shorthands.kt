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

import com.davils.kore.core.version.KoreSemanticVersion
import com.davils.kore.core.version.interval.EVERYTHING
import com.davils.kore.core.version.interval.KoreVersionInterval
import com.davils.kore.core.version.interval.floorAbove
import com.davils.kore.core.version.interval.interval

private const val TILDE_PREFIX: Int = 2

internal fun tildeInterval(partial: Partial): KoreVersionInterval? {
    if (partial.isAny) return EVERYTHING
    return interval(start = partial.lowest(), end = floorAbove(partial.numbers.take(TILDE_PREFIX)))
}

internal fun caretInterval(partial: Partial): KoreVersionInterval? {
    if (partial.isAny) return EVERYTHING
    return interval(start = partial.lowest(), end = floorAbove(caretPrefix(partial.numbers)))
}

internal fun hyphenInterval(from: Partial, to: Partial): KoreVersionInterval? =
    interval(start = lowestOf(from), end = firstAboveOf(to))

private fun lowestOf(partial: Partial): KoreSemanticVersion? {
    if (partial.isAny) return null
    return partial.lowest()
}

private fun firstAboveOf(partial: Partial): KoreSemanticVersion? {
    if (partial.isAny) return null
    return partial.firstAbove()
}

private fun caretPrefix(numbers: List<Int>): List<Int> {
    val firstNonZero = numbers.indexOfFirst { number -> number != 0 }
    if (firstNonZero < 0) return numbers
    return numbers.take(firstNonZero + 1)
}
