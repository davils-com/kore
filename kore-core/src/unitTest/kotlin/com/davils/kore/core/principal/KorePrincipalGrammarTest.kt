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
import com.davils.kore.core.principal.fixture.BRAILLE_PATTERN_BLANK
import com.davils.kore.core.principal.fixture.COMBINING_GRAPHEME_JOINER
import com.davils.kore.core.principal.fixture.CountingRoles
import com.davils.kore.core.principal.fixture.HANGUL_FILLER
import com.davils.kore.core.principal.fixture.MONGOLIAN_FREE_VARIATION_SELECTOR
import com.davils.kore.core.principal.fixture.PLACEHOLDER
import com.davils.kore.core.principal.fixture.defaultIgnorableCodePoints
import com.davils.kore.core.principal.fixture.textWithAnInvisibleCharacter
import com.davils.kore.core.principal.fixture.unsafeText
import com.davils.kore.core.principal.fixture.visibleText
import com.davils.kore.core.text.fixture.BYTE_ORDER_MARK
import com.davils.kore.core.text.fixture.CLEAR_SCREEN
import com.davils.kore.core.text.fixture.ESCAPE
import com.davils.kore.core.text.fixture.FORGED_LOG_LINE
import com.davils.kore.core.text.fixture.LINE_SEPARATOR
import com.davils.kore.core.text.fixture.LONE_HIGH_SURROGATE
import com.davils.kore.core.text.fixture.RIGHT_TO_LEFT_OVERRIDE
import com.davils.kore.core.text.fixture.SOFT_HYPHEN
import com.davils.kore.core.text.fixture.TAG_LATIN_A
import com.davils.kore.core.text.fixture.VARIATION_SELECTOR
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import com.davils.kore.core.text.fixture.shouldBeSafeToShowLineByLine
import com.davils.kore.core.validation.report.KoreValidationException
import com.davils.kore.core.validation.violation.KoreViolation
import com.davils.kore.core.validation.violation.KoreViolationPath
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.checkAll

private const val ALICE: String = "alice"
private const val ADDRESS: String = "address"
private const val READER: String = "reader"
private const val TOO_MANY_TO_READ: Int = 1_000_000
private const val ROLES: String = "roles"

private val principalWithRoles: (String, Array<out String>) -> KorePrincipal = ::principal
private val addRoles: KorePrincipalBuilder.(Array<out String>) -> Unit = KorePrincipalBuilder::roles
private const val UNSAFE_MESSAGE: String =
    "must not contain a control, format, private-use, unassigned or invisible character"
private const val PRIVATE_USE: String = ""
private const val UNASSIGNED: String = "͸"

private val unsafePieces: List<String> = listOf(
    ESCAPE, RIGHT_TO_LEFT_OVERRIDE, LINE_SEPARATOR, SOFT_HYPHEN, LONE_HIGH_SURROGATE, BYTE_ORDER_MARK, TAG_LATIN_A,
    VARIATION_SELECTOR, CLEAR_SCREEN, FORGED_LOG_LINE, PRIVATE_USE, UNASSIGNED, "\u0000", "\u007F", "\u0085", "\t",
)

private fun refusalOf(block: () -> KorePrincipal): KoreViolation {
    val failure = shouldThrow<KoreValidationException> { block() }
    return failure.report.errors.single()
}

