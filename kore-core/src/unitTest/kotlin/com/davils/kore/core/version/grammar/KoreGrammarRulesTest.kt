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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val ARABIC_INDIC_SEVEN: String = "٧"
private const val NO_BREAK_SPACE: String = " "

class KoreGrammarRulesTest : FunSpec({
    context("identifiers") {
        test("an empty identifier is not numeric") {
            isNumeric("") shouldBe false
        }

        test("only ASCII digits make an identifier numeric") {
            isNumeric("0") shouldBe true
            isNumeric("1234567890") shouldBe true
            isNumeric(ARABIC_INDIC_SEVEN) shouldBe false
            isNumeric("1a") shouldBe false
        }

        test("an empty identifier is no pre-release identifier") {
            isPreReleaseIdentifier("") shouldBe false
            isIdentifier("") shouldBe false
        }
    }

    context("the whitespace of a range") {
        test("is a space or a tab and nothing else") {
            isRangeSpace(' ') shouldBe true
            isRangeSpace('\t') shouldBe true
            for (other in listOf('\n', '\r', '\u000B', '\u000C', ' ', ' ', '　', ' ')) {
                isRangeSpace(other) shouldBe false
            }
        }

        test("is trimmed only where it is a space or a tab") {
            trimmedRangeSpace(" \t^1.2\t ") shouldBe "^1.2"
            trimmedRangeSpace("$NO_BREAK_SPACE^1.2 ") shouldBe "$NO_BREAK_SPACE^1.2"
        }

        test("separates tokens however long it runs") {
            rangeTokens(">=1  \t <2") shouldContainExactly listOf(">=1", "<2")
            rangeTokens(">=1$NO_BREAK_SPACE<2") shouldContainExactly listOf(">=1$NO_BREAK_SPACE<2")
        }
    }
})
