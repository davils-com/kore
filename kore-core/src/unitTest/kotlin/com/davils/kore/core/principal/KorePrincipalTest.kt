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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val AUDITOR: String = "auditor"
private const val READER: String = "reader"
private const val EXPORTER: String = "exporter"
private const val WRITER: String = "writer"
private const val ADMIN: String = "admin"
private const val ALICE: String = "alice"
private const val OUTER: String = "outer"
private const val INNER: String = "inner"
private const val ACME: String = "acme"
private const val GLOBEX: String = "globex"
private const val MOST_ROLES_ASKED: Int = 4

private val someRoles: Arb<String> = Arb.element(READER, EXPORTER, WRITER, ADMIN, AUDITOR)

class KorePrincipalTest : FunSpec({
    context("in a coroutine context") {
        test("is absent until somebody puts it there") {
            currentPrincipal().shouldBeNull()
        }

        test("is what the enclosing withPrincipal put there") {
            val seen = withPrincipal(principal(AUDITOR, READER)) { currentPrincipal() }

            seen shouldBe principal(AUDITOR, READER)
        }

        test("reaches a coroutine started with async inside it") {
            val seen = withPrincipal(principal(AUDITOR)) {
                coroutineScope { async { currentPrincipal()?.id }.await() }
            }

            seen shouldBe AUDITOR
        }

        test("reaches a coroutine started with launch inside it") {
            var seen: String? = null

            withPrincipal(principal(AUDITOR)) {
                coroutineScope { launch { seen = currentPrincipal()?.id } }
            }

            seen shouldBe AUDITOR
        }

        test("reaches a flow collected inside it") {
            val seen = withPrincipal(principal(AUDITOR)) {
                flow { emit(currentPrincipal()?.id) }.toList()
            }

            seen shouldContainExactly listOf(AUDITOR)
        }

        test("is gone again once the block is over") {
            withPrincipal(principal(AUDITOR)) { currentPrincipal() }

            currentPrincipal().shouldBeNull()
        }

        test("cannot be put there without withPrincipal, because a principal is no element of a context") {
            principal(AUDITOR).shouldNotBeInstanceOf<CoroutineContext.Element>()
        }

        test("shows no more than the id and the kind in the rendering of the context") {
            val caller = principal(ALICE) {
                tenant = ACME
                displayName = "Alice Example"
                attribute(name = "email", value = PLACEHOLDER, sensitive = false)
            }

            val rendered = withPrincipal(caller) { currentCoroutineContext().toString() }

            rendered shouldContain "KorePrincipal(id=\"alice\", kind=User)"
            rendered shouldNotContain "Alice Example"
            rendered shouldNotContain ACME
            rendered shouldNotContain PLACEHOLDER
        }
    }

    context("establishing one where another is established already") {
        test("refuses to replace a different principal unless it is told to") {
            withPrincipal(principal(OUTER)) {
                val failure = shouldThrow<IllegalStateException> {
                    withPrincipal(principal(INNER)) { currentPrincipal() }
                }

                failure.message shouldContain "replacing = true"
                currentPrincipal()?.id shouldBe OUTER
            }
        }

        test("refuses a principal that only differs in what it may do") {
            withPrincipal(principal(AUDITOR, READER)) {
                shouldThrow<IllegalStateException> { withPrincipal(principal(AUDITOR, READER, ADMIN)) { ADMIN } }
            }
        }

        test("lets the same principal be established again") {
            val seen = withPrincipal(principal(AUDITOR)) {
                withPrincipal(principal(AUDITOR)) { currentPrincipal()?.id }
            }

            seen shouldBe AUDITOR
        }

        test("replaces the outer one for its block only when it is told to") {
            withPrincipal(principal(OUTER)) {
                val inner = withPrincipal(principal(INNER), replacing = true) { currentPrincipal()?.id }

                inner shouldBe INNER
                currentPrincipal()?.id shouldBe OUTER
            }
        }
    }

    context("requiring one") {
        test("answers the principal in the context") {
            val seen = withPrincipal(principal(AUDITOR)) { requirePrincipal() }

            seen.id shouldBe AUDITOR
        }

        test("throws a dedicated exception naming the fix when there is none") {
            val failure = shouldThrow<KorePrincipalMissingException> { requirePrincipal() }

            failure.shouldBeInstanceOf<IllegalStateException>()
            failure.message shouldContain "withPrincipal(...)"
        }
    }

    context("roles") {
        test("answer for a single role") {
            val caller = principal(AUDITOR, READER, EXPORTER)

            caller.hasRole(READER) shouldBe true
            caller.hasRole(WRITER) shouldBe false
        }

        test("answer whether any of several is held") {
            val caller = principal(AUDITOR, READER)

            caller.hasAnyRole(WRITER, READER) shouldBe true
            caller.hasAnyRole(READER) shouldBe true
            caller.hasAnyRole(WRITER, ADMIN) shouldBe false
            caller.hasAnyRole(listOf(WRITER, READER)) shouldBe true
            caller.hasAnyRole(setOf(WRITER)) shouldBe false
        }

        test("answer no when asked whether any of no roles is held") {
            principal(AUDITOR, READER).hasAnyRole(emptyList()) shouldBe false
        }

        test("answer whether all of several are held") {
            val caller = principal(AUDITOR, READER, EXPORTER)

            caller.hasAllRoles(READER, EXPORTER) shouldBe true
            caller.hasAllRoles(READER) shouldBe true
            caller.hasAllRoles(WRITER, READER) shouldBe false
            caller.hasAllRoles(READER, WRITER) shouldBe false
            caller.hasAllRoles(listOf(READER, EXPORTER)) shouldBe true
            caller.hasAllRoles(setOf(READER, WRITER)) shouldBe false
        }

        test("refuse to say whether all of no roles are held, so an empty requirement never grants access") {
            val failure = shouldThrow<IllegalArgumentException> { principal(AUDITOR, READER).hasAllRoles(emptySet()) }

            failure.message shouldBe "At least one required role must be given"
        }

        test("answer like the set of held roles for every requirement that is not empty") {
            checkAll(
                genA = Arb.set(someRoles, 0..MOST_ROLES_ASKED),
                genB = Arb.set(someRoles, 1..MOST_ROLES_ASKED),
            ) { held, asked ->
                val caller = KorePrincipal(id = ALICE, roles = held)

                caller.hasAllRoles(asked) shouldBe held.containsAll(asked)
                caller.hasAnyRole(asked) shouldBe asked.any { role -> role in held }
            }
        }
    }

    context("tenants") {
        test("are the same when both principals name the same tenant") {
            val here = principal(ALICE) { tenant = ACME }

            here.sameTenantAs(principal(AUDITOR) { tenant = ACME }) shouldBe true
            here.sameTenantAs(principal(AUDITOR) { tenant = GLOBEX }) shouldBe false
        }

        test("are never the same when a principal names no tenant, not even for two without one") {
            val here = principal(ALICE) { tenant = ACME }

            KorePrincipal(ALICE).sameTenantAs(KorePrincipal(AUDITOR)) shouldBe false
            here.sameTenantAs(KorePrincipal(AUDITOR)) shouldBe false
            KorePrincipal(AUDITOR).sameTenantAs(here) shouldBe false
        }

        test("are compared with the tenant of a resource the same way") {
            val here = principal(ALICE) { tenant = ACME }

            here.sameTenantAs(ACME) shouldBe true
            here.sameTenantAs(GLOBEX) shouldBe false
            here.sameTenantAs(null) shouldBe false
            KorePrincipal(ALICE).sameTenantAs(null) shouldBe false
        }
    }

    context("stating one in a single call") {
        test("defaults to a user with nothing else known") {
            val caller = KorePrincipal(ALICE)

            caller.kind shouldBe KorePrincipalKind.User
            caller.roles shouldBe emptySet()
            caller.tenant.shouldBeNull()
            caller.displayName.shouldBeNull()
            caller.attributes shouldBe KorePrincipalAttributes.EMPTY
            caller.actingFor.shouldBeNull()
        }

        test("is equal to another with every field the same, and to nothing else") {
            KorePrincipal(id = "a", roles = setOf("x", "y")) shouldBe principal("a", "y", "x")
            KorePrincipal(id = "a", roles = setOf("x")).hashCode() shouldBe principal("a", "x").hashCode()
            principal("a", "x") shouldNotBe principal("a", "y")
            KorePrincipal("a", KorePrincipalKind.Service) shouldNotBe KorePrincipal("a")
            principal("a") { tenant = ACME } shouldNotBe principal("a") { tenant = GLOBEX }
            principal("a") { displayName = "A" } shouldNotBe KorePrincipal("a")
            principal("a") { attribute(name = "k", value = "v") } shouldNotBe KorePrincipal("a")
            KorePrincipal("a").equals("a") shouldBe false
        }

        test("keeps no reference to the set it was given") {
            val roles = mutableSetOf(READER)
            val caller = KorePrincipal(id = AUDITOR, roles = roles)

            roles += WRITER

            caller.roles shouldBe setOf(READER)
        }
    }

    context("rendering") {
        test("shows only the id and the kind, however much else is known") {
            val caller = principal(ALICE) {
                tenant = ACME
                displayName = "Alice"
                roles(WRITER, READER)
                attribute(name = "email", value = PLACEHOLDER, sensitive = false)
            }

            caller.toString() shouldBe "KorePrincipal(id=\"alice\", kind=User)"
        }

        test("quotes the id, so an id cannot pose as another field") {
            val caller = KorePrincipal("a\", kind=System")

            caller.toString() shouldBe """KorePrincipal(id="a\", kind=System", kind=User)"""
        }
    }

    context("the violation codes") {
        test("are stable identifiers in the kore namespace") {
            KorePrincipalViolationCodes.BLANK_ID.value shouldBe "kore.principal.blank-id"
            KorePrincipalViolationCodes.BLANK_ROLE.value shouldBe "kore.principal.blank-role"
            KorePrincipalViolationCodes.BLANK_TENANT.value shouldBe "kore.principal.blank-tenant"
            KorePrincipalViolationCodes.BLANK_DISPLAY_NAME.value shouldBe "kore.principal.blank-display-name"
            KorePrincipalViolationCodes.INVALID_ATTRIBUTE.value shouldBe "kore.principal.invalid-attribute"
            KorePrincipalViolationCodes.TOO_LONG.value shouldBe "kore.principal.too-long"
            KorePrincipalViolationCodes.INVALID_CHARACTER.value shouldBe "kore.principal.invalid-character"
            KorePrincipalViolationCodes.SURROUNDING_WHITESPACE.value shouldBe "kore.principal.surrounding-whitespace"
            KorePrincipalViolationCodes.TOO_MANY_ROLES.value shouldBe "kore.principal.too-many-roles"
            KorePrincipalViolationCodes.TOO_MANY_ATTRIBUTES.value shouldBe "kore.principal.too-many-attributes"
            KorePrincipalViolationCodes.SELF_DELEGATION.value shouldBe "kore.principal.self-delegation"
            KorePrincipalViolationCodes.DELEGATION_CYCLE.value shouldBe "kore.principal.delegation-cycle"
            KorePrincipalViolationCodes.CROSS_TENANT_DELEGATION.value shouldBe
                "kore.principal.cross-tenant-delegation"
            KorePrincipalViolationCodes.DELEGATION_TOO_DEEP.value shouldBe "kore.principal.delegation-too-deep"
        }
    }
})
