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

import com.davils.kore.core.arguments.fixture.FromSources
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.ParameterException
import com.davils.kore.core.arguments.report.ParameterViolation
import com.davils.kore.core.arguments.report.ParameterViolationCodes
import com.davils.kore.core.arguments.report.RedactedException
import com.davils.kore.core.arguments.report.ValueOrigin
import com.davils.kore.core.arguments.report.fixture.Masked
import com.davils.kore.core.arguments.resolution.fixture.CarelessSecret
import com.davils.kore.core.arguments.resolution.fixture.Endpoints
import com.davils.kore.core.arguments.resolution.fixture.ExpiringSecret
import com.davils.kore.core.arguments.resolution.fixture.Exposed
import com.davils.kore.core.arguments.resolution.fixture.SecretLeakingSource
import com.davils.kore.core.arguments.resolution.fixture.StrictSecrets
import com.davils.kore.core.arguments.resolution.fixture.ThrowingRules
import com.davils.kore.core.arguments.resolution.fixture.reachableTexts
import com.davils.kore.core.validation.violation.Severity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val CASES: Int = 200
private const val SHORTEST_SECRET: Int = 12
private const val LONGEST_SECRET: Int = 32
private const val SECRET: String = "hunter2-placeholder"
private const val MASK: String = "****"
private const val SECRET_TOKEN: String = "--token=$SECRET"

private val secrets: Arb<String> = Arb.string(
    minSize = SHORTEST_SECRET,
    maxSize = LONGEST_SECRET,
    codepoints = Codepoint.alphanumeric(),
)

private fun causesOf(failure: Throwable): List<Throwable> = generateSequence(failure) { it.cause }.toList()

private fun conversionOf(failure: ParameterException): ParameterViolation.ConversionFailure =
    failure.violations.filterIsInstance<ParameterViolation.ConversionFailure>().single()

private fun described(argv: List<String>): String {
    val parameters = Endpoints()
    parameters.load(argv, NO_ENVIRONMENT)
    return parameters.describe()
}

