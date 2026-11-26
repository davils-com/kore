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

package com.davils.kore.core

import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.checkAll

private const val TOKEN: String = "API_TOKEN"
private const val SECRET: String = "s3cr3t-value"

private val variables: Map<String, String> = mapOf(TOKEN to SECRET, "HOME" to "/home/core")

class EnvironmentTest : FunSpec({
    context("a masked environment") {
        test("gives every value by its name") {
            val environment = MaskedEnvironment(variables)

            environment[TOKEN] shouldBe SECRET
            environment.containsKey("HOME") shouldBe true
            environment.containsKey("MISSING") shouldBe false
            environment shouldBe variables
        }

        test("shows names but never values") {
            val environment = MaskedEnvironment(variables)

            environment.toString() shouldBe "{API_TOKEN=****, HOME=****}"
            environment.entries.toString() shouldNotContain SECRET
            val entries = environment.entries
            val token = entries.first { entry -> entry.key == TOKEN }

            token.toString() shouldBe "API_TOKEN=****"
        }

        test("escapes hostile names") {
            checkAll(hostileText) { name ->
                val environment = MaskedEnvironment(mapOf(name to SECRET))

                environment.toString().shouldBeSafeToShow()
            }
        }

        test("compares entries like any other map") {
            val masked = MaskedEnvironment(variables).entries
            val entry = masked.first { candidate -> candidate.key == TOKEN }
            val plain = variables.entries.first { candidate -> candidate.key == TOKEN }

            (entry == plain) shouldBe true
            entry.hashCode() shouldBe plain.hashCode()
            entry.equals(TOKEN) shouldBe false
        }

        test("cannot be changed by a caller") {
            val environment: Map<String, String> = MaskedEnvironment(variables)

            shouldThrowAny { (environment as MutableMap<String, String>).clear() }
        }
    }

    context("a single variable of the process") {
        test("is null when it is not set") {
            processVariable("KORE_SURELY_UNSET_VARIABLE_4F2A") shouldBe null
        }

        test("refuses a name that no platform allows, without repeating it") {
            val refused = listOf("", "A=B", "A\u0000B", "N".repeat(Limits.MAX_NAME_LENGTH + 1))

            refused.forEach { name ->
                val failure = shouldThrow<IllegalArgumentException> { processVariable(name) }

                failure.message.orEmpty() shouldNotContain "A=B"
            }
        }

        test("accepts a name of the longest length") {
            processVariable("N".repeat(Limits.MAX_NAME_LENGTH)) shouldBe null
        }
    }

    context("the process environment") {
        test("is masked") {
            val environment = processEnvironment()

            environment.values.forEach { value ->
                if (value.length > 1) environment.toString() shouldNotContain "=$value"
            }
        }
    }
})
