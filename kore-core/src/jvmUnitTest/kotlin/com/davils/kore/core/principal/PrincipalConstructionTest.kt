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

import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

private const val ALICE: String = "alice"
private const val READER: String = "reader"

private val publicConstructors: List<Constructor<*>> =
    Principal::class.java.declaredConstructors.filter { constructor -> Modifier.isPublic(constructor.modifiers) }

private fun createdThrough(constructor: Constructor<*>, id: String, roles: Set<String>): Any {
    val given = listOf(
        CallerIdentity(id = id, kind = PrincipalKind.User, tenant = null),
        roles,
        null,
        PrincipalAttributes.EMPTY,
        null,
        false,
        false,
    )
    val arguments = given + List(constructor.parameterCount - given.size) { null }
    val create: (Array<out Any?>) -> Any = constructor::newInstance
    return create(arguments.toTypedArray())
}

class PrincipalConstructionTest : FunSpec({
    context("a principal created through a constructor that Kotlin makes public in the bytecode") {
        test("is validated all the same, so no invalid principal can exist") {
            publicConstructors.forEach { constructor ->
                val failure = shouldThrow<InvocationTargetException> { createdThrough(constructor, " ", setOf(READER)) }

                failure.cause.shouldBeInstanceOf<ValidationException>()
            }
        }

        test("keeps a copy of the roles, so a later change to the given set does not reach it") {
            publicConstructors.forEach { constructor ->
                val roles = mutableSetOf(READER)
                val caller = createdThrough(constructor, ALICE, roles).shouldBeInstanceOf<Principal>()

                roles += " admin"

                caller.roles shouldBe setOf(READER)
            }
        }
    }

    context("a set of attributes") {
        test("has no public constructor at all, so it is created only through its validating functions") {
            val constructors = PrincipalAttributes::class.java.declaredConstructors
            val reachable = constructors.filter { constructor -> Modifier.isPublic(constructor.modifiers) }

            reachable.shouldBeEmpty()
        }
    }
})
