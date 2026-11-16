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

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.fixture.Empty
import com.davils.kore.core.arguments.report.fixture.Reported
import com.davils.kore.core.arguments.report.fixture.Secretive
import com.davils.kore.core.validation.report.KoreValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val MASK: String = "****"
private const val SECRET: String = "hunter2"
private const val BAD_PORT: String = "--port=eighty"
private const val TYPO: String = "--moed=production"
private const val PROBLEMS: Int = 3

private fun failureOf(vararg argv: String): KoreParameterException =
    shouldThrow<KoreParameterException> { Reported().load(argv.toList(), NO_ENVIRONMENT) }

class KoreParametersReportTest : FunSpec({
    context("a failed load") {
        test("produces one exception carrying all three problems at once") {
            failureOf(BAD_PORT, TYPO).violations shouldHaveSize PROBLEMS
        }

        test("is a validation exception whose report carries every problem") {
            val failure: KoreValidationException = failureOf(BAD_PORT, TYPO)

            failure.report.subject shouldBe "program parameters"
            failure.report.errors shouldHaveSize PROBLEMS
            failure.message shouldBe failure.report.render()
        }

        test("names every problem and leaves the parameter listing to its usage") {
            val failure = failureOf(BAD_PORT, TYPO)
            val report = failure.message

            report shouldContain "Invalid program parameters (3 problems):"
            report shouldContain "is not a valid integer"
            report shouldContain "did you mean --mode?"
            report shouldContain "APP_DATA_DIR"
            report shouldNotContain "Supported parameters:"
            failure.usage shouldContain "Supported parameters:"
        }

        test("does not report a single problem in the plural") {
            val failure = failureOf("--port=1")

            failure.violations shouldHaveSize 1
            failure.message shouldContain "(1 problem):"
        }

        test("never puts a secret into the report, not even when it is what failed") {
            val failure = shouldThrow<KoreParameterException> {
                Secretive().load(listOf("--db-port=$SECRET"), NO_ENVIRONMENT)
            }

            val report = failure.message
            report shouldNotContain SECRET
            report shouldContain MASK
        }
    }

    context("the usage listing") {
        test("shows names, types, defaults and variables") {
            val listing = Reported().usage()

            listing shouldContain "--port, -p <int>"
            listing shouldContain "The port to listen on."
            listing shouldContain "required"
            listing shouldContain "default: Production"
            listing shouldContain "env: APP_PORT"
            listing shouldContain "repeatable"
        }

        test("does not print the uninteresting default of a plain flag") {
            val listing = Reported().usage()

            listing shouldContain "--dry-run"
            listing shouldNotContain "default: false"
        }

        test("says so when a schema has no parameters") {
            Empty().usage() shouldBe "This program takes no parameters."
        }
    }

    context("describing the loaded values") {
        test("masks the secret and names where each value came from") {
            val parameters = Reported()

            parameters.load(listOf("--port=8080", "--db-password=$SECRET"), mapOf("APP_DATA_DIR" to "/srv/data"))
            val description = parameters.describe()

            description shouldNotContain SECRET
            description shouldContain "--db-password  $MASK"
            description shouldContain "8080 (from the command line)"
            description shouldContain "/srv/data (from APP_DATA_DIR)"
            description shouldContain "Production (default)"
        }

        test("says so when a schema has no parameters") {
            val parameters = Empty()

            parameters.load(emptyList(), NO_ENVIRONMENT)

            parameters.describe() shouldBe "This program takes no parameters."
        }
    }
})
