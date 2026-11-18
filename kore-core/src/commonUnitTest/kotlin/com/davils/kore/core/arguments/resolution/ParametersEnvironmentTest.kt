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

package com.davils.kore.core.arguments.resolution

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.ParameterException
import com.davils.kore.core.arguments.report.ParameterViolation
import com.davils.kore.core.arguments.report.ValueOrigin
import com.davils.kore.core.arguments.resolution.fixture.FakeVault
import com.davils.kore.core.arguments.resolution.fixture.Mandated
import com.davils.kore.core.arguments.resolution.fixture.Prefixed
import com.davils.kore.core.arguments.resolution.fixture.Sourced
import com.davils.kore.core.arguments.resolution.fixture.TypedSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val DATA_DIR: String = "APP_DATA_DIR"
private const val PORT: Int = 8080
private const val PORT_VARIABLE: String = "APP_PORT"
private const val TOKEN: String = "--token"
private const val FROM_VAULT: String = "from-vault"
private const val REGION: String = "eu-central"

private fun prefixed(vararg environment: Pair<String, String>): Prefixed {
    val parameters = Prefixed()
    parameters.load(emptyList(), environment.toMap())
    return parameters
}

class ParametersEnvironmentTest : FunSpec({
    context("a parameter fed from the environment") {
        test("falls back to the variable derived from its name") {
            prefixed(DATA_DIR to "/srv/data").dataDir shouldBe "/srv/data"
        }

        test("loses to the command line") {
            val parameters = Prefixed()

            parameters.load(listOf("--data-dir=/from/argv"), mapOf(DATA_DIR to "/from/env"))

            parameters.dataDir shouldBe "/from/argv"
        }

        test("reads an explicitly named variable instead of the derived one") {
            prefixed("VAULT_KEY" to "s3cret", "APP_SECRET_KEY" to "ignored").secretKey shouldBe "s3cret"
        }

        test("is not fed when it opted out") {
            prefixed("APP_LOCAL" to "value").local shouldBe null
        }

        test("can be a flag turned on") {
            prefixed("APP_EAGER" to "true").isEager shouldBe true
        }

        test("is converted like any other value") {
            prefixed(PORT_VARIABLE to "8080").port shouldBe PORT
        }

        test("is reported under the variable, not the parameter, when it is bad") {
            val failure = shouldThrow<ParameterException> {
                Prefixed().load(emptyList(), mapOf(PORT_VARIABLE to "eighty"))
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<ParameterViolation.ConversionFailure>()
            violation.origin.shouldBeInstanceOf<ValueOrigin.Environment>()
            violation.path.toString() shouldBe PORT_VARIABLE
        }

        test("is named by a missing required parameter") {
            val failure = shouldThrow<ParameterException> { Mandated().load(emptyList(), NO_ENVIRONMENT) }

            failure.violations.single().message shouldContain "APP_TOKEN"
        }
    }

    context("a value source") {
        test("supplies what the command line and the environment did not") {
            val parameters = Sourced()
            val vault = FakeVault(mapOf(TOKEN to FROM_VAULT, "--region" to REGION))

            parameters.load(listOf("--region=argv"), listOf(vault), NO_ENVIRONMENT)

            parameters.token shouldBe FROM_VAULT
            parameters.region shouldBe "argv"
        }

        test("is named when its value is described") {
            val parameters = Sourced()

            parameters.load(emptyList(), listOf(FakeVault(mapOf(TOKEN to FROM_VAULT))), NO_ENVIRONMENT)

            parameters.describe() shouldContain "$FROM_VAULT (from vault)"
        }

        test("is asked with the process environment when none is given") {
            val parameters = Sourced()

            parameters.load(emptyList(), listOf(FakeVault(mapOf("--region" to REGION))))

            parameters.region shouldBe REGION
        }

        test("loses to the environment") {
            val parameters = Sourced()

            parameters.load(
                emptyList(),
                listOf(FakeVault(mapOf(TOKEN to FROM_VAULT))),
                mapOf("APP_TOKEN" to "from-env"),
            )

            parameters.token shouldBe "from-env"
        }

        test("is named when a value it supplied is bad") {
            val failure = shouldThrow<ParameterException> {
                TypedSource().load(emptyList(), listOf(FakeVault(mapOf("--port" to "eighty"))), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<ParameterViolation.ConversionFailure>()
            violation.path.toString() shouldContain "vault"
        }
    }
})
