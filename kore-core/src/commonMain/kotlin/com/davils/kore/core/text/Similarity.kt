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

package com.davils.kore.core.text

import com.davils.kore.core.Limits
import com.davils.kore.core.immutableList
import com.davils.kore.core.text.internal.charCountOf
import com.davils.kore.core.text.internal.codePointAt
import kotlin.math.abs

private const val MOST_SUGGESTED_DISTANCE: Int = 3
private const val LENGTH_PER_EDIT: Int = 3

/**
 * Counts the edits that turn [left] into [right], up to [limit]: the Levenshtein distance, in which inserting,
 * deleting or replacing one character is one edit.
 *
 * Characters are code points, so a character outside the Basic Multilingual Plane, such as an emoji, counts once,
 * and a lone surrogate counts as a character of its own. Characters are compared exactly, case included; nothing is
 * normalised. The work grows with the product of both lengths at most and stops as soon as the distance is known to
 * exceed [limit]. The function is pure and safe to call from any thread.
 *
 * @param left the first text.
 * @param right the second text.
 * @param limit the largest distance that matters, zero or more.
 * @return the distance if it is at most [limit], and `limit + 1` otherwise.
 * @throws IllegalArgumentException if [limit] is negative, or if [left] or [right] is longer than
 * [Limits.MAX_EDIT_DISTANCE_LENGTH] UTF-16 code units; the text is refused before any work is done.
 * @since 2.1.0
 */
public fun editDistance(left: CharSequence, right: CharSequence, limit: Int): Int {
    require(limit >= 0) { "The limit of an edit distance cannot be negative, but was $limit" }
    require(left.length <= Limits.MAX_EDIT_DISTANCE_LENGTH && right.length <= Limits.MAX_EDIT_DISTANCE_LENGTH) {
        "An edit distance is measured between texts of at most ${Limits.MAX_EDIT_DISTANCE_LENGTH} characters"
    }
    return boundedDistance(left = codePointsOf(left), right = codePointsOf(right), limit = limit)
}

/**
 * Returns the candidate closest to [token], for a hint such as "did you mean `--port`?", or `null` if none is close
 * enough.
 *
 * A candidate is close enough when its [editDistance] to [token] is at most [maxDistance]; the closest one wins, and
 * of several equally close ones the first in the order of [candidates]. By default a token may be off by one edit
 * for every three characters, at most three edits, so a short token gets no suggestion that has little in common
 * with it. With [ignoreCase] the letters `A` to `Z` count as `a` to `z`; no other character is folded, so the result
 * is the same on every platform and in every locale.
 *
 * A token longer than [Limits.MAX_SUGGESTION_TOKEN_LENGTH] gets no suggestion, and a candidate longer than
 * [Limits.MAX_EDIT_DISTANCE_LENGTH] is never suggested; neither is measured. The work grows with the number of
 * candidates, so a caller that suggests from candidates of foreign origin bounds their number first. The candidate is
 * returned as it is: whoever shows it escapes it, for example through [SafeText].
 *
 * @param token the text that matched nothing, such as an unknown option.
 * @param candidates the texts that would have matched.
 * @param maxDistance the most edits a suggestion may be away from [token], zero or more.
 * @param ignoreCase whether ASCII letters match regardless of case.
 * @return the closest candidate, or `null` if none is within [maxDistance].
 * @throws IllegalArgumentException if [maxDistance] is negative.
 * @since 2.1.0
 */
public fun closestMatch(
    token: String,
    candidates: Iterable<String>,
    maxDistance: Int = suggestedDistanceFor(token),
    ignoreCase: Boolean = false,
): String? {
    val matches = closeMatches(
        token = token,
        candidates = candidates,
        maxCount = 1,
        maxDistance = maxDistance,
        ignoreCase = ignoreCase,
    )
    return matches.firstOrNull()
}

