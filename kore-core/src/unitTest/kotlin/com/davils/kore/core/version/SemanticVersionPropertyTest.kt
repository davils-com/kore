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

import com.davils.kore.core.Limits
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlin.math.sign

private const val CASES: Int = 500
private const val HIGHEST_NUMBER: Int = 3
private const val MOST_IDENTIFIERS: Int = 3
private const val LONGEST_FREE_IDENTIFIER: Int = 60
private const val MOST_FREE_IDENTIFIERS: Int = 8
private const val NUMBERED_LINE_EXTRA: Int = 8
private const val SHORTEST_LONG_LABEL: Int = 200

private val identifiers: Arb<List<String>> =
    Arb.list(Arb.element("alpha", "beta", "rc", "0", "1", "2", "11", "x-y"), 0..MOST_IDENTIFIERS)

private val versions: Arb<SemanticVersion> = arbitrary {
    SemanticVersion(
        major = Arb.int(0..HIGHEST_NUMBER).bind(),
        minor = Arb.int(0..HIGHEST_NUMBER).bind(),
        patch = Arb.int(0..HIGHEST_NUMBER).bind(),
        preRelease = identifiers.bind(),
        build = identifiers.bind(),
    )
}

private val freeIdentifiers: Arb<List<String>> = Arb.list(
    Arb.string(1..LONGEST_FREE_IDENTIFIER, Codepoint.alphanumeric()),
    0..MOST_FREE_IDENTIFIERS,
)

class SemanticVersionPropertyTest : FunSpec({
    context("the text form") {
        test("every version reads back from its own text form") {
            checkAll(CASES, versions) { version ->
                SemanticVersion.parse(version.toString()) shouldBe version
            }
        }
    }

    context("the limits") {
        test("every version stated in code reads back from its text, or is refused when it is stated") {
            checkAll(CASES, freeIdentifiers, freeIdentifiers) { preRelease, build ->
                val stated = runCatching { SemanticVersion(major = 1, preRelease = preRelease, build = build) }
                val version = stated.getOrNull()

                stated.exceptionOrNull()?.shouldBeInstanceOf<IllegalArgumentException>()
                if (version != null) SemanticVersion.parse(version.toString()) shouldBe version
            }
        }

        test("a continued pre-release line reads back from its text, or is refused as too long") {
            val longestLabel = Limits.MAX_VERSION_LENGTH - NUMBERED_LINE_EXTRA
            checkAll(CASES, Arb.int(SHORTEST_LONG_LABEL..longestLabel)) { length ->
                val line = SemanticVersion(major = 1, preRelease = listOf("a".repeat(length), "9"))
                val continued = runCatching { line.nextPreRelease() }
                val next = continued.getOrNull()

                continued.exceptionOrNull()?.shouldBeInstanceOf<IllegalStateException>()
                if (next != null) SemanticVersion.parse(next.toString()) shouldBe next
            }
        }

        test("hostile text is refused with a reason that is safe to show") {
            checkAll(CASES, hostileText) { text ->
                val written = "1.0.0-$text"
                val failure = runCatching { SemanticVersion.parse(written) }.exceptionOrNull()

                (SemanticVersion.parseOrNull(written) == null) shouldBe (failure != null)
                failure?.shouldBeInstanceOf<IllegalArgumentException>()
                failure?.message.orEmpty().shouldBeSafeToShow()
            }
        }
    }

    context("comparison") {
        test("is antisymmetric") {
            checkAll(CASES, versions, versions) { left, right ->
                left.compareTo(right).sign shouldBe -right.compareTo(left).sign
            }
        }

        test("is transitive") {
            checkAll(iterations = CASES, genA = versions, genB = versions, genC = versions) { first, second, third ->
                val sorted = listOf(first, second, third).sorted()

                (sorted[0] <= sorted[2]) shouldBe true
            }
        }
    }

    context("ranges") {
        test("a version is in the range from itself to its successor and not in the one after") {
            checkAll(CASES, versions) { version ->
                val next = SemanticVersion(major = version.major, minor = version.minor, patch = version.patch + 1)
                val range = SemanticVersionRange.between(from = version, until = next)

                range.contains(version) shouldBe true
                range.contains(next, PreReleasePolicy.ByPrecedence) shouldBe false
            }
        }
    }
})
