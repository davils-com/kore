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

package com.davils.kore.core.principal

import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.principal.fixture.COMBINING_GRAPHEME_JOINER
import com.davils.kore.core.principal.fixture.HANGUL_FILLER
import com.davils.kore.core.principal.fixture.PLACEHOLDER
import com.davils.kore.core.principal.fixture.unsafeText
import com.davils.kore.core.principal.fixture.visibleText
import com.davils.kore.core.text.fixture.ESCAPE
import com.davils.kore.core.text.fixture.FORGED_LOG_LINE
import com.davils.kore.core.text.fixture.LONE_HIGH_SURROGATE
import com.davils.kore.core.text.fixture.RIGHT_TO_LEFT_OVERRIDE
import com.davils.kore.core.text.fixture.SOFT_HYPHEN
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.checkAll

private const val EMAIL: String = "email"
private const val ISSUER: String = "issuer"
private const val ALICE: String = "alice"
private const val TOO_MANY_MESSAGE: String = "A principal carries at most 64 attributes"
private const val UNSAFE_MESSAGE: String =
    "must not contain a control, format, private-use, unassigned or invisible character"

private val attributesOf: (Array<out Pair<String, String>>) -> KorePrincipalAttributes =
    KorePrincipalAttributes.Companion::invoke

private fun claims(count: Int): Array<Pair<String, String>> = Array(count) { index -> "claim-${index + 1}" to "$index" }

