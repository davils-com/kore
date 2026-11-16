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
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreParameterViolationCodes
import com.davils.kore.core.arguments.report.KoreRedactedException
import com.davils.kore.core.arguments.resolution.fixture.AnsweringSource
import com.davils.kore.core.arguments.resolution.fixture.CarelessConverter
import com.davils.kore.core.arguments.resolution.fixture.CarelessSecret
import com.davils.kore.core.arguments.resolution.fixture.CountingSource
import com.davils.kore.core.arguments.resolution.fixture.FailingSource
import com.davils.kore.core.arguments.resolution.fixture.FakeVault
import com.davils.kore.core.arguments.resolution.fixture.NamelessSource
import com.davils.kore.core.arguments.resolution.fixture.Unfinished
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PLACEHOLDER: String = "placeholder-secret-value"
private const val REFUSED: String = "connection refused"
private const val SEVEN: Int = 7

class KoreParametersContainmentTest : FunSpec({
    context("a converter that throws something other than KoreConversionException") {
        test("is reported as a conversion failure beside every other problem") {
            val failure = shouldThrow<KoreParameterException> {
                CarelessConverter().load(listOf("--port=eighty", "--workers=many"), NO_ENVIRONMENT)
            }

            failure.violations.map { it.code } shouldContainExactly listOf(
                KoreParameterViolationCodes.CONVERSION_FAILED,
                KoreParameterViolationCodes.CONVERSION_FAILED,
            )
        }

        test("is reported with a generic reason and keeps what it threw as the cause") {
            val failure = shouldThrow<KoreParameterException> {
                CarelessConverter().load(listOf("--port=eighty"), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            violation.reason shouldBe "could not be read as port"
            violation.cause.shouldBeInstanceOf<NumberFormatException>()
        }

        test("does not leak a sensitive value through its own message") {
            val failure = shouldThrow<KoreParameterException> {
                CarelessSecret().load(listOf("--pin=$PLACEHOLDER"), NO_ENVIRONMENT)
            }

            failure.message shouldNotContain PLACEHOLDER
        }
    }

    context("an error thrown for a sensitive parameter") {
        test("is reported without what it says, whether a rule or a converter threw it") {
            val failure = shouldThrow<KoreParameterException> {
                Unfinished().load(listOf("--token=$PLACEHOLDER", "--pin=$PLACEHOLDER"), NO_ENVIRONMENT)
            }

            failure.message shouldNotContain PLACEHOLDER
            failure.violations.forEach { violation -> violation.toString() shouldNotContain PLACEHOLDER }
            val conversion = failure.violations.filterIsInstance<KoreParameterViolation.ConversionFailure>().single()
            val cause = conversion.cause.shouldBeInstanceOf<KoreRedactedException>()
            cause.originalType shouldBe "java.lang.AssertionError"
            failure.violations.filterIsInstance<KoreParameterViolation.ValidationFailure>().single()
        }
    }

    context("a value source whose name cannot be read") {
        test("is reported as unavailable under its position instead of aborting the load") {
            val failure = shouldThrow<KoreParameterException> {
                val vault = FakeVault(mapOf("--token" to "t"))
                FromSources().load(emptyList(), listOf(vault, NamelessSource(PLACEHOLDER)), NO_ENVIRONMENT)
            }

            val unavailable = failure.violations.filterIsInstance<KoreParameterViolation.SourceUnavailable>()
            unavailable.map { violation -> violation.path.toString() } shouldContainExactly listOf(
                "--region (from source 2)",
            )
            unavailable.single().cause.message shouldContain PLACEHOLDER
            failure.message shouldNotContain PLACEHOLDER
        }

        test("keeps only the type of what it threw for a sensitive parameter") {
            val failure = shouldThrow<KoreParameterException> {
                FromSources().load(emptyList(), listOf(NamelessSource(PLACEHOLDER)), NO_ENVIRONMENT)
            }

            val unavailable = failure.violations.filterIsInstance<KoreParameterViolation.SourceUnavailable>()
            unavailable.map { violation -> violation.sourceName }.toSet() shouldBe setOf("source 1")
            val secret = unavailable.first { violation -> violation.parameter.isSensitive }
            secret.cause.shouldBeInstanceOf<KoreRedactedException>().originalType shouldBe
                "java.lang.IllegalStateException"
        }
    }

    context("a value source that answers") {
        test("is asked for its name once, however many values it gives") {
            val source = CountingSource()
            val parameters = FromSources()

            parameters.load(emptyList(), listOf(source), NO_ENVIRONMENT)

            parameters.region shouldBe SEVEN
            source.nameReads shouldBe 1
        }
    }

    context("a value source that throws") {
        test("is reported as unavailable instead of aborting the load") {
            val failure = shouldThrow<KoreParameterException> {
                FromSources().load(emptyList(), listOf(FailingSource()), NO_ENVIRONMENT)
            }

            failure.violations.map { it.code }.toSet() shouldBe setOf(KoreParameterViolationCodes.SOURCE_UNAVAILABLE)
        }

        test("is reported under the source's name with the exception kept but not rendered") {
            val failure = shouldThrow<KoreParameterException> {
                FromSources().load(emptyList(), listOf(FailingSource()), NO_ENVIRONMENT)
            }

            val violation = failure.violations.last()
            violation.shouldBeInstanceOf<KoreParameterViolation.SourceUnavailable>()
            violation.path.toString() shouldBe "--region (from vault)"
            violation.sourceName shouldBe "vault"
            violation.message shouldBe "could not be read, because the source failed"
            violation.cause.message shouldBe REFUSED
            failure.message shouldNotContain REFUSED
        }

        test("keeps only the type of what it threw for a sensitive parameter") {
            val failure = shouldThrow<KoreParameterException> {
                FromSources().load(emptyList(), listOf(FailingSource()), NO_ENVIRONMENT)
            }

            val violation = failure.violations.first()
            violation.shouldBeInstanceOf<KoreParameterViolation.SourceUnavailable>()
            violation.path.toString() shouldBe "--token (from vault)"
            val cause = violation.cause.shouldBeInstanceOf<KoreRedactedException>()
            cause.originalType shouldBe "java.lang.IllegalStateException"
            cause.message shouldNotContain REFUSED
            cause.cause shouldBe null
        }

        test("does not stop a later source from being asked") {
            val failure = shouldThrow<KoreParameterException> {
                FromSources().load(emptyList(), listOf(FailingSource(), AnsweringSource()), NO_ENVIRONMENT)
            }

            val conversion = failure.violations.filterIsInstance<KoreParameterViolation.ConversionFailure>()
            conversion.single().path.toString() shouldBe "--region (from defaults)"
        }
    }
})
