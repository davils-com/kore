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

import com.davils.kore.core.arguments.fixture.Lenient
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.Syntax
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val NAME: String = "--name"
private const val VALUE: String = "kore"
private const val NEGATIVE_PORT: Int = -5
private const val NAMED_VALUE: String = "$NAME=$VALUE"
private const val VERBOSE: String = "--verbose"
private const val STRAY: String = "stray"

private fun loaded(vararg argv: String): Syntax {
    val parameters = Syntax()
    parameters.load(argv.toList(), NO_ENVIRONMENT)
    return parameters
}

private fun failureOf(vararg argv: String): KoreParameterException =
    shouldThrow<KoreParameterException> { Syntax().load(argv.toList(), NO_ENVIRONMENT) }

class KoreParametersSyntaxTest : FunSpec({
    context("a parameter with a value") {
        test("is read from a long name written with an equals sign") {
            loaded(NAMED_VALUE).name shouldBe VALUE
        }

        test("is read from a long name whose value is the next token") {
            loaded(NAME, VALUE).name shouldBe VALUE
        }

        test("is read from a short name whose value is the next token") {
            loaded("-n", VALUE).name shouldBe VALUE
        }

        test("is read from a short name with an attached value") {
            loaded("-n$VALUE").name shouldBe VALUE
        }

        test("is read from a short name written with an equals sign") {
            loaded("-n=$VALUE").name shouldBe VALUE
        }

        test("splits at the first equals sign only") {
            loaded("$NAME=a=b=c").name shouldBe "a=b=c"
        }

        test("accepts an empty value") {
            loaded("$NAME=").name shouldBe ""
        }

        test("reads a negative number as a value rather than as a parameter") {
            loaded("--port", "-5").port shouldBe NEGATIVE_PORT
        }

        test("may be a single dash") {
            loaded(NAME, "-").name shouldBe "-"
        }

        test("is null when an empty argument list leaves it out") {
            val parameters = loaded()

            parameters.name shouldBe null
            parameters.port shouldBe null
        }
    }

    context("a flag") {
        test("is true when it is named") {
            loaded(VERBOSE).isVerbose shouldBe true
        }

        test("is false when it is not named") {
            loaded().isVerbose shouldBe false
        }

        test("accepts an explicit value") {
            loaded("--verbose=false").isVerbose shouldBe false
        }

        test("turns on with every other flag in a cluster") {
            val parameters = loaded("-va")

            parameters.isVerbose shouldBe true
            parameters.isArchive shouldBe true
        }

        test("may end a cluster in a parameter that takes a value") {
            val parameters = loaded("-van", VALUE)

            parameters.isVerbose shouldBe true
            parameters.isArchive shouldBe true
            parameters.name shouldBe VALUE
        }

        test("turns its default off when negated") {
            loaded("--no-colour").isColoured shouldBe false
        }

        test("keeps its default when a negatable flag is not named") {
            loaded().isColoured shouldBe true
        }
    }

    context("a repeatable parameter") {
        test("collects every occurrence") {
            loaded("--tag=a", "-t", "b", "--tag", "c").tags shouldContainExactly listOf("a", "b", "c")
        }

        test("is empty when it is not given") {
            loaded().tags shouldBe emptyList()
        }
    }

    context("the terminator") {
        test("leaves everything after it untouched") {
            val parameters = loaded(NAMED_VALUE, "--", "--not-mine", "-x", "plain")

            parameters.name shouldBe VALUE
            parameters.rest shouldContainExactly listOf("--not-mine", "-x", "plain")
        }
    }

    context("what is reported") {
        test("a parameter that expects a value but ends the list") {
            val violation = failureOf(NAME).violations.single()

            violation.shouldBeInstanceOf<KoreParameterViolation.MissingValue>()
        }

        test("a following parameter is not swallowed as a missing value") {
            val violation = failureOf(NAME, VERBOSE).violations.single()

            violation.shouldBeInstanceOf<KoreParameterViolation.MissingValue>()
            violation.parameter.primaryName shouldBe NAME
        }

        test("a single valued parameter given twice rather than silently overwritten") {
            val violation = failureOf("$NAME=first", "$NAME=second").violations.single()

            violation.shouldBeInstanceOf<KoreParameterViolation.Repeated>()
            violation.occurrences shouldBe 2
        }

        test("a token with no name after its dashes as malformed") {
            val violation = failureOf("--=value").violations.single()

            violation.shouldBeInstanceOf<KoreParameterViolation.MalformedArgument>()
        }

        test("an undeclared parameter with the name it was probably meant to be") {
            val violation = failureOf("--verbsoe").violations.single()

            violation.shouldBeInstanceOf<KoreParameterViolation.UnknownArgument>()
            violation.suggestion shouldBe VERBOSE
        }

        test("an undeclared parameter close to nothing suggests nothing") {
            val violation = failureOf("--something-entirely-different").violations.single()

            violation.shouldBeInstanceOf<KoreParameterViolation.UnknownArgument>()
            violation.suggestion shouldBe null
        }

        test("a single dash on its own as a stray value") {
            val violation = failureOf("-").violations.single()

            violation.shouldBeInstanceOf<KoreParameterViolation.StrayValue>()
            violation.position shouldBe 1
        }

        test("a bare positional argument by where it was") {
            val violation = failureOf(STRAY).violations.single()

            violation.shouldBeInstanceOf<KoreParameterViolation.StrayValue>()
            violation.position shouldBe 1
        }
    }

    context("collecting unknown arguments") {
        test("collects an undeclared parameter instead of rejecting it") {
            val parameters = Lenient()

            parameters.load(listOf(NAMED_VALUE, "--theirs", STRAY), NO_ENVIRONMENT)

            parameters.name shouldBe VALUE
            parameters.unknown shouldContainExactly listOf("--theirs", STRAY)
        }
    }
})
