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

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val CASES: Int = 5_000
private const val LONGEST: Int = 256
private const val CORE_GROUPS: Int = 3
private const val MINIMUM_SHARE: Int = 20
private const val NOISE_LENGTH: Int = 24
private const val SMALL_NUMBER: Int = 12
private const val OVERFLOW_MARGIN: Long = 2
private const val MOST_IDENTIFIERS: Int = 3
private const val FURTHEST_INSERT: Int = 40
private const val ALPHABET: String = "0123456789.-+aZx"

private val official: Regex = Regex(
    """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""" +
        """(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?""" +
        """(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$""",
)

private val alphabet: Arb<Char> = Arb.element(ALPHABET.toList())

private val noise: Arb<String> =
    Arb.string(minSize = 0, maxSize = NOISE_LENGTH, codepoints = alphabet.map { Codepoint(it.code) })

private val identifier: Arb<String> = Arb.element("0", "1", "01", "9", "10", "alpha", "rc", "x-y", "-", "0a", "")

private val identifiers: Arb<String> =
    Arb.list(identifier, 1..MOST_IDENTIFIERS).map { parts -> parts.joinToString(separator = ".") }

private val numeral: Arb<String> = Arb.choice(
    Arb.int(0..SMALL_NUMBER).map(Int::toString),
    Arb.long(Int.MAX_VALUE - OVERFLOW_MARGIN..Int.MAX_VALUE + OVERFLOW_MARGIN).map(Long::toString),
    Arb.element("00", "01", ""),
)

private val shaped: Arb<String> = arbitrary {
    val core = "${numeral.bind()}.${numeral.bind()}.${numeral.bind()}"
    val preRelease = Arb.choice(identifiers.map { text -> "-$text" }, Arb.element("")).bind()
    val build = Arb.choice(identifiers.map { text -> "+$text" }, Arb.element("")).bind()
    "$core$preRelease$build"
}

private val mutated: Arb<String> = arbitrary {
    val text = shaped.bind()
    val insertAt = Arb.int(0..FURTHEST_INSERT).bind()
    val position = insertAt.coerceAtMost(text.length)
    text.substring(startIndex = 0, endIndex = position) + alphabet.bind() + text.substring(position)
}

private val candidates: Arb<String> = Arb.choice(noise, shaped, mutated)

private fun isOfficial(text: String): Boolean {
    val match = official.matchEntire(text) ?: return false
    val isFitting = (1..CORE_GROUPS).all { group -> match.groupValues[group].toIntOrNull() != null }
    return isFitting && text.length <= LONGEST
}

class SemanticVersionConformanceTest : FunSpec({
    context("the regular expression published at semver.org") {
        test("accepts exactly what the parser accepts") {
            var accepted = 0
            checkAll(CASES, candidates) { text ->
                val isAccepted = isOfficial(text)
                if (isAccepted) accepted++
                withClue(text) {
                    (SemanticVersion.parseOrNull(text) != null) shouldBe isAccepted
                }
            }

            withClue("the generated texts must exercise both answers") {
                (accepted in CASES / MINIMUM_SHARE..CASES - CASES / MINIMUM_SHARE) shouldBe true
            }
        }
    }

    context("rendering") {
        test("renders every version it accepts exactly as it was written") {
            checkAll(CASES, candidates) { text ->
                val version = SemanticVersion.parseOrNull(text)
                if (version != null) {
                    withClue(text) { version.toString() shouldBe text }
                }
            }
        }
    }
})