class KorePrincipalAttributesTest : FunSpec({
    context("reading") {
        test("an empty set has nothing in it and renders as nothing") {
            KorePrincipalAttributes.EMPTY.isEmpty shouldBe true
            KorePrincipalAttributes.EMPTY.toString() shouldBe "{}"
        }

        test("names are matched exactly, case included") {
            val attributes = KorePrincipalAttributes("sub" to ALICE)

            attributes["sub"] shouldBe ALICE
            attributes["Sub"].shouldBeNull()
            ("Sub" in attributes) shouldBe false
            ("sub" in attributes) shouldBe true
        }

        test("keeps its names when an attribute is added to a copy") {
            val attributes = KorePrincipalAttributes("a" to "1", "b" to "2")

            attributes.with(name = "c", value = "3")

            attributes.names shouldBe setOf("a", "b")
        }
    }

    context("sensitivity") {
        test("every attribute stated through the factory is sensitive") {
            val attributes = KorePrincipalAttributes(EMAIL to PLACEHOLDER, ISSUER to "idp")

            attributes.isSensitive(EMAIL) shouldBe true
            attributes.isSensitive(ISSUER) shouldBe true
        }

        test("an attribute added without saying otherwise is sensitive") {
            val attributes = KorePrincipalAttributes.EMPTY.with(name = EMAIL, value = PLACEHOLDER)

            attributes.isSensitive(EMAIL) shouldBe true
            attributes[EMAIL] shouldBe PLACEHOLDER
        }

        test("an attribute is only marked as not sensitive when that is said explicitly") {
            val attributes = KorePrincipalAttributes.EMPTY.with(name = ISSUER, value = "idp", sensitive = false)

            attributes.isSensitive(ISSUER) shouldBe false
            attributes[ISSUER] shouldBe "idp"
        }

        test("setting a visible attribute again without saying so makes it sensitive") {
            val visible = KorePrincipalAttributes.EMPTY.with(name = EMAIL, value = PLACEHOLDER, sensitive = false)

            visible.with(name = EMAIL, value = "x").isSensitive(EMAIL) shouldBe true
        }

        test("setting a sensitive attribute again as not sensitive makes it visible") {
            val masked = KorePrincipalAttributes(EMAIL to PLACEHOLDER)

            masked.with(name = EMAIL, value = "x", sensitive = false).isSensitive(EMAIL) shouldBe false
        }

        test("removing an attribute removes its sensitivity with it") {
            val masked = KorePrincipalAttributes(EMAIL to PLACEHOLDER)
            val attributes = masked.without(EMAIL)

            attributes.isEmpty shouldBe true
            attributes.isSensitive(EMAIL) shouldBe false
        }

        test("removing an attribute that is not there changes nothing") {
            val attributes = KorePrincipalAttributes("a" to "1")

            attributes.without("b") shouldBeSameInstanceAs attributes
        }
    }

    context("rendering") {
        test("names every attribute and masks every value, sensitive or not") {
            val issued = KorePrincipalAttributes(ISSUER to "idp")
            val attributes = issued.with(name = EMAIL, value = PLACEHOLDER, sensitive = false)

            attributes.toString() shouldBe """{"email"=****, "issuer"=****}"""
        }

        test("quotes and escapes a name, so a name cannot break the format") {
            val attributes = KorePrincipalAttributes("a\"=b, c\\" to "x")

            attributes.toString() shouldBe """{"a\"=b, c\\"=****}"""
        }

        test("never shows a value and is safe to show, whatever the names and values are") {
            checkAll(visibleText, visibleText) { name, value ->
                val attributes = KorePrincipalAttributes(name to PLACEHOLDER + value)
                val rendered = attributes.toString()

                rendered shouldNotContain PLACEHOLDER
                rendered.shouldBeSafeToShow()
            }
        }

        test("a principal carrying an attribute never prints its value") {
            val caller = principal(ALICE) { attribute(name = EMAIL, value = PLACEHOLDER, sensitive = false) }

            caller.toString() shouldNotContain PLACEHOLDER
        }
    }

    context("equality") {
        test("covers values and sensitivity") {
            val masked = KorePrincipalAttributes(EMAIL to PLACEHOLDER)
            val plain = KorePrincipalAttributes.EMPTY.with(name = EMAIL, value = PLACEHOLDER, sensitive = false)

            masked shouldBe KorePrincipalAttributes(EMAIL to PLACEHOLDER)
            masked.hashCode() shouldBe KorePrincipalAttributes(EMAIL to PLACEHOLDER).hashCode()
            masked shouldNotBe plain
            masked shouldNotBe KorePrincipalAttributes(EMAIL to "other")
        }

        test("never matches something that is not a set of attributes") {
            KorePrincipalAttributes.EMPTY.equals("{}") shouldBe false
        }
    }

    context("refusals") {
        test("a blank name is refused") {
            shouldThrow<IllegalArgumentException> { KorePrincipalAttributes(" " to "x") }
                .message shouldBe "An attribute name must not be blank"
            shouldThrow<IllegalArgumentException> { KorePrincipalAttributes.EMPTY.with(name = "", value = "x") }
        }

        test("a name longer than the limit is refused, and one of exactly the limit is not") {
            val longest = "n".repeat(KoreLimits.MAX_ATTRIBUTE_NAME_LENGTH)

            shouldThrow<IllegalArgumentException> { KorePrincipalAttributes(longest + "n" to "x") }
                .message shouldBe "An attribute name must not be longer than 256 characters"
            KorePrincipalAttributes(longest to "x").names shouldBe setOf(longest)
        }

        test("a name with a control or format character is refused") {
            listOf(ESCAPE, RIGHT_TO_LEFT_OVERRIDE, SOFT_HYPHEN, LONE_HIGH_SURROGATE, "\n").forEach { piece ->
                val failure = shouldThrow<IllegalArgumentException> {
                    KorePrincipalAttributes.EMPTY.with(name = "a${piece}b", value = "x")
                }
                failure.message shouldBe "An attribute name $UNSAFE_MESSAGE"
            }
        }

        test("a value longer than the limit is refused by naming the attribute, never the value") {
            val value = PLACEHOLDER.padEnd(KoreLimits.MAX_ATTRIBUTE_VALUE_LENGTH + 1, 'x')

            shouldThrow<IllegalArgumentException> { KorePrincipalAttributes(EMAIL to value) }
                .message shouldBe "The value of the attribute \"email\" must not be longer than 4096 characters"
        }

        test("a value with a control character is refused without showing it") {
            val failure = shouldThrow<IllegalArgumentException> {
                KorePrincipalAttributes.EMPTY.with(name = EMAIL, value = PLACEHOLDER + FORGED_LOG_LINE)
            }

            failure.message shouldBe "The value of the attribute \"email\" $UNSAFE_MESSAGE"
        }

        test("an empty value and a value of exactly the limit are accepted") {
            val longest = "v".repeat(KoreLimits.MAX_ATTRIBUTE_VALUE_LENGTH)

            KorePrincipalAttributes(EMAIL to "")[EMAIL] shouldBe ""
            KorePrincipalAttributes(EMAIL to longest)[EMAIL] shouldBe longest
        }

        test("more attributes than the limit are refused by the factory") {
            val entries = claims(KoreLimits.MAX_ATTRIBUTES + 1)

            shouldThrow<IllegalArgumentException> { attributesOf(entries) }
                .message shouldBe TOO_MANY_MESSAGE
            attributesOf(claims(KoreLimits.MAX_ATTRIBUTES)).names.size shouldBe KoreLimits.MAX_ATTRIBUTES
        }

        test("more entries than the limit are refused before any of them is read, even when names repeat") {
            val refusedLast = claims(KoreLimits.MAX_ATTRIBUTES) + ("" to "x")
            val repeated = Array(KoreLimits.MAX_ATTRIBUTES + 1) { EMAIL to "x" }

            shouldThrow<IllegalArgumentException> { attributesOf(refusedLast) }
                .message shouldBe TOO_MANY_MESSAGE
            shouldThrow<IllegalArgumentException> { attributesOf(repeated) }
                .message shouldBe TOO_MANY_MESSAGE
        }

        test("a name or a value with an invisible character is refused") {
            listOf(HANGUL_FILLER, "$EMAIL$COMBINING_GRAPHEME_JOINER").forEach { name ->
                shouldThrow<IllegalArgumentException> { KorePrincipalAttributes(name to "x") }
                    .message shouldBe "An attribute name $UNSAFE_MESSAGE"
            }
            shouldThrow<IllegalArgumentException> { KorePrincipalAttributes(EMAIL to "x$HANGUL_FILLER") }
                .message shouldBe "The value of the attribute \"email\" $UNSAFE_MESSAGE"
        }

        test("one attribute more than the limit is refused, while replacing one in a full set is not") {
            val full = attributesOf(claims(KoreLimits.MAX_ATTRIBUTES))

            shouldThrow<IllegalArgumentException> { full.with(name = "one-more", value = "x") }
                .message shouldBe TOO_MANY_MESSAGE
            full.with(name = "claim-1", value = "replaced")["claim-1"] shouldBe "replaced"
        }

        test("every refused value is refused with a message that is safe to show and never shows the value") {
            checkAll(unsafeText) { text ->
                val failure = shouldThrow<IllegalArgumentException> {
                    KorePrincipalAttributes.EMPTY.with(name = EMAIL, value = PLACEHOLDER + text)
                }
                val message = failure.message.shouldNotBeNull()

                message shouldNotContain PLACEHOLDER
                message.shouldBeSafeToShow()
            }
        }

        test("every refused name is refused with a message that is safe to show") {
            checkAll(unsafeText) { text ->
                val failure = shouldThrow<IllegalArgumentException> { KorePrincipalAttributes(text to PLACEHOLDER) }

                failure.message
                    .shouldNotBeNull()
                    .shouldBeSafeToShow()
            }
        }
    }
})