/**
 * Returns up to [maxCount] candidates close to [token], the closest first, for a hint such as "did you mean `port`,
 * `ports` or `sort`?".
 *
 * It measures every candidate as [closestMatch] does and with the same limits. The result is ordered by distance and,
 * among equally close candidates, by the order of [candidates], and holds every distinct candidate once.
 *
 * @param token the text that matched nothing.
 * @param candidates the texts that would have matched.
 * @param maxCount the most candidates to return, zero or more.
 * @param maxDistance the most edits a suggestion may be away from [token], zero or more.
 * @param ignoreCase whether ASCII letters match regardless of case.
 * @return the close candidates, the closest first, as a list that no caller can change; empty if none is close.
 * @throws IllegalArgumentException if [maxCount] or [maxDistance] is negative.
 * @since 2.1.0
 */
public fun closeMatches(
    token: String,
    candidates: Iterable<String>,
    maxCount: Int,
    maxDistance: Int = suggestedDistanceFor(token),
    ignoreCase: Boolean = false,
): List<String> {
    require(maxCount >= 0) { "The number of suggestions cannot be negative, but was $maxCount" }
    require(maxDistance >= 0) { "The distance of a suggestion cannot be negative, but was $maxDistance" }
    if (token.length > Limits.MAX_SUGGESTION_TOKEN_LENGTH || maxCount == 0) return emptyList()
    val target = codePointsOf(folded(token, ignoreCase))
    val measurable = candidates.filter { candidate -> candidate.length <= Limits.MAX_EDIT_DISTANCE_LENGTH }
    val close = measurable.distinct().mapNotNull { candidate ->
        rated(candidate = candidate, target = target, maxDistance = maxDistance, ignoreCase = ignoreCase)
    }
    val ranked = close.sortedBy { (_, distance) -> distance }
    val chosen = ranked.take(maxCount)
    return chosen.map { (candidate, _) -> candidate }.immutableList()
}

private fun rated(candidate: String, target: IntArray, maxDistance: Int, ignoreCase: Boolean): Pair<String, Int>? {
    val measured = codePointsOf(folded(candidate, ignoreCase))
    val distance = boundedDistance(left = target, right = measured, limit = maxDistance)
    if (distance > maxDistance) return null
    return candidate to distance
}

private fun suggestedDistanceFor(token: String): Int =
    minOf(a = MOST_SUGGESTED_DISTANCE, b = token.length / LENGTH_PER_EDIT)

private fun folded(text: String, ignoreCase: Boolean): String {
    if (!ignoreCase) return text
    return asciiLowercase(text)
}

private fun codePointsOf(text: CharSequence): IntArray {
    val codePoints = IntArray(text.length)
    var count = 0
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        codePoints[count] = codePoint
        count += 1
        index += charCountOf(codePoint)
    }
    return codePoints.copyOf(count)
}

private fun boundedDistance(left: IntArray, right: IntArray, limit: Int): Int {
    if (abs(left.size - right.size) > limit) return limit + 1
    return minOf(a = distanceWithin(left = left, right = right, limit = limit), b = limit + 1)
}

private fun distanceWithin(left: IntArray, right: IntArray, limit: Int): Int {
    var previous = IntArray(right.size + 1) { column -> column }
    var current = IntArray(right.size + 1)
    for (row in 1..left.size) {
        current[0] = row
        var rowMinimum = row
        for (column in 1..right.size) {
            val substitution = previous[column - 1] + substitutionCost(left = left[row - 1], right = right[column - 1])
            current[column] = minOf(a = substitution, b = previous[column] + 1, c = current[column - 1] + 1)
            rowMinimum = minOf(a = rowMinimum, b = current[column])
        }
        if (rowMinimum > limit) return limit + 1
        val swap = previous
        previous = current
        current = swap
    }
    return previous[right.size]
}

private fun substitutionCost(left: Int, right: Int): Int {
    if (left == right) return 0
    return 1
}
