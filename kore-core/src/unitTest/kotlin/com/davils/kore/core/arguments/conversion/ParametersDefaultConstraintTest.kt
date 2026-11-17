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

package com.davils.kore.core.arguments.conversion

import com.davils.kore.core.arguments.conversion.fixture.ExistingDirectory
import com.davils.kore.core.arguments.conversion.fixture.InsideTheChoice
import com.davils.kore.core.arguments.conversion.fixture.MissingDirectory
import com.davils.kore.core.arguments.conversion.fixture.MissingFile
import com.davils.kore.core.arguments.conversion.fixture.OutsideTheChoice
import com.davils.kore.core.arguments.conversion.fixture.SecretChoice
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.RepeatedChoices
import com.davils.kore.core.arguments.report.ParameterException
import com.davils.kore.core.arguments.report.ParameterViolation
import com.davils.kore.core.arguments.report.ParameterViolationCodes
import com.davils.kore.core.arguments.report.ValueOrigin
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val NOT_ONE_OF: String = "'medium' is not one of low, high"
private const val MISSING_NAME: String = "kore-default-that-does-not-exist"
private const val DOES_NOT_EXIST: String = "does not exist"

private fun failureOf(block: () -> Unit): ParameterViolation.ConversionFailure {
    val failure = shouldThrow<ParameterException> { block() }
    return failure.violations.single().shouldBeInstanceOf<ParameterViolation.ConversionFailure>()
}

class ParametersDefaultConstraintTest : FunSpec({
    context("a choice") {
        test("refuses a default that is not one of its choices, under the default's origin") {
            val violation = failureOf { OutsideTheChoice().load(emptyList(), NO_ENVIRONMENT) }

            violation.origin shouldBe ValueOrigin.Default
            violation.code shouldBe ParameterViolationCodes.CONVERSION_FAILED
            violation.path.toString() shouldBe "--level (default)"
            violation.message shouldBe NOT_ONE_OF
        }

        test("accepts a default that is one of its choices") {
            val parameters = InsideTheChoice()

            parameters.load(emptyList(), NO_ENVIRONMENT)

            parameters.level shouldBe "low"
        }

        test("does not consult the default when a value is given") {
            val parameters = OutsideTheChoice()

            parameters.load(listOf("--level=high"), NO_ENVIRONMENT)

            parameters.level shouldBe "high"
        }

        test("checks every element of a repeated parameter's default") {
            failureOf { RepeatedChoices().load(emptyList(), NO_ENVIRONMENT) }.message shouldBe NOT_ONE_OF
        }

        test("masks a sensitive default it refuses") {
            val violation = failureOf { SecretChoice().load(emptyList(), NO_ENVIRONMENT) }

            violation.message shouldNotContain "platinum-placeholder"
            violation.value shouldBe "****"
        }
    }

    context("a path") {
        test("refuses a default that does not exist when it must") {
            val directory = tempdir().toPath()
            val missing = directory.resolve(MISSING_NAME)

            val violation = failureOf { MissingDirectory(missing).load(emptyList(), NO_ENVIRONMENT) }

            violation.origin shouldBe ValueOrigin.Default
            violation.reason shouldBe DOES_NOT_EXIST
        }

        test("accepts a default that satisfies every check") {
            val existing = tempdir().toPath()
            val parameters = ExistingDirectory(existing)

            parameters.load(emptyList(), NO_ENVIRONMENT)

            parameters.dataDir shouldBe existing
        }

        test("still checks a value that is given") {
            val directory = tempdir().toPath()
            val missing = directory.resolve(MISSING_NAME)

            val violation = failureOf {
                ExistingDirectory(
                    directory,
                ).load(listOf("--data-dir=$missing"), NO_ENVIRONMENT)
            }

            violation.origin shouldBe ValueOrigin.CommandLine
            violation.reason shouldBe DOES_NOT_EXIST
        }
    }

    context("a file") {
        test("refuses a default that does not exist when it must") {
            val missing = tempdir().resolve(MISSING_NAME)

            failureOf { MissingFile(missing).load(emptyList(), NO_ENVIRONMENT) }.reason shouldBe DOES_NOT_EXIST
        }

        test("still converts a given value to a file") {
            val directory = tempdir()
            val parameters = MissingFile(directory.resolve(MISSING_NAME))

            parameters.load(listOf("--config=$directory"), NO_ENVIRONMENT)

            parameters.config shouldBe directory
        }
    }

    context("reported alongside everything else") {
        test("a bad default does not hide a bad argument") {
            val failure = shouldThrow<ParameterException> {
                OutsideTheChoice().load(listOf("--unknown"), NO_ENVIRONMENT)
            }

            failure.violations.map { it.code } shouldContainExactly listOf(
                ParameterViolationCodes.UNKNOWN_ARGUMENT,
                ParameterViolationCodes.CONVERSION_FAILED,
            )
        }
    }
})
