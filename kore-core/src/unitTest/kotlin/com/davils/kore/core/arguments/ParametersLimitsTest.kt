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

package com.davils.kore.core.arguments

import com.davils.kore.core.Limits
import com.davils.kore.core.arguments.fixture.FEWEST_ARGUMENTS
import com.davils.kore.core.arguments.fixture.FEWEST_VALUES
import com.davils.kore.core.arguments.fixture.FixedSource
import com.davils.kore.core.arguments.fixture.Limited
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.SHORTEST_ARGUMENT
import com.davils.kore.core.arguments.report.ParameterException
import com.davils.kore.core.arguments.report.ParameterViolation
import com.davils.kore.core.arguments.report.ParameterViolationCodes
import com.davils.kore.core.arguments.report.ValueOrigin
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val SECRET: String = "hunter2-hunter2-hunter2"
private const val NAME_A: String = "--name=a"
private const val EMPTY_TAG: String = "--tag="

private fun refusalOf(vararg argv: String): ParameterViolation =
    shouldThrow<ParameterException> { Limited().load(argv.toList(), NO_ENVIRONMENT) }.violations.single()

class ParametersLimitsTest : FunSpec({
    context("the limits of a schema") {
        test("default to the limits of the library") {
            val limits = ParameterLimits()

            limits.maxArguments shouldBe Limits.MAX_ARGUMENTS
            limits.maxArgumentLength shouldBe Limits.MAX_ARGUMENT_LENGTH
            limits.maxValuesPerParameter shouldBe Limits.MAX_VALUES_PER_PARAMETER
            limits.maxSuggestedArguments shouldBe Limits.MAX_SUGGESTED_ARGUMENTS
        }

        test("that admit nothing at all are refused") {
            shouldThrow<IllegalArgumentException> { ParameterLimits(maxArguments = 0) }
            shouldThrow<IllegalArgumentException> { ParameterLimits(maxArgumentLength = 0) }
            shouldThrow<IllegalArgumentException> { ParameterLimits(maxValuesPerParameter = 0) }
            shouldThrow<IllegalArgumentException> { ParameterLimits(maxSuggestedArguments = -1) }
        }

        test("are equal when every limit is") {
            ParameterLimits(maxArguments = 1) shouldBe ParameterLimits(maxArguments = 1)
            ParameterLimits(maxArguments = 1).hashCode() shouldBe ParameterLimits(maxArguments = 1).hashCode()
            ParameterLimits(maxArguments = 1) shouldNotBe ParameterLimits(maxArguments = 2)
            ParameterLimits().toString() shouldContain "maxArguments=4096"
        }
    }

    context("more arguments than the schema reads") {
        test("are refused with a single violation before any is read") {
            val violation = refusalOf(NAME_A, "--name=b", "stray", "--unknown", "--item=x")

            violation.shouldBeInstanceOf<ParameterViolation.TooManyArguments>()
            violation.code shouldBe ParameterViolationCodes.TOO_MANY_ARGUMENTS
            violation.count shouldBe FEWEST_ARGUMENTS + 1
            violation.limit shouldBe FEWEST_ARGUMENTS
        }

        test("are read when they are exactly as many as the limit") {
            val parameters = Limited()

            parameters.load(listOf(NAME_A, "--item=x", "--item=y", "--tag=z"), NO_ENVIRONMENT)

            parameters.items shouldContainExactly listOf("x", "y")
        }
    }

    context("an argument longer than the schema reads") {
        test("is refused by its position without being repeated") {
            val failure = shouldThrow<ParameterException> {
                Limited().load(listOf(NAME_A, "--secret=$SECRET"), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<ParameterViolation.ArgumentTooLong>()
            violation.code shouldBe ParameterViolationCodes.TOO_LONG
            violation.position shouldBe 2
            violation.path.toString() shouldBe "argument 2"
            failure.message shouldNotContain SECRET
        }

        test("after the terminator is handed over untouched") {
            val parameters = Limited()

            parameters.load(listOf("--", SECRET), NO_ENVIRONMENT)

            parameters.rest shouldContainExactly listOf(SECRET)
        }
    }

    context("a value longer than the schema reads") {
        test("from the environment is refused under its variable") {
            val failure = shouldThrow<ParameterException> {
                Limited().load(emptyList(), mapOf("APP_SECRET" to SECRET))
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<ParameterViolation.ValueTooLong>()
            violation.origin shouldBe ValueOrigin.Environment("APP_SECRET")
            violation.limit shouldBe SHORTEST_ARGUMENT
            failure.message shouldNotContain SECRET
        }

        test("from a value source is refused under the source") {
            val failure = shouldThrow<ParameterException> {
                Limited().load(emptyList(), listOf(FixedSource(name = "vault", value = SECRET)), NO_ENVIRONMENT)
            }

            val tooLong = failure.violations.filterIsInstance<ParameterViolation.ValueTooLong>()
            tooLong.map { it.origin }.toSet() shouldBe setOf(ValueOrigin.External("vault"))
            failure.message shouldNotContain SECRET
        }
    }

    context("more values than a parameter takes") {
        test("are refused when one argument splits into too many") {
            val violation = refusalOf("--tag=a,b,c,d")

            violation.shouldBeInstanceOf<ParameterViolation.TooManyValues>()
            violation.code shouldBe ParameterViolationCodes.TOO_MANY_VALUES
            violation.limit shouldBe FEWEST_VALUES
        }

        test("are refused when a parameter is repeated too often, rather than cut short") {
            val violation = refusalOf("--item=a", "--item=b", "--item=c", "--item=d")

            violation.shouldBeInstanceOf<ParameterViolation.TooManyValues>()
        }

        test("are refused when a parameter is given more often than the limit, even with nothing to split") {
            val violation = refusalOf(EMPTY_TAG, EMPTY_TAG, EMPTY_TAG, EMPTY_TAG)

            violation.shouldBeInstanceOf<ParameterViolation.TooManyValues>()
            violation.origin shouldBe ValueOrigin.CommandLine
        }

        test("are read when they are exactly as many as the limit") {
            val parameters = Limited()

            parameters.load(listOf("--tag=a,b,,c,"), NO_ENVIRONMENT)

            parameters.tags shouldContainExactly listOf("a", "b", "c")
        }
    }
})
