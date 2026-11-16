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

package com.davils.kore.core.version.interval

import com.davils.kore.core.immutableList
import com.davils.kore.core.version.KoreSemanticVersion

private const val LEFT: Int = 0
private const val RIGHT: Int = 1
private const val SIDES: Int = 2
private const val BEFORE: Int = -1
private const val INSIDE: Int = 0
private const val AFTER: Int = 1

private val versionsWithNullFirst: Comparator<KoreSemanticVersion?> = nullsFirst()
private val versionsWithNullLast: Comparator<KoreSemanticVersion?> = nullsLast()
private val byFirst: Comparator<KoreVersionInterval> = compareBy(versionsWithNullFirst, KoreVersionInterval::first)
private val canonicalOrder: Comparator<KoreVersionInterval> =
    byFirst.thenBy(KoreVersionInterval::isStartExclusive).thenBy(versionsWithNullLast, KoreVersionInterval::end)
private val sidesByFirst: Comparator<Pair<Int, KoreVersionInterval>> =
    compareBy(versionsWithNullFirst) { (_, interval) -> interval.first }

internal fun normalized(intervals: List<KoreVersionInterval>): List<KoreVersionInterval> {
    val sorted = intervals.sortedWith(byFirst)
    val named = sorted.flatMap(::namedBands)
    val bands = spanned(named.sortedWith(byFirst))
    val bandEnds = bands.associate { band -> band.start to band.end }
    val mains = spanned(sorted).map { span -> mainOf(span, bandEnds) }
    val pieces = bands.filterNot { band -> isNamedByMain(band, mains) }
    val canonical = (mains + pieces).sortedWith(canonicalOrder)
    return canonical.immutableList()
}

internal fun intersection(
    left: List<KoreVersionInterval>,
    right: List<KoreVersionInterval>,
): List<KoreVersionInterval> {
    val sides = left.map { interval -> LEFT to interval } + right.map { interval -> RIGHT to interval }
    val open = List(SIDES) { mutableListOf<KoreVersionInterval>() }
    val overlaps = mutableListOf<KoreVersionInterval>()
    for ((side, next) in sides.sortedWith(sidesByFirst)) {
        val point = next.first
        for (active in open) active.removeAll { interval -> isEndedBy(interval, point) }
        open[RIGHT - side].mapNotNullTo(overlaps) { other -> overlap(first = other, second = next) }
        open[side] += next
    }
    return normalized(overlaps)
}

private fun isEndedBy(interval: KoreVersionInterval, point: KoreSemanticVersion?): Boolean {
    val end = interval.end
    return end != null && point != null && end <= point
}

private fun spanned(sorted: List<KoreVersionInterval>): List<KoreVersionInterval> {
    val spans = mutableListOf<KoreVersionInterval>()
    for (next in sorted) {
        val last = spans.lastOrNull()?.takeIf { previous -> reaches(earlier = previous, later = next) }
        if (last != null) spans.removeAt(spans.lastIndex)
        spans += spanOf(from = last ?: next, until = next)
    }
    return spans
}

private fun spanOf(from: KoreVersionInterval, until: KoreVersionInterval): KoreVersionInterval = KoreVersionInterval(
    start = from.first,
    isStartExclusive = false,
    end = laterEnd(first = from.end, second = until.end),
)

private fun namedBands(interval: KoreVersionInterval): List<KoreVersionInterval> {
    val cores = namedCores(interval)
    return cores.mapNotNull { core -> overlap(first = interval, second = preReleasesOf(core)) }
}

private fun namedCores(interval: KoreVersionInterval): Set<KoreSemanticVersion> {
    val bounds = listOfNotNull(interval.lower, interval.upper)
    val preReleases = bounds.filter(KoreSemanticVersion::isPreRelease)
    return preReleases.mapTo(mutableSetOf(), KoreSemanticVersion::release)
}

private fun preReleasesOf(core: KoreSemanticVersion): KoreVersionInterval {
    val floor = preReleaseFloor(listOf(core.major, core.minor, core.patch))
    return KoreVersionInterval(start = floor, isStartExclusive = false, end = core)
}

private fun mainOf(
    span: KoreVersionInterval,
    bandEnds: Map<KoreSemanticVersion?, KoreSemanticVersion?>,
): KoreVersionInterval {
    val floor = span.start ?: MINIMUM
    val isNamed = isFullyNamed(span, floor, bandEnds)
    if (floor == MINIMUM) return lowestMainOf(span, isNamed)
    return unnamedFrom(span, releaseBelow(floor).takeUnless { isNamed })
}

private fun lowestMainOf(span: KoreVersionInterval, isNamed: Boolean): KoreVersionInterval =
    KoreVersionInterval(start = MINIMUM.takeIf { isNamed }, isStartExclusive = false, end = span.end)

private fun unnamedFrom(span: KoreVersionInterval, below: KoreSemanticVersion?): KoreVersionInterval {
    if (below == null) return span
    return KoreVersionInterval(start = below, isStartExclusive = true, end = span.end)
}

private fun isFullyNamed(
    span: KoreVersionInterval,
    floor: KoreSemanticVersion,
    bandEnds: Map<KoreSemanticVersion?, KoreSemanticVersion?>,
): Boolean {
    val ceiling = earlierEnd(first = span.end, second = floor.release())
    return bandEnds[floor] == ceiling
}

private fun isNamedByMain(band: KoreVersionInterval, mains: List<KoreVersionInterval>): Boolean {
    val start = band.start ?: return false
    val holder = mains.getOrNull(mains.binarySearch { main -> placeOf(main, start) })
    return holder?.namesPreReleaseOf(start) == true
}

private fun placeOf(main: KoreVersionInterval, point: KoreSemanticVersion): Int {
    val end = main.end
    if (end != null && end <= point) return BEFORE
    return INSIDE.takeIf { point in main } ?: AFTER
}

private fun overlap(first: KoreVersionInterval, second: KoreVersionInterval): KoreVersionInterval? {
    val later = laterStarting(first, second)
    val end = earlierEnd(first = first.end, second = second.end)
    return interval(start = later.start, end = end, isStartExclusive = later.isStartExclusive)
}

private fun reaches(earlier: KoreVersionInterval, later: KoreVersionInterval): Boolean {
    val end = earlier.end
    val start = later.first
    return end == null || start == null || start <= end
}

private fun laterStarting(first: KoreVersionInterval, second: KoreVersionInterval): KoreVersionInterval {
    val order = compareValues(a = first.first, b = second.first)
    val isTieWonByFirst = order == 0 && !first.isStartExclusive
    val isFirstLater = order > 0 || isTieWonByFirst
    if (isFirstLater) return first
    return second
}

private fun earlierEnd(first: KoreSemanticVersion?, second: KoreSemanticVersion?): KoreSemanticVersion? {
    if (first == null || second == null) return first ?: second
    return minOf(a = first, b = second)
}

private fun laterEnd(first: KoreSemanticVersion?, second: KoreSemanticVersion?): KoreSemanticVersion? {
    if (first == null || second == null) return null
    return maxOf(a = first, b = second)
}
