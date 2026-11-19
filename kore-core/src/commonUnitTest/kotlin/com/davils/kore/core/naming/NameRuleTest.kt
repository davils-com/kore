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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.core.naming

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll

private const val SCHEME_LENGTH: Int = 32
private const val KEY_LENGTH: Int = 64
private const val HUGE: Int = 10 * 1_024 * 1_024
private const val PLUGIN_ID: String = "plugin id"
private const val WARM_CACHE: String = "warm-cache"
private const val SIGNING_KEY: String = "2026-signing"
private const val ACME_BILLING: String = "acme.billing"

private val scheme: NameRule = nameRule {
    separators = "+.-"
    maxLength = SCHEME_LENGTH
    example = "https"
}

private val keyId: NameRule = nameRule {
    start = NameStart.LetterOrDigit
    maxLength = KEY_LENGTH
    example = SIGNING_KEY
}

private fun refusal(rule: NameRule, text: String): String {
    val failure = shouldThrow<IllegalArgumentException> { rule.requireValid(text = text, kind = PLUGIN_ID) }
    return failure.message.orEmpty()
}

class NameRuleTest : FunSpec({
    context("the common rules") {
        test("accept their own examples") {
            listOf(NameRule.KEBAB, NameRule.DOTTED, NameRule.QUALIFIED, NameRule.STRICT_KEBAB).forEach { rule ->
                rule.matches(rule.example) shouldBe true
                rule.maxLength shouldBe Limits.MAX_NAME_LENGTH
            }
        }

        test("accept a lax kebab name with doubled and trailing dashes") {
            NameRule.KEBAB.matches("warm--cache-") shouldBe true
            NameRule.KEBAB.matches("warm.cache") shouldBe false
        }

        test("divide a dotted name into kebab parts") {
            NameRule.DOTTED.matches("inventory") shouldBe true
            NameRule.DOTTED.matches("inventory.data-store.v2") shouldBe true
            NameRule.DOTTED.matches("inventory..data") shouldBe false
            NameRule.DOTTED.matches("inventory.2nd") shouldBe false
        }

        test("require two parts of a qualified name") {
            NameRule.QUALIFIED.matches("acme") shouldBe false
            NameRule.QUALIFIED.matches(ACME_BILLING) shouldBe true
        }

        test("refuse doubled and trailing separators in a strict name") {
            NameRule.STRICT_KEBAB.matches("load-settings.v2") shouldBe true
            NameRule.STRICT_KEBAB.matches("load--settings") shouldBe false
            NameRule.STRICT_KEBAB.matches("load-.settings") shouldBe false
            NameRule.STRICT_KEBAB.matches("load-") shouldBe false
        }
    }

    context("every rule") {
        test("refuses upper case, white space and characters outside ASCII") {
            listOf("Cache", "warm cache", "wärme", "cache\n").forEach { text ->
                NameRule.KEBAB.matches(text) shouldBe false
            }
        }

        test("refuses a name longer than its maximum before checking it") {
            val longest = "a".repeat(SCHEME_LENGTH)

            scheme.matches(longest) shouldBe true
            scheme.matches(longest + "a") shouldBe false
            NameRule.KEBAB.matches("a".repeat(HUGE)) shouldBe false
        }

        test("accepts only names that are safe to show as they are") {
            checkAll(hostileText) { text ->
                val isValid = NameRule.DOTTED.matches(text)

                if (isValid) SafeText.text(text) shouldBe text
            }
        }

        test("returns a valid name or null") {
            NameRule.KEBAB.validOrNull(WARM_CACHE) shouldBe WARM_CACHE
            NameRule.KEBAB.validOrNull("Warm").shouldBeNull()
        }
    }

    context("a built rule") {
        test("starts with a digit when it allows one") {
            keyId.matches(SIGNING_KEY) shouldBe true
            NameRule.KEBAB.matches(SIGNING_KEY) shouldBe false
        }

        test("allows the separators it names") {
            scheme.matches("svn+ssh") shouldBe true
            scheme.matches("svn_ssh") shouldBe false
        }

        test("refuses a contradictory configuration") {
            shouldThrow<IllegalArgumentException> { nameRule { example = "" } }
            shouldThrow<IllegalArgumentException> { nameRule { example = "Upper" } }
            shouldThrow<IllegalArgumentException> {
                nameRule {
                    separators = "-*"
                    example = "a"
                }
            }
            shouldThrow<IllegalArgumentException> {
                nameRule {
                    segmentSeparator = '-'
                    example = "a"
                }
            }
            shouldThrow<IllegalArgumentException> {
                nameRule {
                    segmentSeparator = '#'
                    example = "a"
                }
            }
            shouldThrow<IllegalArgumentException> {
                nameRule {
                    minSegments = 2
                    example = "a"
                }
            }
            shouldThrow<IllegalArgumentException> {
                nameRule {
                    minSegments = 0
                    example = "a"
                }
            }
            shouldThrow<IllegalArgumentException> {
                nameRule {
                    maxLength = Limits.MAX_NAME_LENGTH + 1
                    example = "a"
                }
            }
            shouldThrow<IllegalArgumentException> {
                nameRule {
                    maxLength = 0
                    example = "a"
                }
            }
        }
    }

    context("requireValid") {
        test("returns a valid name unchanged") {
            NameRule.QUALIFIED.requireValid(text = ACME_BILLING, kind = PLUGIN_ID) shouldBe ACME_BILLING
        }

        test("names the text, the kind, an example and the reason") {
            val message = refusal(NameRule.QUALIFIED, "Core")

            message shouldBe "\"Core\" is not a plugin id such as acme.billing: it has a part that does not start " +
                "with a lower-case letter"
        }

        test("gives the reason of every problem") {
            refusal(NameRule.QUALIFIED, "") shouldContain "is empty"
            refusal(scheme, "a".repeat(SCHEME_LENGTH + 1)) shouldContain "is longer than 32 characters"
            refusal(NameRule.KEBAB, "-a") shouldContain "does not start with a lower-case letter"
            refusal(keyId, "-a") shouldContain "with a lower-case letter or a digit"
            refusal(NameRule.QUALIFIED, "acme.Billing") shouldContain "has a part that does not start"
            refusal(NameRule.DOTTED, "acme_billing") shouldContain "digits, '-', '.'"
            refusal(NameRule.STRICT_KEBAB, "a--b") shouldContain "has two separators in a row"
            refusal(NameRule.STRICT_KEBAB, "a-") shouldContain "ends with a separator"
            refusal(NameRule.DOTTED, "a..b") shouldContain "has an empty part"
            refusal(NameRule.QUALIFIED, "acme") shouldContain "has fewer than 2 parts"
        }

        test("escapes hostile text, kind and example") {
            checkAll(hostileText) { text ->
                val failure = shouldThrow<IllegalArgumentException> {
                    NameRule.KEBAB.requireValid("$text!", kind = text, example = text)
                }

                failure.message.orEmpty().shouldBeSafeToShow()
            }
        }
    }
})
