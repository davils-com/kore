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

package com.davils.kore.core.arguments.lexer

import com.davils.kore.core.arguments.KoreParameterLimits
import com.davils.kore.core.arguments.KoreUnknownArgumentPolicy
import com.davils.kore.core.arguments.fixture.Lenient
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.Strict
import com.davils.kore.core.arguments.fixture.Syntax
import com.davils.kore.core.arguments.lexer.fixture.Bundled
import com.davils.kore.core.arguments.lexer.fixture.FEWEST_BUNDLED_VALUES
import com.davils.kore.core.arguments.lexer.fixture.Unhelpful
import com.davils.kore.core.arguments.lexer.fixture.lexedFlag
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreValueOrigin
import com.davils.kore.core.arguments.report.fixture.Many
import com.davils.kore.core.arguments.resolution.SchemaOptions
import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import com.davils.kore.core.text.fixture.shouldBeSafeToShowLineByLine
import com.davils.kore.core.validation.violation.KoreSeverity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val CASES: Int = 200
private const val SECRET: String = "hunter2"
private const val THEIRS: String = "--theirs=$SECRET"
private const val NEAR_MISSES: Int = 20
private const val MANY_LETTERS: Int = 5_000

private fun suggestionsFor(failure: KoreParameterException): List<String?> =
    failure.violations.filterIsInstance<KoreParameterViolation.UnknownArgument>().map { it.suggestion }

class KoreParametersLexerHardeningTest : FunSpec({
    context("a token after a parameter that takes a value") {
        test("is taken as the value when it is a negative number in ASCII digits") {
            listOf("-5", "-1.5", "-2e3", "-.5").forEach { token -> looksLikeValue(token) shouldBe true }
        }

        test("is not taken as the value when it only resembles a number") {
            val lookalikes = listOf("-NaN", "-Infinity", "-0x1p3", "-1d", "-١٢", "-+5", "--5")

            lookalikes.forEach { token -> looksLikeValue(token) shouldBe false }
        }

        test("is not taken as the value when it is longer than any number") {
            looksLikeValue("-" + "1".repeat(KoreLimits.MAX_NUMERIC_TEXT_LENGTH)) shouldBe false
        }

        test("that is not a number in ASCII digits leaves the parameter without a value") {
            val failure = shouldThrow<KoreParameterException> {
                Syntax().load(listOf("--port", "-NaN"), NO_ENVIRONMENT)
            }

            failure.violations.first().shouldBeInstanceOf<KoreParameterViolation.MissingValue>()
        }
    }

    context("unknown arguments that are collected") {
        test("are handed over as given, while only their names are listed apart") {
            val parameters = Lenient()

            parameters.load(listOf(THEIRS, "-p$SECRET", "stray", "--name=kore"), NO_ENVIRONMENT)

            parameters.unknown shouldContainExactly listOf(THEIRS, "-p$SECRET", "stray")
            parameters.unknownNames shouldContainExactly listOf("--theirs", "-p")
        }

        test("are never shown by the warnings or the description") {
            val parameters = Lenient()

            parameters.load(listOf(THEIRS), NO_ENVIRONMENT)

            parameters.warnings.toString() shouldNotContain SECRET
            parameters.describe() shouldNotContain SECRET
        }
    }

    context("suggestions for unknown arguments") {
        test("are searched for the first sixteen unknown arguments only") {
            val failure = shouldThrow<KoreParameterException> {
                Many().load(List(NEAR_MISSES) { "--moed" }, NO_ENVIRONMENT)
            }

            val suggestions = suggestionsFor(failure)
            suggestions.take(KoreLimits.MAX_SUGGESTED_ARGUMENTS).toSet() shouldBe setOf("--mode")
            suggestions.drop(KoreLimits.MAX_SUGGESTED_ARGUMENTS).toSet() shouldBe setOf(null)
        }

        test("are not searched at all when the schema allows none") {
            val failure = shouldThrow<KoreParameterException> { Unhelpful().load(listOf("--moed"), NO_ENVIRONMENT) }

            suggestionsFor(failure) shouldBe listOf(null)
        }
    }

    context("a hostile unknown argument") {
        test("is shown safely in the report and in the violation") {
            checkAll(CASES, hostileText) { hostile ->
                val failure = shouldThrow<KoreParameterException> {
                    Strict().load(listOf("--x$hostile", "-$hostile"), NO_ENVIRONMENT)
                }

                failure.message.shouldBeSafeToShowLineByLine()
                failure.violations.forEach { violation -> violation.toString().shouldBeSafeToShow() }
            }
        }
    }

    context("a parameter given more often than it takes values") {
        test("keeps at most one occurrence beyond the limit while the command line is read") {
            val limits = KoreParameterLimits(maxValuesPerParameter = FEWEST_BUNDLED_VALUES)

            val given = lexedFlag(limits, listOf("-" + "v".repeat(MANY_LETTERS), "--verbose"))

            given.raws.size shouldBe FEWEST_BUNDLED_VALUES + 1
            given.occurrences shouldBe MANY_LETTERS + 1
        }

        test("is refused with how often it was given in full") {
            val failure = shouldThrow<KoreParameterException> {
                Bundled().load(listOf("-" + "v".repeat(MANY_LETTERS)), NO_ENVIRONMENT)
            }

            val repeated = failure.violations.filterIsInstance<KoreParameterViolation.Repeated>().single()
            repeated.occurrences shouldBe MANY_LETTERS
            failure.violations.filterIsInstance<KoreParameterViolation.TooManyValues>().single()
        }

        test("in bundles that fill the default limits is refused without running out of memory") {
            val bundle = "-" + "v".repeat(KoreLimits.MAX_ARGUMENT_LENGTH - 1)
            val argv = List(KoreLimits.MAX_ARGUMENTS) { bundle }

            val failure = shouldThrow<KoreParameterException> { Syntax().load(argv, NO_ENVIRONMENT) }

            val repeated = failure.violations.filterIsInstance<KoreParameterViolation.Repeated>().single()
            repeated.occurrences shouldBe KoreLimits.MAX_ARGUMENTS * (KoreLimits.MAX_ARGUMENT_LENGTH - 1)
        }
    }

    context("the internal state of the lexer") {
        test("never counts fewer occurrences than it keeps") {
            shouldThrow<IllegalArgumentException> {
                RawValues(listOf(SECRET, SECRET), KoreValueOrigin.CommandLine, occurrences = 1)
            }
        }

        test("never prints the raw values it holds") {
            val options = SchemaOptions(
                unknownArguments = KoreUnknownArgumentPolicy.Collect,
                limits = KoreParameterLimits(),
                sensitiveOnCommandLine = KoreSeverity.Warning,
            )
            val lexed = ArgumentLexer(emptyList(), options).lex(listOf(THEIRS, "--", SECRET))
            val raw = RawValues(listOf(SECRET), KoreValueOrigin.CommandLine)

            lexed.toString() shouldNotContain SECRET
            raw.toString() shouldNotContain SECRET
        }
    }
})
