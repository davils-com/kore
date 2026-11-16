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
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreParameterViolationCodes
import com.davils.kore.core.arguments.resolution.fixture.Ageing
import com.davils.kore.core.arguments.resolution.fixture.Suspicious
import com.davils.kore.core.validation.violation.KoreSeverity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PORT: String = "--port=8080"
private const val OLD_DIR: String = "--old-dir=/srv"
private const val ADVICE: String = "is deprecated: use --data-dir instead"
private const val MANY_THREADS: Int = 512

private fun ageing(vararg argv: String): Ageing {
    val parameters = Ageing()
    parameters.load(argv.toList(), NO_ENVIRONMENT)
    return parameters
}

class KoreParametersWarningTest : FunSpec({
    context("a deprecated parameter") {
        test("that is not given says nothing") {
            ageing(PORT).warnings shouldHaveSize 0
        }

        test("that is given still loads and still yields its value") {
            val parameters = ageing(PORT, OLD_DIR)

            parameters.oldDir shouldBe "/srv"
            parameters.isLoaded shouldBe true
        }

        test("that is given reports a warning with its advice") {
            val warning = ageing(PORT, OLD_DIR).warnings.single()

            warning.severity shouldBe KoreSeverity.Warning
            warning.code shouldBe KoreParameterViolationCodes.DEPRECATED
            warning.path.toString() shouldBe "--old-dir"
            warning.message shouldBe ADVICE
        }

        test("without advice says only that") {
            ageing(PORT, "--legacy").warnings.single().message shouldBe "is deprecated"
        }

        test("fed from the environment warns as well") {
            val parameters = Ageing()

            parameters.load(listOf(PORT), mapOf("APP_OLD_DIR" to "/srv"))

            parameters.warnings shouldHaveSize 1
        }

        test("is marked in the usage listing") {
            Ageing().usage() shouldContain "deprecated"
        }
    }

    context("a failed load") {
        test("shows warnings in their own block") {
            val failure = shouldThrow<KoreParameterException> { Ageing().load(listOf(OLD_DIR), NO_ENVIRONMENT) }

            failure.report.errors shouldHaveSize 1
            failure.report.warnings shouldHaveSize 1
            val message = failure.message
            message shouldContain "Invalid program parameters (1 problem):"
            message shouldContain "1 warning:"
            message shouldContain ADVICE
        }
    }

    context("a rule that only warns") {
        test("leaves the value usable") {
            val parameters = Suspicious()

            parameters.load(listOf("--threads=512"), NO_ENVIRONMENT)

            parameters.threads shouldBe MANY_THREADS
            parameters.warnings.single().message shouldContain "unusually high"
        }

        test("is reported beside a rule that fails") {
            val failure = shouldThrow<KoreParameterException> {
                Suspicious().load(
                    listOf("--threads=-1"),
                    NO_ENVIRONMENT,
                )
            }

            val warning = failure.violations.single { it.severity == KoreSeverity.Warning }
            warning.shouldBeInstanceOf<KoreParameterViolation.ValidationFailure>()
            failure.report.errors shouldHaveSize 1
        }
    }
})
