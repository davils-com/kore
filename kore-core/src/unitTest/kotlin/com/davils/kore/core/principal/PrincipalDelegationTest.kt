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

import com.davils.kore.core.Limits
import com.davils.kore.core.principal.fixture.PLACEHOLDER
import com.davils.kore.core.principal.fixture.chainOf
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val ALICE: String = "alice"
private const val INDEXER: String = "indexer"
private const val GATEWAY: String = "gateway"
private const val READER: String = "reader"
private const val SERVICE: String = "service"
private const val ACME: String = "acme"
private const val GLOBEX: String = "globex"

private val chainAndRepeat: Arb<Pair<Int, Int>> = Arb.int(2 until Limits.MAX_DELEGATION_CHAIN).flatMap { length ->
    Arb.int(1 until length).map { repeated -> length to repeated }
}

class PrincipalDelegationTest : FunSpec({
    val alice = principal(ALICE, READER)
    val indexer = Principal(INDEXER, PrincipalKind.Service, setOf(SERVICE))
    val gateway = Principal(GATEWAY, PrincipalKind.Service)

    fun refusalOf(block: () -> Principal): ValidationException = shouldThrow<ValidationException> {
        block()
    }

    context("without delegation") {
        test("a principal acts for nobody unless told to") {
            alice.actingFor.shouldBeNull()
            alice.isDelegated shouldBe false
            alice.originator shouldBe alice
            alice.chain shouldContainExactly listOf(alice)
        }
    }

    context("acting on behalf of another") {
        test("keeps both, the immediate caller first") {
            val delegated = indexer.onBehalfOf(alice)

            delegated.id shouldBe INDEXER
            delegated.actingFor shouldBe alice
            delegated.isDelegated shouldBe true
            delegated.originator shouldBe alice
            delegated.chain.map { it.id } shouldContainExactly listOf(INDEXER, ALICE)
        }

        test("nests, and the originator is who started the chain") {
            val delegated = gateway.onBehalfOf(indexer.onBehalfOf(alice))

            delegated.chain.map { it.id } shouldContainExactly listOf(GATEWAY, INDEXER, ALICE)
            delegated.originator shouldBe alice
        }

        test("does not pass roles along the chain") {
            val delegated = indexer.onBehalfOf(alice)

            delegated.hasRole(READER) shouldBe false
            delegated.hasRole(SERVICE) shouldBe true
            delegated.originator.hasRole(READER) shouldBe true
        }

        test("leaves the original principal as it was") {
            indexer.onBehalfOf(alice)

            indexer.actingFor.shouldBeNull()
        }

        test("may act for a principal of its own tenant") {
            val user = principal(ALICE) { tenant = ACME }
            val service = principal(INDEXER) { tenant = ACME }

            service.onBehalfOf(user).originator shouldBe user
        }

        test("may act for the same id in another tenant when acting across tenants is allowed explicitly") {
            val here = principal(ALICE) { tenant = ACME }
            val there = principal(ALICE) { tenant = GLOBEX }

            here.onBehalfOf(there, acrossTenants = true).originator shouldBe there
        }

        test("may act across tenants in the builder when that is allowed explicitly") {
            val there = principal(ALICE) { tenant = GLOBEX }

            val delegated = principal(INDEXER) {
                tenant = ACME
                actingFor(there, acrossTenants = true)
            }

            delegated.originator shouldBe there
        }

        test("treats an explicit allowance within one tenant like no allowance at all") {
            indexer.onBehalfOf(alice, acrossTenants = true) shouldBe indexer.onBehalfOf(alice)
            indexer.onBehalfOf(alice, acrossTenants = true).hashCode() shouldBe indexer.onBehalfOf(alice).hashCode()
        }

        test("may act for the same id of another kind, because that is another caller") {
            val service = Principal(ALICE, PrincipalKind.Service)

            service.onBehalfOf(principal(ALICE)).isDelegated shouldBe true
        }
    }

    context("what is refused") {
        test("acting on behalf of oneself") {
            val failure = refusalOf { alice.onBehalfOf(principal(ALICE)) }

            failure.report.errors
                .single()
                .code shouldBe PrincipalViolationCodes.SELF_DELEGATION
        }

        test("acting for a principal of another tenant without being allowed to") {
            val here = principal(INDEXER) { tenant = ACME }

            val failure = refusalOf { here.onBehalfOf(principal(ALICE) { tenant = GLOBEX }) }

            val violation = failure.report.errors.single()
            violation.code shouldBe PrincipalViolationCodes.CROSS_TENANT_DELEGATION
            violation.path.toString() shouldBe "actingFor"
            violation.message shouldBe
                "must belong to the same tenant unless acting across tenants is allowed explicitly"
        }

        test("acting from within a tenant for a principal without one, and the other way round") {
            val inside = principal(INDEXER) { tenant = ACME }
            val outside = principal(ALICE)

            val inward = refusalOf { inside.onBehalfOf(outside) }
            val outward = refusalOf { outside.onBehalfOf(inside) }

            inward.report.errors.map { it.code } shouldContainExactly
                listOf(PrincipalViolationCodes.CROSS_TENANT_DELEGATION)
            outward.report.errors.map { it.code } shouldContainExactly
                listOf(PrincipalViolationCodes.CROSS_TENANT_DELEGATION)
        }

        test("acting across tenants in the builder without being allowed to") {
            val there = principal(ALICE) { tenant = GLOBEX }

            val failure = refusalOf {
                principal(INDEXER) {
                    tenant = ACME
                    actingFor(there)
                }
            }

            failure.report.errors
                .single()
                .code shouldBe PrincipalViolationCodes.CROSS_TENANT_DELEGATION
        }

        test("a chain that leads back to the principal at its start") {
            val failure = refusalOf { alice.onBehalfOf(indexer.onBehalfOf(alice)) }

            val violation = failure.report.errors.single()
            violation.code shouldBe PrincipalViolationCodes.DELEGATION_CYCLE
            violation.message shouldBe "must not lead back to a principal that is already in the chain"
        }

        test("a cycle that closes further down the chain") {
            val failure = refusalOf { alice.onBehalfOf(gateway.onBehalfOf(indexer.onBehalfOf(alice))) }

            failure.report.errors
                .single()
                .code shouldBe PrincipalViolationCodes.DELEGATION_CYCLE
        }

        test("every chain that names one caller twice, wherever the second one is") {
            checkAll(chainAndRepeat) { (length, repeated) ->
                val chain = chainOf(length)
                val again = chain.chain[repeated]

                val failure = refusalOf { Principal(again.id, again.kind).onBehalfOf(chain) }

                failure.report.errors.map { it.code } shouldContainExactly
                    listOf(PrincipalViolationCodes.DELEGATION_CYCLE)
            }
        }

        test("a chain longer than the bound") {
            val chain = chainOf(Limits.MAX_DELEGATION_CHAIN)

            val failure = refusalOf { principal("one-too-many").onBehalfOf(chain) }

            failure.report.errors
                .single()
                .code shouldBe PrincipalViolationCodes.DELEGATION_TOO_DEEP
            failure.report.errors
                .single()
                .message shouldBe "makes a chain of 9 principals, and at most 8 are allowed"
        }

        test("nothing about a chain of exactly the bound") {
            chainOf(Limits.MAX_DELEGATION_CHAIN).chain.size shouldBe Limits.MAX_DELEGATION_CHAIN
        }
    }

    context("rendering and equality") {
        test("the rendering names only the immediate caller, never the chain or its attributes") {
            val user = principal(ALICE) { attribute(name = "email", value = PLACEHOLDER, sensitive = false) }
            val delegated = indexer.onBehalfOf(user)

            delegated.toString() shouldBe "Principal(id=\"indexer\", kind=Service)"
            delegated.toString() shouldNotContain PLACEHOLDER
            delegated.toString() shouldNotContain ALICE
        }

        test("delegation takes part in equality") {
            indexer.onBehalfOf(alice) shouldBe indexer.onBehalfOf(alice)
            indexer.onBehalfOf(alice).hashCode() shouldBe indexer.onBehalfOf(alice).hashCode()
            (indexer.onBehalfOf(alice) == indexer) shouldBe false
        }
    }
})