class ParametersSecretsTest : FunSpec({
    context("a conversion failure of a sensitive value") {
        test("never takes its reason from the converter's own words") {
            checkAll(CASES, secrets) { secret ->
                val failure = shouldThrow<ParameterException> {
                    ExpiringSecret().load(listOf("--token=$secret"), NO_ENVIRONMENT)
                }

                val conversion = conversionOf(failure)
                withClue(failure.message) {
                    conversion.reason shouldBe "could not be read as token"
                    conversion.value shouldBe MASK
                    failure.message shouldNotContain secret
                    conversion.toString() shouldNotContain secret
                }
            }
        }

        test("keeps only the type of what the converter threw") {
            checkAll(CASES, secrets) { secret ->
                val failure = shouldThrow<ParameterException> {
                    CarelessSecret().load(listOf("--pin=not-a-pin-$secret"), NO_ENVIRONMENT)
                }

                val cause = conversionOf(failure).cause.shouldBeInstanceOf<RedactedException>()
                cause.originalType shouldBe "java.lang.NumberFormatException"
                cause.stackTrace.toList().shouldBeEmpty()
                causesOf(cause).forEach { link -> link.message.orEmpty() shouldNotContain secret }
            }
        }

        test("keeps a constant reason even for a value a built-in converter refused") {
            val failure = shouldThrow<ParameterException> {
                Masked().load(listOf("--token=t", "--port=$SECRET"), mapOf("APP_TAG" to "a"))
            }

            conversionOf(failure).reason shouldBe "could not be read as int"
        }
    }

    context("a conversion failure of a value that is not sensitive") {
        test("keeps the converter's reason and what it threw") {
            val failure = shouldThrow<ParameterException> {
                Endpoints().load(listOf("--endpoint=http//no scheme"), NO_ENVIRONMENT)
            }

            conversionOf(failure).reason shouldContain "is not a valid URI"
        }
    }

    context("a rule that throws") {
        test("is reported as a failed validation instead of escaping the load") {
            val failure = shouldThrow<ParameterException> {
                ThrowingRules().load(listOf("--name=$SECRET"), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<ParameterViolation.ValidationFailure>()
            violation.code shouldBe ParameterViolationCodes.VALIDATION_FAILED
            violation.severity shouldBe Severity.Error
            violation.message shouldBe "could not be validated, because one of its rules failed unexpectedly"
            failure.message shouldNotContain SECRET
        }

        test("on a sensitive value never shows what the rule said") {
            val failure = shouldThrow<ParameterException> {
                ThrowingRules().load(listOf(SECRET_TOKEN), NO_ENVIRONMENT)
            }

            val violation = failure.violations.filterIsInstance<ParameterViolation.ValidationFailure>().single()
            violation.message shouldBe "failed validation - the detail is withheld because the parameter is sensitive"
            failure.message shouldNotContain SECRET
        }
    }

    context("a value source that fails for a sensitive parameter") {
        test("keeps only the type of what it threw") {
            val failure = shouldThrow<ParameterException> {
                FromSources().load(emptyList(), listOf(SecretLeakingSource(SECRET)), NO_ENVIRONMENT)
            }

            val unavailable = failure.violations.filterIsInstance<ParameterViolation.SourceUnavailable>()
            val secretOne = unavailable.first { violation -> violation.parameter.isSensitive }
            val plainOne = unavailable.first { violation -> !violation.parameter.isSensitive }
            val redacted = secretOne.cause.shouldBeInstanceOf<RedactedException>()
            redacted.message.orEmpty() shouldNotContain SECRET
            plainOne.cause.message shouldContain SECRET
        }
    }

    context("a mask") {
        test("reveals neither how many values there are nor whether there is one") {
            val parameters = Masked()

            parameters.load(listOf("--token=t", "--tag=a", "--tag=b", "--tag=c"), NO_ENVIRONMENT)

            val lines = parameters.describe().lines()
            lines shouldContainLine "  --tag    **** (from the command line)"
            lines shouldContainLine "  --port   ****"
        }
    }

    context("a sensitive value given on the command line") {
        test("is warned about, naming the variable to use instead") {
            val parameters = Exposed()

            parameters.load(listOf(SECRET_TOKEN, "--plain=visible"), NO_ENVIRONMENT)

            val warning = parameters.warnings.single()
            warning.shouldBeInstanceOf<ParameterViolation.SensitiveOnCommandLine>()
            warning.code shouldBe ParameterViolationCodes.SENSITIVE_ON_COMMAND_LINE
            warning.severity shouldBe Severity.Warning
            warning.message shouldContain "APP_TOKEN"
            warning.toString() shouldNotContain SECRET
        }

        test("without a variable is warned about all the same") {
            val parameters = Exposed()

            parameters.load(listOf("--local=$SECRET"), NO_ENVIRONMENT)

            parameters.warnings.single().message shouldBe "is sensitive but was given on the command line, " +
                "where the process list and the shell history show it"
        }

        test("is not warned about when it came from the environment") {
            val parameters = Exposed()

            parameters.load(emptyList(), mapOf("APP_TOKEN" to SECRET))

            parameters.warnings.shouldBeEmpty()
        }

        test("is refused when the schema treats it as an error") {
            val failure = shouldThrow<ParameterException> {
                StrictSecrets().load(listOf(SECRET_TOKEN), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<ParameterViolation.SensitiveOnCommandLine>()
            violation.severity shouldBe Severity.Error
            failure.message shouldNotContain SECRET
        }
    }

    context("user information in a URI that is not sensitive") {
        test("is masked when the value is described") {
            val description = described(
                listOf(
                    "--endpoint=https://user:$SECRET@api.example.com/v1?q=1#top",
                    "--mirror=https://mirror:$SECRET@m.example.com/",
                    "--dsn=postgres://app:$SECRET@db/app",
                ),
            )

            description shouldNotContain SECRET
            description shouldContain "https://****@api.example.com/v1?q=1#top (from the command line)"
            description shouldContain "[https://****@m.example.com/] (from the command line)"
            description shouldContain "postgres://****@db/app (from the command line)"
        }

        test("is masked in the default the usage listing shows") {
            val usage = Endpoints().usage()

            usage shouldNotContain "default-password"
            usage shouldContain "default: postgres://****@db/app"
        }

        test("is masked in the value of a conversion failure and absent from its causes") {
            val failure = shouldThrow<ParameterException> {
                Endpoints().load(listOf("--strict=https://user:$SECRET@api.example.com/"), NO_ENVIRONMENT)
            }

            val conversion = conversionOf(failure)
            conversion.value shouldBe "https://****@api.example.com/"
            failure.message shouldNotContain SECRET
            causesOf(conversion.cause).forEach { link -> link.message.orEmpty() shouldNotContain SECRET }
        }

        test("is masked in a described value whatever follows the scheme") {
            val forms = listOf(
                "https:user:$SECRET@evil.example/x",
                "https:/user:$SECRET@evil.example/x",
                "https:///user:$SECRET@evil.example/x",
                "https:\\user:$SECRET@evil.example/x",
                "https:/\t/user:$SECRET@evil.example/x",
                " https:user:$SECRET@evil.example/x",
                "\\/user:$SECRET@evil.example/x",
            )

            forms.forEach { form ->
                val description = described(listOf("--dsn=$form"))

                withClue(description) {
                    description shouldNotContain SECRET
                    description shouldContain "****@evil.example/x (from the command line)"
                }
            }
        }

        test("in a URI that names no host after its scheme is refused without being shown") {
            val failure = shouldThrow<ParameterException> {
                Endpoints().load(listOf("--endpoint=https:/user:$SECRET@evil.example/x"), NO_ENVIRONMENT)
            }

            val conversion = conversionOf(failure)
            conversion.reason shouldBe "does not name its host after //, which its scheme requires"
            conversion.value shouldBe "https:/****@evil.example/x"
            failure.message shouldNotContain SECRET
        }

        test("is masked in a value with backslashes that could not be parsed") {
            val failure = shouldThrow<ParameterException> {
                Endpoints().load(listOf("--strict=https:\\user:$SECRET@evil.example/x"), NO_ENVIRONMENT)
            }

            conversionOf(failure).value shouldNotContain SECRET
            failure.message shouldNotContain SECRET
        }

        test("is masked in a value that could not be parsed at all") {
            val failure = shouldThrow<ParameterException> {
                Endpoints().load(listOf("--strict=https://user:$SECRET@exa mple.com/"), NO_ENVIRONMENT)
            }

            val conversion = conversionOf(failure)
            conversion.value shouldNotContain SECRET
            conversion.cause.cause.shouldBeNull()
            causesOf(conversion.cause).forEach { link -> link.message.orEmpty() shouldNotContain SECRET }
        }
    }

    context("a violation about a sensitive parameter") {
        test("drops what it was given when it is created, whoever creates it") {
            val failure = shouldThrow<ParameterException> {
                ExpiringSecret().load(listOf(SECRET_TOKEN), NO_ENVIRONMENT)
            }
            val parameter = conversionOf(failure).parameter
            val thrown = IllegalStateException("the value $SECRET is wrong")
            val violations = listOf(
                ParameterViolation.ConversionFailure(
                    parameter = parameter,
                    origin = ValueOrigin.CommandLine,
                    given = SECRET,
                    thrown = thrown,
                ),
                ParameterViolation.SourceUnavailable(parameter, "vault", thrown),
                ParameterViolation.ValidationFailure(
                    parameter = parameter,
                    origin = ValueOrigin.CommandLine,
                    detail = "the value $SECRET is wrong",
                    code = ParameterViolationCodes.VALIDATION_FAILED,
                    severity = Severity.Error,
                ),
            )

            violations.forEach { violation ->
                val texts = reachableTexts(violation)
                withClue(texts) { texts.forEach { text -> text shouldNotContain SECRET } }
            }
        }

        test("keeps only the type of the exception it replaces") {
            val redacted = RedactedException(IllegalArgumentException(SECRET))

            redacted.originalType shouldBe "java.lang.IllegalArgumentException"
            reachableTexts(redacted).forEach { text -> text shouldNotContain SECRET }
        }
    }

    context("the internal records of a loaded value") {
        test("never print the value they hold") {
            Assigned(SECRET, ValueOrigin.CommandLine).toString() shouldNotContain SECRET
            val loaded = LoadedArguments(
                rest = listOf(SECRET),
                unknown = listOf(SECRET),
                unknownNames = listOf(SECRET),
                warnings = emptyList(),
            )

            loaded.toString() shouldNotContain SECRET
        }
    }
})

private infix fun List<String>.shouldContainLine(expected: String) {
    withClue(joinToString(separator = "\n")) { (expected in this) shouldBe true }
}
