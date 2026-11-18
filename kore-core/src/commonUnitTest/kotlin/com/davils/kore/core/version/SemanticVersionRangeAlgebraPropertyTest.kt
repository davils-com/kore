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

package com.davils.kore.core.version

import com.davils.kore.core.version.interval.VersionInterval
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val HIGHEST_NUMBER: Int = 3
private const val MOST_INTERVALS: Int = 3
private const val MOST_COMPARATORS: Int = 2
private const val MOST_CANDIDATES: Int = 8
private const val UNBOUNDED_SHARE: Double = 0.2
private const val LABEL: String = "zeta"
private const val ALTERNATIVE_SEPARATOR: String = " || "

private val preRelease: Arb<List<String>> = Arb.list(Arb.element("alpha", "rc", "0", "1", "2"), 0..2)

private val build: Arb<List<String>> = Arb.list(Arb.element("a", "b", "7"), 0..1)

private val versions: Arb<SemanticVersion> = arbitrary {
    SemanticVersion(
        major = Arb.int(0..HIGHEST_NUMBER).bind(),
        minor = Arb.int(0..HIGHEST_NUMBER).bind(),
        patch = Arb.int(0..HIGHEST_NUMBER).bind(),
        preRelease = preRelease.bind(),
        build = build.bind(),
    )
}

private val bounds: Arb<SemanticVersion?> = versions.orNull(UNBOUNDED_SHARE)

private val singleIntervals: Arb<SemanticVersionRange> = arbitrary {
    val lower = bounds.bind()
    val isLowerInclusive = Arb.boolean().bind()
    val upper = bounds.bind()
    val isUpperInclusive = Arb.boolean().bind()
    val stated = runCatching {
        SemanticVersionRange.bounded(
            lower = lower,
            lowerInclusive = isLowerInclusive,
            upper = upper,
            upperInclusive = isUpperInclusive,
        )
    }
    stated.getOrElse { SemanticVersionRange.exactly(lower ?: upper ?: SemanticVersion(1)) }
}

private val unitedRanges: Arb<SemanticVersionRange> =
    Arb.list(singleIntervals, 1..MOST_INTERVALS).map { parts -> parts.reduce(SemanticVersionRange::union) }

private val sharedRanges: Arb<SemanticVersionRange> =
    Arb.bind(unitedRanges, unitedRanges) { left, right -> left intersect right }

private val comparators: Arb<String> = arbitrary {
    val operator = Arb.element("", "=", ">", ">=", "<", "<=", "^", "~").bind()
    "$operator${versions.bind()}"
}

private val alternatives: Arb<String> =
    Arb.list(comparators, 1..MOST_COMPARATORS).map { parts -> parts.joinToString(separator = " ") }

private val readRanges: Arb<SemanticVersionRange> = Arb.list(alternatives, 1..MOST_INTERVALS).map { parts ->
    val text = parts.joinToString(separator = ALTERNATIVE_SEPARATOR)
    SemanticVersionRange.parseOrNull(text) ?: SemanticVersionRange.NONE
}

private val ranges: Arb<SemanticVersionRange> = Arb.choice(unitedRanges, sharedRanges, readRanges)

private fun isNested(outer: VersionInterval, inner: VersionInterval): Boolean {
    val start = inner.first ?: return false
    val outerEnd = outer.end
    val innerEnd = inner.end
    val isInnerEndBelow = innerEnd != null && outerEnd != null && innerEnd <= outerEnd
    val isEndInside = outerEnd == null || isInnerEndBelow
    val isInside = compareValues(a = outer.first, b = start) <= 0 && isEndInside
    return isInside && inner.namesPreReleaseOf(start) && !outer.namesPreReleaseOf(start)
}

private fun isApart(earlier: VersionInterval, later: VersionInterval): Boolean {
    val end = earlier.end
    val start = later.first
    return end != null && start != null && end < start
}