class KorePrincipalGrammarTest : FunSpec({
    context("ids") {
        test("keep the printable characters identity providers use in their subjects") {
            listOf(
                "auth0|5f7c8ec7c33c6c004bbafe82",
                "a+b@example.com",
                "google-oauth2|104",
                "CN=Alice Example, O=Acme",
                "Alice Example",
                "日本語",
                "😀",
            ).forEach { id -> KorePrincipal(id).id shouldBe id }
        }

        test("may be as long as the limit, but not longer") {
            val longest = "i".repeat(KoreLimits.MAX_PRINCIPAL_ID_LENGTH)

            KorePrincipal(longest).id shouldBe longest
            val violation = refusalOf { KorePrincipal(longest + "i") }
            violation.code shouldBe KorePrincipalViolationCodes.TOO_LONG
            violation.message shouldBe "must not be longer than 256 characters"
        }

        test("must not hold a control, format, private-use, unassigned or invisible character") {
            unsafePieces.forEach { piece ->
                val violation = refusalOf { KorePrincipal("a${piece}b") }

                violation.code shouldBe KorePrincipalViolationCodes.INVALID_CHARACTER
                violation.message shouldBe UNSAFE_MESSAGE
            }
        }

        test("must not hold any character that Unicode marks as default ignorable, wherever it stands") {
            defaultIgnorableCodePoints.forEach { codePoint ->
                val invisible = Character.toString(codePoint)

                listOf("a${invisible}b", "$ALICE$invisible", "$invisible$ALICE").forEach { id ->
                    val violation = refusalOf { KorePrincipal(id) }

                    violation.code shouldBe KorePrincipalViolationCodes.INVALID_CHARACTER
                    violation.message shouldBe UNSAFE_MESSAGE
                }
            }
        }

        test("are refused whenever they hold an invisible character, so no invisible variant passes for a caller") {
            checkAll(textWithAnInvisibleCharacter) { text ->
                val violation = refusalOf { KorePrincipal(text) }

                violation.code shouldBe KorePrincipalViolationCodes.INVALID_CHARACTER
            }
        }

        test("must not be an invisible variant of another id, so nobody acts for an invisible variant of itself") {
            listOf(HANGUL_FILLER, COMBINING_GRAPHEME_JOINER, MONGOLIAN_FREE_VARIATION_SELECTOR).forEach { invisible ->
                shouldThrow<KoreValidationException> { principal(ALICE).onBehalfOf(principal(ALICE + invisible)) }
            }
        }

        test("count the braille pattern blank as blank, and as whitespace at their edges, but not inside") {
            refusalOf { KorePrincipal(BRAILLE_PATTERN_BLANK + " ") }.code shouldBe KorePrincipalViolationCodes.BLANK_ID
            listOf(ALICE + BRAILLE_PATTERN_BLANK, BRAILLE_PATTERN_BLANK + ALICE).forEach { id ->
                refusalOf { KorePrincipal(id) }.code shouldBe KorePrincipalViolationCodes.SURROUNDING_WHITESPACE
            }
            KorePrincipal("al${BRAILLE_PATTERN_BLANK}ice").id shouldBe "al${BRAILLE_PATTERN_BLANK}ice"
        }

        test("must not start or end with whitespace, so two ids never differ only at their edges") {
            listOf(" alice", "alice ", " alice", "alice　").forEach { id ->
                val violation = refusalOf { KorePrincipal(id) }

                violation.code shouldBe KorePrincipalViolationCodes.SURROUNDING_WHITESPACE
                violation.message shouldBe "must not start or end with whitespace"
            }
        }

        test("are refused whenever they hold a character that is not safe to show") {
            checkAll(unsafeText) { text ->
                KorePrincipalBuilder(text)
                    .validate()
                    .isValid shouldBe false
            }
        }

        test("are kept and shown safely whenever they consist of visible characters") {
            checkAll(visibleText) { text ->
                val caller = KorePrincipal(text)

                caller.id shouldBe text
                caller.toString().shouldBeSafeToShow()
            }
        }
    }

    context("roles") {
        test("keep printable characters") {
            principal(ALICE, "role:admin", "api|read", "scope+write").roles shouldBe
                setOf("role:admin", "api|read", "scope+write")
        }

        test("are each reported under their own name when refused") {
            val tooLong = "r".repeat(KoreLimits.MAX_PRINCIPAL_TEXT_LENGTH + 1)

            val failure = shouldThrow<KoreValidationException> {
                principal(ALICE) { roles("kept", " admin", "a${ESCAPE}b", tooLong) }
            }

            val errors = failure.report.errors
            errors.map { it.code } shouldContainExactly listOf(
                KorePrincipalViolationCodes.SURROUNDING_WHITESPACE,
                KorePrincipalViolationCodes.INVALID_CHARACTER,
                KorePrincipalViolationCodes.TOO_LONG,
            )
            errors.take(2).map { it.path.toString() } shouldContainExactly listOf(
                "roles[\" admin\"]",
                """roles["a\u{001B}b"]""",
            )
        }

        test("may number as many as the limit, but not more") {
            val most = (1..KoreLimits.MAX_ROLES).map { index -> "role-$index" }

            KorePrincipal(id = ALICE, roles = most.toSet()).roles.size shouldBe KoreLimits.MAX_ROLES
            val violation = refusalOf { KorePrincipal(id = ALICE, roles = (most + "one-more").toSet()) }
            violation.code shouldBe KorePrincipalViolationCodes.TOO_MANY_ROLES
            violation.message shouldBe "must not hold more than 256 roles"
        }

        test("are refused before any of them is read when a set holds more than the limit") {
            val roles = CountingRoles(held = TOO_MANY_TO_READ)

            val violation = refusalOf { KorePrincipal(id = ALICE, roles = roles) }

            violation.code shouldBe KorePrincipalViolationCodes.TOO_MANY_ROLES
            roles.reads shouldBe 0
        }

        test("are refused all the same when a set reports fewer roles than it holds") {
            val roles = CountingRoles(held = KoreLimits.MAX_ROLES + 1, reported = 1)

            refusalOf { KorePrincipal(id = ALICE, roles = roles) }.code shouldBe
                KorePrincipalViolationCodes.TOO_MANY_ROLES
        }

        test("are refused as names before any is read when there are more than the limit, repeated names included") {
            val names = Array(KoreLimits.MAX_ROLES + 1) { READER }

            refusalOf { principalWithRoles(ALICE, names) }.code shouldBe KorePrincipalViolationCodes.TOO_MANY_ROLES
            principalWithRoles(ALICE, Array(KoreLimits.MAX_ROLES) { READER }).roles shouldBe setOf(READER)
        }

        test("are refused in one call of the builder with more names than the limit, repeated names included") {
            val names = Array(KoreLimits.MAX_ROLES + 1) { READER }

            val violation = refusalOf { principal(ALICE) { addRoles(names) } }

            violation.code shouldBe KorePrincipalViolationCodes.TOO_MANY_ROLES
            violation.path.toString() shouldBe ROLES
        }

        test("are reported once and no longer checked when the calls of the builder add up to more than the limit") {
            val violation = refusalOf {
                principal(ALICE) {
                    roles(" refused")
                    repeat(KoreLimits.MAX_ROLES + 1) { index -> roles("role-$index") }
                    roles(" refused too")
                }
            }

            violation.code shouldBe KorePrincipalViolationCodes.TOO_MANY_ROLES
            violation.path.toString() shouldBe ROLES
        }

        test("are shown safely in a report whatever was refused") {
            checkAll(unsafeText) { text ->
                val builder = KorePrincipalBuilder(ALICE)
                builder.roles(text)

                val report = builder.validate()

                report.isValid shouldBe false
                report.render().shouldBeSafeToShowLineByLine()
            }
        }
    }

    context("tenant and display name") {
        test("a tenant must not have surrounding whitespace, an unsafe character or too many characters") {
            val tooLong = "t".repeat(KoreLimits.MAX_PRINCIPAL_TEXT_LENGTH + 1)

            listOf(
                " acme" to KorePrincipalViolationCodes.SURROUNDING_WHITESPACE,
                "ac${RIGHT_TO_LEFT_OVERRIDE}me" to KorePrincipalViolationCodes.INVALID_CHARACTER,
                tooLong to KorePrincipalViolationCodes.TOO_LONG,
            ).forEach { (shown, code) ->
                val violation = refusalOf { principal(ALICE) { tenant = shown } }

                violation.code shouldBe code
                violation.path.toString() shouldBe "tenant"
            }
        }

        test("a tenant, a role and a display name must not hold an invisible character either") {
            listOf(HANGUL_FILLER, COMBINING_GRAPHEME_JOINER, MONGOLIAN_FREE_VARIATION_SELECTOR).forEach { invisible ->
                val failure = shouldThrow<KoreValidationException> {
                    principal(ALICE) {
                        tenant = "acme$invisible"
                        roles("admin$invisible")
                        displayName = "Alice$invisible"
                    }
                }

                val errors = failure.report.errors
                errors.map { it.code } shouldContainExactly List(errors.size) {
                    KorePrincipalViolationCodes.INVALID_CHARACTER
                }
                val rolePath = KoreViolationPath.named(KorePrincipal::roles).key("admin$invisible")
                errors.map { it.path.toString() } shouldContainExactly listOf("$rolePath", "tenant", "displayName")
            }
        }

        test("a display name may have surrounding whitespace, but no unsafe character and not too many characters") {
            val tooLong = "d".repeat(KoreLimits.MAX_PRINCIPAL_TEXT_LENGTH + 1)

            principal(ALICE) { displayName = " Alice " }.displayName shouldBe " Alice "
            listOf(
                "Alice${FORGED_LOG_LINE}" to KorePrincipalViolationCodes.INVALID_CHARACTER,
                tooLong to KorePrincipalViolationCodes.TOO_LONG,
            ).forEach { (shown, code) ->
                val violation = refusalOf { principal(ALICE) { displayName = shown } }

                violation.code shouldBe code
                violation.path.toString() shouldBe "displayName"
            }
        }
    }

    context("attributes in the builder") {
        test("a refused name is reported under attributes without showing it") {
            val failure = shouldThrow<KoreValidationException> {
                principal(ALICE) { attribute(name = "a${CLEAR_SCREEN}b", value = "x") }
            }

            val violation = failure.report.errors.single()
            violation.path.toString() shouldBe "attributes"
            violation.code shouldBe KorePrincipalViolationCodes.INVALID_CHARACTER
            violation.message shouldBe "an attribute name $UNSAFE_MESSAGE"
            failure.message shouldNotContain ESCAPE
        }

        test("a name longer than the limit is reported") {
            val name = "n".repeat(KoreLimits.MAX_ATTRIBUTE_NAME_LENGTH + 1)

            val violation = refusalOf { principal(ALICE) { attribute(name = name, value = "x") } }

            violation.code shouldBe KorePrincipalViolationCodes.TOO_LONG
            violation.message shouldBe "an attribute name must not be longer than 256 characters"
        }

        test("a refused value is reported under the name of its attribute without showing the value") {
            val failure = shouldThrow<KoreValidationException> {
                principal(ALICE) { attribute(name = ADDRESS, value = PLACEHOLDER + FORGED_LOG_LINE) }
            }

            val violation = failure.report.errors.single()
            violation.path.toString() shouldBe "attributes[\"address\"]"
            violation.code shouldBe KorePrincipalViolationCodes.INVALID_CHARACTER
            failure.message shouldNotContain PLACEHOLDER
        }

        test("a value longer than the limit is reported") {
            val value = "v".repeat(KoreLimits.MAX_ATTRIBUTE_VALUE_LENGTH + 1)

            val violation = refusalOf { principal(ALICE) { attribute(name = ADDRESS, value = value) } }

            violation.code shouldBe KorePrincipalViolationCodes.TOO_LONG
            violation.message shouldBe "must not be longer than 4096 characters"
        }

        test("more attributes than the limit are reported once") {
            val violation = refusalOf {
                principal(ALICE) {
                    for (index in 0..KoreLimits.MAX_ATTRIBUTES + 1) {
                        attribute(name = "claim-$index", value = "x")
                    }
                }
            }

            violation.code shouldBe KorePrincipalViolationCodes.TOO_MANY_ATTRIBUTES
            violation.path.toString() shouldBe "attributes"
            violation.message shouldBe "must not hold more than 64 attributes"
        }

        test("an attribute of a full builder may still be replaced") {
            val caller = principal(ALICE) {
                for (index in 1..KoreLimits.MAX_ATTRIBUTES) {
                    attribute(name = "claim-$index", value = "x")
                }
                attribute(name = "claim-1", value = "replaced")
            }

            caller.attributes["claim-1"] shouldBe "replaced"
        }

        test("are shown safely in a report whatever was refused") {
            checkAll(unsafeText) { text ->
                val builder = KorePrincipalBuilder(ALICE)
                builder.attribute(name = text, value = "x")
                builder.attribute(name = ADDRESS, value = text)

                val report = builder.validate()

                report.render().shouldBeSafeToShowLineByLine()
            }
        }
    }
})
