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

import com.davils.kore.core.principal.fixture.PLACEHOLDER
import com.davils.kore.core.validation.report.KoreValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val ALICE: String = "alice"
private const val READER: String = "reader"
private const val ISSUER: String = "issuer"
private const val BLANK: String = "must not be blank"
private const val ACME: String = "acme"
private const val GLOBEX: String = "globex"
private const val INDEXER: String = "indexer"
private const val EVALUATED: String =
    "This builder was already evaluated by build() or validate(), so nothing in it can be changed any more"

class KorePrincipalBuilderTest : FunSpec({
    context("building") {
        test("builds a principal with every field set") {
            val alice = principal(ALICE) { tenant = "acme" }
            val caller = principal("indexer") {
                kind = KorePrincipalKind.Service
                tenant = "acme"
                displayName = "Search indexer"
                roles(READER, "exporter")
                attribute(name = ISSUER, value = "idp", sensitive = false)
                attribute(name = "token", value = PLACEHOLDER)
                actingFor(alice)
            }

            caller.kind shouldBe KorePrincipalKind.Service
            caller.tenant shouldBe "acme"
            caller.displayName shouldBe "Search indexer"
            caller.roles shouldBe setOf(READER, "exporter")
            caller.attributes[ISSUER] shouldBe "idp"
            caller.attributes.isSensitive(ISSUER) shouldBe false
            caller.attributes.isSensitive("token") shouldBe true
            caller.actingFor shouldBe alice
        }

        test("a builder with nothing set is a plain user") {
            principal(ALICE) { } shouldBe KorePrincipal(ALICE)
        }
    }

    context("reporting") {
        test("reports every problem at once, each under the field it is about") {
            val failure = shouldThrow<KoreValidationException> {
                principal(" ") {
                    tenant = ""
                    displayName = " "
                    roles(READER, "")
                    attribute(name = "", value = "x")
                }
            }

            failure.report.errors.map { it.path.toString() } shouldContainExactly listOf(
                "attributes",
                "id",
                "roles[\"\"]",
                "tenant",
                "displayName",
            )
            failure.report.errors.map { it.code } shouldContainExactly listOf(
                KorePrincipalViolationCodes.INVALID_ATTRIBUTE,
                KorePrincipalViolationCodes.BLANK_ID,
                KorePrincipalViolationCodes.BLANK_ROLE,
                KorePrincipalViolationCodes.BLANK_TENANT,
                KorePrincipalViolationCodes.BLANK_DISPLAY_NAME,
            )
            failure.report.errors.map { it.message } shouldContainExactly listOf(
                "an attribute name must not be blank",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            )
        }

        test("refuses acting for itself as a violation rather than a throw from inside the block") {
            val failure = shouldThrow<KoreValidationException> {
                principal(ALICE) { actingFor(principal(ALICE)) }
            }

            failure.report.errors
                .single()
                .code shouldBe KorePrincipalViolationCodes.SELF_DELEGATION
            failure.report.errors
                .single()
                .message shouldBe "must not be the principal itself"
        }

        test("the single-call form reports every problem at once too") {
            val failure = shouldThrow<KoreValidationException> { KorePrincipal(id = "", roles = setOf(" ")) }

            failure.report.errors.map { it.code } shouldContainExactly listOf(
                KorePrincipalViolationCodes.BLANK_ID,
                KorePrincipalViolationCodes.BLANK_ROLE,
            )
        }

        test("a report names the subject") {
            val failure = shouldThrow<KoreValidationException> { principal("") }

            failure.report.subject shouldBe "principal"
        }

        test("a builder report names the subject as well") {
            val failure = shouldThrow<KoreValidationException> { principal("") { tenant = "acme" } }

            failure.report.subject shouldBe "principal"
        }
    }

    context("after evaluating") {
        test("refuses every change made after validate() inside the block instead of dropping it") {
            val alice = principal(ALICE) { tenant = GLOBEX }
            val refusals = mutableListOf<IllegalStateException>()

            val caller = principal(INDEXER) {
                kind = KorePrincipalKind.Service
                tenant = ACME
                validate()
                refusals += shouldThrow<IllegalStateException> { tenant = GLOBEX }
                refusals += shouldThrow<IllegalStateException> { kind = KorePrincipalKind.User }
                refusals += shouldThrow<IllegalStateException> { displayName = "Indexer" }
                refusals += shouldThrow<IllegalStateException> { roles(READER) }
                refusals += shouldThrow<IllegalStateException> { attribute(name = ISSUER, value = "idp") }
                refusals += shouldThrow<IllegalStateException> { actingFor(alice, acrossTenants = true) }
                tenant shouldBe ACME
                kind shouldBe KorePrincipalKind.Service
                displayName.shouldBeNull()
            }

            refusals.map { it.message } shouldContainExactly List(refusals.size) { EVALUATED }
            caller.tenant shouldBe ACME
            caller.kind shouldBe KorePrincipalKind.Service
            caller.isDelegated shouldBe false
        }

        test("refuses every change made through a builder kept beyond its block") {
            var kept: KorePrincipalBuilder? = null
            principal(ALICE) { kept = this }
            val builder = kept.shouldNotBeNull()

            shouldThrow<IllegalStateException> { builder.tenant = ACME }
            shouldThrow<IllegalStateException> { builder.roles(READER) }
            shouldThrow<IllegalStateException> { builder.actingFor(principal(INDEXER)) }
        }

        test("keeps an invalid principal to itself, so not even its evaluation hands one out") {
            val builder = KorePrincipalBuilder("$ALICE ")
            builder.roles(READER)

            val evaluation = builder.evaluate()

            evaluation.value.shouldBeNull()
            evaluation.report.errors
                .single()
                .code shouldBe KorePrincipalViolationCodes.SURROUNDING_WHITESPACE
        }
    }
})