class SemanticVersionRangeAlgebraPropertyTest : FunSpec({
    context("the set operations") {
        test("a version is in a union exactly when it is in either side, under every policy") {
            checkAll(iterations = CASES, genA = ranges, genB = ranges, genC = versions) { left, right, version ->
                val united = left union right

                for (policy in PreReleasePolicy.entries) {
                    withClue("$left | $right | $policy") {
                        united.contains(version, policy) shouldBe
                            (left.contains(version, policy) || right.contains(version, policy))
                    }
                }
            }
        }

        test("a version is in an intersection by precedence exactly when it is in both sides") {
            checkAll(iterations = CASES, genA = ranges, genB = ranges, genC = versions) { left, right, version ->
                val shared = left intersect right
                val policy = PreReleasePolicy.ByPrecedence

                shared.contains(version, policy) shouldBe
                    (left.contains(version, policy) && right.contains(version, policy))
            }
        }

        test("an intersection admits a pre-release that either side names, as one comparator set does") {
            checkAll(iterations = CASES, genA = ranges, genB = ranges, genC = versions) { left, right, version ->
                val shared = left intersect right
                val isNamed = left.contains(version) || right.contains(version)

                val isInside = shared.contains(version, PreReleasePolicy.ByPrecedence)

                shared.contains(version) shouldBe (isInside && isNamed)
            }
        }

        test("both are commutative") {
            checkAll(CASES, ranges, ranges) { left, right ->
                val united = left union right
                val shared = left intersect right

                united shouldBe (right union left)
                shared shouldBe (right intersect left)
            }
        }

        test("both are associative") {
            checkAll(iterations = CASES, genA = ranges, genB = ranges, genC = ranges) { first, second, third ->
                val unitedFirst = first union second
                val unitedLast = second union third
                val sharedFirst = first intersect second
                val sharedLast = second intersect third

                unitedFirst union third shouldBe (first union unitedLast)
                sharedFirst intersect third shouldBe (first intersect sharedLast)
            }
        }

        test("both are idempotent") {
            checkAll(CASES, ranges) { range ->
                val united = range union range
                val shared = range intersect range

                united shouldBe range
                shared shouldBe range
            }
        }
    }

    context("alternatives") {
        test("a version is in alternatives exactly when it is in one of them, under every policy") {
            checkAll(
                iterations = CASES,
                genA = alternatives,
                genB = alternatives,
                genC = versions,
            ) { left, right, version ->
                val written = "$left$ALTERNATIVE_SEPARATOR$right"
                val both = SemanticVersionRange.parseOrNull(written)
                val first = SemanticVersionRange.parseOrNull(left)
                val second = SemanticVersionRange.parseOrNull(right)

                withClue(written) { (both == null) shouldBe (first == null || second == null) }
                if (both != null && first != null && second != null) {
                    for (policy in PreReleasePolicy.entries) {
                        withClue("$written | $policy") {
                            both.contains(version, policy) shouldBe
                                (first.contains(version, policy) || second.contains(version, policy))
                        }
                    }
                }
            }
        }
    }

    context("the canonical form") {
        test("reads back to an equal range") {
            checkAll(CASES, ranges) { range ->
                withClue(range.toString()) { SemanticVersionRange.parse(range.toString()) shouldBe range }
            }
        }

        test("keeps intervals apart, or nests one only to name a pre-release the other does not") {
            checkAll(CASES, ranges) { range ->
                val intervals = range.intervals
                for ((index, earlier) in intervals.withIndex()) {
                    for (later in intervals.drop(index + 1)) {
                        val isSeparate = isApart(earlier, later) ||
                            isNested(outer = earlier, inner = later) ||
                            isNested(outer = later, inner = earlier)
                        withClue("$range: $earlier and $later") { isSeparate shouldBe true }
                    }
                }
            }
        }
    }

    context("the discrete bounds") {
        test("an exclusive lower bound admits exactly the versions above it") {
            checkAll(CASES, versions, versions) { bound, version ->
                val above = SemanticVersionRange.parse(">$bound")

                above.contains(version, PreReleasePolicy.ByPrecedence) shouldBe (version > bound)
            }
        }

        test("an inclusive upper bound admits exactly the versions up to it") {
            checkAll(CASES, versions, versions) { bound, version ->
                val upTo = SemanticVersionRange.parse("<=$bound")

                upTo.contains(version, PreReleasePolicy.ByPrecedence) shouldBe (version <= bound)
            }
        }

        test("exactly one version admits only that version, whatever its build") {
            checkAll(CASES, versions, versions) { bound, version ->
                SemanticVersionRange.exactly(bound).contains(version) shouldBe version.isSamePrecedence(bound)
            }
        }
    }

    context("pre-release policies") {
        test("by precedence is plain membership in one of the intervals") {
            checkAll(CASES, ranges, versions) { range, version ->
                val isInAnInterval = range.intervals.any { interval -> version in interval }

                range.contains(version, PreReleasePolicy.ByPrecedence) shouldBe isInAnInterval
            }
        }

        test("the stricter policies only ever take versions away") {
            checkAll(CASES, ranges, versions) { range, version ->
                val isInside = range.contains(version, PreReleasePolicy.ByPrecedence)
                val isInsideSameCore = range.contains(version, PreReleasePolicy.SameCoreOnly)
                val isInsideReleasesOnly = range.contains(version, PreReleasePolicy.ReleasesOnly)

                (!isInsideSameCore || isInside) shouldBe true
                (!isInsideReleasesOnly || isInsideSameCore) shouldBe true
                (isInsideReleasesOnly && version.isPreRelease) shouldBe false
            }
        }

        test("the default policy is same core only") {
            checkAll(CASES, ranges, versions) { range, version ->
                range.contains(version) shouldBe range.contains(version, PreReleasePolicy.SameCoreOnly)
            }
        }
    }

    context("selecting") {
        test("the newest version chosen is inside, and nothing inside is newer") {
            checkAll(CASES, ranges, Arb.list(versions, 0..MOST_CANDIDATES)) { range, candidates ->
                val chosen = range.maxSatisfying(candidates)
                val inside = range.satisfying(candidates)

                (chosen == null) shouldBe inside.isEmpty()
                val newer = inside.filter { version ->
                    chosen != null && SemanticVersion.TOTAL_ORDER.compare(version, chosen) > 0
                }
                newer shouldBe emptyList()
            }
        }
    }

    context("the operations on a version") {
        test("every next version is above the one it follows") {
            checkAll(CASES, versions) { version ->
                (version.nextMajor() > version) shouldBe true
                (version.nextMinor() > version) shouldBe true
                (version.nextPatch() > version) shouldBe true
                (version.nextPreRelease(LABEL) > version) shouldBe true
            }
        }

        test("a version is compatible with itself") {
            checkAll(CASES, versions) { version ->
                version.isCompatibleWith(version) shouldBe true
            }
        }

        test("the total order agrees with equality") {
            checkAll(CASES, versions, versions) { left, right ->
                (SemanticVersion.TOTAL_ORDER.compare(left, right) == 0) shouldBe (left == right)
            }
        }

        test("the same precedence agrees with the natural order") {
            checkAll(CASES, versions, versions) { left, right ->
                left.isSamePrecedence(right) shouldBe (left.compareTo(right) == 0)
            }
        }
    }
})
