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

package com.davils.kore.core.arguments.declaration

import com.davils.kore.core.arguments.declaration.fixture.Defaults
import com.davils.kore.core.arguments.declaration.fixture.ServerParameters
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.lines
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val PORT: Int = 8080
private const val SPLIT: String = "split"
private const val SENSITIVE: String = "sensitive"
private const val VALIDATE: String = "validate"
private const val NEGATABLE: String = "negatable"

private fun methodsOf(type: Class<*>): List<String> = type.methods.map { method -> method.name }

class KoreParameterDeclarationTest : FunSpec({
    context("a schema declared with one function per kind") {
        test("reads every kind with its default where nothing was given") {
            val parameters = ServerParameters()

            parameters.load(listOf("-p", "8080", "--tag", "a,b", "--no-dry-run"), NO_ENVIRONMENT)

            parameters.port shouldBe PORT
            parameters.host shouldBe "0.0.0.0"
            parameters.token.shouldBeNull()
            parameters.tags shouldContainExactly listOf("a", "b")
            parameters.isDryRun shouldBe false
        }

        test("reads a required value from the variable its prefix derives") {
            val parameters = ServerParameters()

            parameters.load(emptyList(), mapOf("KORE_PORT" to "8080"))

            parameters.port shouldBe PORT
        }

        test("applies the groups declared in its initialiser") {
            val failure = shouldThrow<KoreParameterException> {
                ServerParameters().load(listOf("--port=1", "--token=t", "--tag=a"), NO_ENVIRONMENT)
            }

            failure.report.errors shouldHaveSize 1
            failure.violations.filterIsInstance<KoreParameterViolation.MutuallyExclusive>() shouldHaveSize 1
        }

        test("lists every declaration in the usage") {
            ServerParameters().usage() shouldBe lines(
                "Supported parameters:",
                "  --port, -p <int>  Port the control plane listens on  required  env: KORE_PORT",
                "  --host <string>   default: 0.0.0.0  env: KORE_HOST",
                "  --token <string>  env: KORE_TOKEN",
                "  --tag <string>    repeatable  env: KORE_TAG",
                "  --dry-run         env: KORE_DRY_RUN",
            )
        }
    }

    context("defaults") {
        test("are listed in the usage, masked where the parameter is sensitive") {
            Defaults().usage() shouldBe lines(
                "Supported parameters:",
                "  --name <string>       repeatable  default: [alpha, beta]",
                "  --secret <string>     repeatable  default: ****",
                "  --loud                default: true",
                "  --described <string>  Says what it is  default: plain",
                "  --raw <value>",
                "  --number <int>        repeatable",
            )
        }

        test("are described with where they came from") {
            val parameters = Defaults()

            parameters.load(listOf("--raw=abc"), NO_ENVIRONMENT)

            parameters.describe() shouldBe lines(
                "Parameters:",
                "  --name       [alpha, beta] (default)",
                "  --secret     **** (default)",
                "  --loud       true (default)",
                "  --described  plain (default)",
                "  --raw        cba (from the command line)",
                "  --number     [] (default)",
            )
        }

        test("leave an optional parameter that was not given without an origin") {
            val parameters = Defaults()

            parameters.load(emptyList(), NO_ENVIRONMENT)

            parameters.raw.shouldBeNull()
            parameters.describe().lines() shouldContain "  --raw        null"
        }
    }

    context("the configuration of each kind") {
        test("offers split only for a repeatable parameter") {
            methodsOf(KoreMultipleConfiguration::class.java) shouldContain SPLIT
            methodsOf(KoreValueConfiguration::class.java) shouldNotContain SPLIT
            methodsOf(KoreFlagConfiguration::class.java) shouldNotContain SPLIT
        }

        test("offers negatable only for a flag") {
            methodsOf(KoreFlagConfiguration::class.java) shouldContain NEGATABLE
            methodsOf(KoreValueConfiguration::class.java) shouldNotContain NEGATABLE
            methodsOf(KoreMultipleConfiguration::class.java) shouldNotContain NEGATABLE
        }

        test("offers no secrecy and no validation for a flag") {
            methodsOf(KoreFlagConfiguration::class.java) shouldNotContain SENSITIVE
            methodsOf(KoreFlagConfiguration::class.java) shouldNotContain VALIDATE
            methodsOf(KoreValueConfiguration::class.java) shouldContain SENSITIVE
            methodsOf(KoreValueConfiguration::class.java) shouldContain VALIDATE
        }
    }
})
