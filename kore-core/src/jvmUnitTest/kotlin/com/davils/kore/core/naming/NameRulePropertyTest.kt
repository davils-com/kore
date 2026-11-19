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

package com.davils.kore.core.naming

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val LONGEST: Int = 24
private const val ITERATIONS: Int = 5_000
private const val SCHEME_LENGTH: Int = 32
private const val KEY_LENGTH: Int = 64

private val pieces: List<String> = listOf(
    "a", "z", "k", "0", "9", "-", ".", "_", "+", ":", "/", "A", " ", "ä", "\u0000", "\n", "--", "..", "a.b",
)

private val candidates: Arb<String> =
    Arb.list(Arb.element(pieces), 0..LONGEST).map { parts -> parts.joinToString(separator = "") }

private val oracles: List<Pair<NameRule, Regex>> = listOf(
    NameRule.KEBAB to Regex("[a-z][a-z0-9-]*"),
    NameRule.DOTTED to Regex("[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)*"),
    NameRule.QUALIFIED to Regex("[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+"),
    NameRule.STRICT_KEBAB to Regex("[a-z][a-z0-9]*([.-][a-z0-9]+)*"),
    nameRule {
        separators = "+.-"
        maxLength = SCHEME_LENGTH
        example = "https"
    } to Regex("[a-z][a-z0-9+.-]{0,31}"),
    nameRule {
        start = NameStart.LetterOrDigit
        maxLength = KEY_LENGTH
        example = "k1"
    } to Regex("[a-z0-9][a-z0-9-]{0,63}"),
)

class NameRulePropertyTest : FunSpec({
    context("a name rule") {
        test("accepts exactly what the regular expression it replaces accepts") {
            checkAll(iterations = ITERATIONS, genA = candidates) { text ->
                oracles.forEach { (rule, regex) -> rule.matches(text) shouldBe regex.matches(text) }
            }
        }
    }
})
