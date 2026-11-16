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

import com.davils.kore.core.arguments.fixture.FEWEST_ARGUMENTS
import com.davils.kore.core.arguments.fixture.FEWEST_VALUES
import com.davils.kore.core.arguments.fixture.FixedSource
import com.davils.kore.core.arguments.fixture.Limited
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.SHORTEST_ARGUMENT
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreParameterViolationCodes
import com.davils.kore.core.arguments.report.KoreValueOrigin
import com.davils.kore.core.limit.KoreLimits
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

private fun refusalOf(vararg argv: String): KoreParameterViolation =
    shouldThrow<KoreParameterException> { Limited().load(argv.toList(), NO_ENVIRONMENT) }.violations.single()

class KoreParametersLimitsTest : FunSpec({
    context("the limits of a schema") {
        test("default to the limits of the library") {
            val limits = KoreParameterLimits()

            limits.maxArguments shouldBe KoreLimits.MAX_ARGUMENTS
            limits.maxArgumentLength shouldBe KoreLimits.MAX_ARGUMENT_LENGTH
            limits.maxValuesPerParameter shouldBe KoreLimits.MAX_VALUES_PER_PARAMETER
            limits.maxSuggestedArguments shouldBe KoreLimits.MAX_SUGGESTED_ARGUMENTS
        }

        test("that admit nothing at all are refused") {
            shouldThrow<IllegalArgumentException> { KoreParameterLimits(maxArguments = 0) }
            shouldThrow<IllegalArgumentException> { KoreParameterLimits(maxArgumentLength = 0) }
            shouldThrow<IllegalArgumentException> { KoreParameterLimits(maxValuesPerParameter = 0) }
            shouldThrow<IllegalArgumentException> { KoreParameterLimits(maxSuggestedArguments = -1) }
        }

        test("are equal when every limit is") {
            KoreParameterLimits(maxArguments = 1) shouldBe KoreParameterLimits(maxArguments = 1)
            KoreParameterLimits(maxArguments = 1).hashCode() shouldBe KoreParameterLimits(maxArguments = 1).hashCode()
            KoreParameterLimits(maxArguments = 1) shouldNotBe KoreParameterLimits(maxArguments = 2)
            KoreParameterLimits().toString() shouldContain "maxArguments=4096"
        }
    }

    context("more arguments than the schema reads") {
        test("are refused with a single violation before any is read") {
            val violation = refusalOf(NAME_A, "--name=b", "stray", "--unknown", "--item=x")

            violation.shouldBeInstanceOf<KoreParameterViolation.TooManyArguments>()
            violation.code shouldBe KoreParameterViolationCodes.TOO_MANY_ARGUMENTS
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
            val failure = shouldThrow<KoreParameterException> {
                Limited().load(listOf(NAME_A, "--secret=$SECRET"), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.ArgumentTooLong>()
            violation.code shouldBe KoreParameterViolationCodes.TOO_LONG
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
            val failure = shouldThrow<KoreParameterException> {
                Limited().load(emptyList(), mapOf("APP_SECRET" to SECRET))
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.ValueTooLong>()
            violation.origin shouldBe KoreValueOrigin.Environment("APP_SECRET")
            violation.limit shouldBe SHORTEST_ARGUMENT
            failure.message shouldNotContain SECRET
        }

        test("from a value source is refused under the source") {
            val failure = shouldThrow<KoreParameterException> {
                Limited().load(emptyList(), listOf(FixedSource(name = "vault", value = SECRET)), NO_ENVIRONMENT)
            }

            val tooLong = failure.violations.filterIsInstance<KoreParameterViolation.ValueTooLong>()
            tooLong.map { it.origin }.toSet() shouldBe setOf(KoreValueOrigin.External("vault"))
            failure.message shouldNotContain SECRET
        }
    }

    context("more values than a parameter takes") {
        test("are refused when one argument splits into too many") {
            val violation = refusalOf("--tag=a,b,c,d")

            violation.shouldBeInstanceOf<KoreParameterViolation.TooManyValues>()
            violation.code shouldBe KoreParameterViolationCodes.TOO_MANY_VALUES
            violation.limit shouldBe FEWEST_VALUES
        }

        test("are refused when a parameter is repeated too often, rather than cut short") {
            val violation = refusalOf("--item=a", "--item=b", "--item=c", "--item=d")

            violation.shouldBeInstanceOf<KoreParameterViolation.TooManyValues>()
        }

        test("are refused when a parameter is given more often than the limit, even with nothing to split") {
            val violation = refusalOf(EMPTY_TAG, EMPTY_TAG, EMPTY_TAG, EMPTY_TAG)

            violation.shouldBeInstanceOf<KoreParameterViolation.TooManyValues>()
            violation.origin shouldBe KoreValueOrigin.CommandLine
        }

        test("are read when they are exactly as many as the limit") {
            val parameters = Limited()

            parameters.load(listOf("--tag=a,b,,c,"), NO_ENVIRONMENT)

            parameters.tags shouldContainExactly listOf("a", "b", "c")
        }
    }
})
