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

package com.davils.kore.core.arguments.report

import kotlin.math.abs

private const val MAXIMUM_DISTANCE: Int = 3
private const val DISTANCE_RATIO: Int = 3
private const val LONGEST_WORTH_COMPARING: Int = 64

internal fun closestName(token: String, candidates: Collection<String>): String? {
    val limit = minOf(a = MAXIMUM_DISTANCE, b = token.length / DISTANCE_RATIO)
    if (token.length > LONGEST_WORTH_COMPARING) return null
    var best: String? = null
    var bestDistance = limit + 1
    for (candidate in candidates) {
        if (abs(token.length - candidate.length) >= bestDistance) continue
        val distance = boundedDistance(left = token, right = candidate, cutoff = bestDistance)
        if (distance < bestDistance) {
            bestDistance = distance
            best = candidate
        }
    }
    return best
}

internal fun boundedDistance(left: String, right: String, cutoff: Int): Int {
    var previous = IntArray(right.length + 1) { it }
    var current = IntArray(right.length + 1)
    for (row in 1..left.length) {
        current[0] = row
        var rowMinimum = row
        for (column in 1..right.length) {
            val substitution = previous[column - 1] + substitutionCost(left = left[row - 1], right = right[column - 1])
            current[column] = minOf(a = substitution, b = previous[column] + 1, c = current[column - 1] + 1)
            rowMinimum = minOf(a = rowMinimum, b = current[column])
        }
        if (rowMinimum >= cutoff) return cutoff
        val swap = previous
        previous = current
        current = swap
    }
    return previous[right.length]
}

private fun substitutionCost(left: Char, right: Char): Int {
    if (left == right) return 0
    return 1
}
