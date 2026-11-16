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

package com.davils.kore.core.arguments.report

import com.davils.kore.core.arguments.report.fixture.FromEnvironment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val VARIABLE: String = "APP_PORT"
private const val VAULT: String = "vault"

class KoreValueOriginTest : FunSpec({
    context("an origin") {
        test("from the environment is equal to another naming the same variable") {
            KoreValueOrigin.Environment(VARIABLE) shouldBe KoreValueOrigin.Environment(VARIABLE)
            KoreValueOrigin.Environment(VARIABLE).hashCode() shouldBe KoreValueOrigin.Environment(VARIABLE).hashCode()
        }

        test("from an external source is equal to another naming the same source and to no other") {
            KoreValueOrigin.External(VAULT) shouldBe KoreValueOrigin.External(VAULT)
            KoreValueOrigin.External(VAULT) shouldNotBe KoreValueOrigin.External("consul")
        }

        test("names what it describes when printed") {
            KoreValueOrigin.Environment(VARIABLE).toString() shouldContain VARIABLE
            KoreValueOrigin.External(VAULT).toString() shouldContain VAULT
        }

        test("that is reported can be compared with one the caller builds") {
            val failure = shouldThrow<KoreParameterException> {
                FromEnvironment().load(emptyList(), mapOf(VARIABLE to "eighty"))
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            violation.origin shouldBe KoreValueOrigin.Environment(VARIABLE)
        }
    }
})
