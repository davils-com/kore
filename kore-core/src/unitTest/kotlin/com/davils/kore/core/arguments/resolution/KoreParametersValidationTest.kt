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
import com.davils.kore.core.arguments.resolution.fixture.Grouped
import com.davils.kore.core.arguments.resolution.fixture.Mandatory
import com.davils.kore.core.arguments.resolution.fixture.Validated
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PORT: Int = 8080
private const val LONGEST_NAME: Int = 9
private const val PASSWORD: String = "--password=secret"
private const val TOKEN: String = "--token"
private const val LOCALHOST: String = "localhost"

private fun validatedFailure(vararg argv: String): KoreParameterException =
    shouldThrow<KoreParameterException> { Validated().load(argv.toList(), NO_ENVIRONMENT) }

private fun groupedFailure(vararg argv: String): KoreParameterViolation =
    shouldThrow<KoreParameterException> { Grouped().load(argv.toList(), NO_ENVIRONMENT) }.violations.single()

private fun mandatory(vararg argv: String): Mandatory {
    val parameters = Mandatory()
    parameters.load(argv.toList(), NO_ENVIRONMENT)
    return parameters
}

class KoreParametersValidationTest : FunSpec({
    context("a validation rule") {
        test("keeps a value that satisfies it") {
            val parameters = Validated()

            parameters.load(listOf("--port=8080"), NO_ENVIRONMENT)

            parameters.port shouldBe PORT
        }

        test("reports a value that fails it with the rule's own message") {
            val violation = validatedFailure("--port=70000").violations.single()

            violation.shouldBeInstanceOf<KoreParameterViolation.ValidationFailure>()
            violation.message shouldBe "must be a valid TCP port"
        }

        test("runs together with every other rule declared for one parameter") {
            val offending = " ".repeat(LONGEST_NAME)

            val messages = validatedFailure("--name=$offending").violations.map { it.message }

            messages shouldContainExactly listOf("must not be blank", "must be at most 8 characters")
        }

        test("runs against each element of a repeatable parameter") {
            validatedFailure("--weight=1", "--weight=-2", "--weight=-3").violations shouldHaveSize 2
        }
    }

    context("a parameter group") {
        test("accepts one of a mutually exclusive pair") {
            val parameters = Grouped()

            parameters.load(listOf("$TOKEN=abc"), NO_ENVIRONMENT)

            parameters.token shouldBe "abc"
            parameters.keyFile shouldBe null
        }

        test("reports both of a mutually exclusive pair") {
            val violation = groupedFailure("$TOKEN=abc", "--key-file=/tmp/key")

            violation.shouldBeInstanceOf<KoreParameterViolation.MutuallyExclusive>()
            violation.parameters.map { it.primaryName } shouldContainExactly listOf(TOKEN, "--key-file")
            violation.message shouldBe "cannot be used together"
            violation.path.toString() shouldBe "--token, --key-file"
        }

        test("reports a parameter given without what it depends on") {
            val violation = groupedFailure(PASSWORD)

            violation.shouldBeInstanceOf<KoreParameterViolation.MissingDependency>()
            violation.missing.map { it.primaryName } shouldContainExactly listOf("--user")
            violation.message shouldBe "requires --user"
        }

        test("accepts a parameter given with what it depends on") {
            val parameters = Grouped()

            parameters.load(listOf(PASSWORD, "--user=alice"), NO_ENVIRONMENT)

            parameters.password shouldBe "secret"
        }
    }

    context("a required parameter") {
        test("that is given reads as a non null value beside a default") {
            val parameters = mandatory("--port=8080")

            parameters.port shouldBe PORT
            parameters.host shouldBe LOCALHOST
        }

        test("that is absent is reported") {
            val failure = shouldThrow<KoreParameterException> { Mandatory().load(emptyList(), NO_ENVIRONMENT) }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.MissingRequired>()
            violation.parameter.primaryName shouldBe "--port"
            violation.message shouldBe "is required but was not given"
        }
    }

    context("a declared default") {
        test("is used when nothing supplies a value") {
            mandatory("--port=1").host shouldBe LOCALHOST
        }

        test("loses to a given value") {
            mandatory("--port=1", "--host=example.com").host shouldBe "example.com"
        }
    }
})
