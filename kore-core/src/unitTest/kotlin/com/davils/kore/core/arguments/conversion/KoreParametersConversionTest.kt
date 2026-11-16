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

import com.davils.kore.core.arguments.conversion.fixture.Colour
import com.davils.kore.core.arguments.conversion.fixture.Custom
import com.davils.kore.core.arguments.conversion.fixture.Paths
import com.davils.kore.core.arguments.conversion.fixture.RequiredPaths
import com.davils.kore.core.arguments.conversion.fixture.Typed
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import java.net.URI
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val COUNT: Int = 7
private const val SIZE: Long = 9_000_000_000L
private const val RATIO: Double = 1.5
private const val TIMEOUT_SECONDS: Int = 30
private const val TIMEOUT_MINUTES: Int = 5
private const val ENDPOINT: String = "https://api.example.com/v1"

private fun typed(vararg argv: String): Typed {
    val parameters = Typed()
    parameters.load(argv.toList(), NO_ENVIRONMENT)
    return parameters
}

private fun typedFailure(vararg argv: String): KoreParameterViolation = shouldThrow<KoreParameterException> {
    Typed().load(argv.toList(), NO_ENVIRONMENT)
}.violations.single()

private fun pathFailure(vararg argv: String): KoreParameterViolation.ConversionFailure {
    val violation = shouldThrow<KoreParameterException> {
        RequiredPaths().load(argv.toList(), NO_ENVIRONMENT)
    }.violations.single()
    return violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
}

class KoreParametersConversionTest : FunSpec({
    context("the primitive types") {
        test("are converted") {
            val parameters = typed("--count=7", "--size=9000000000", "--ratio=1.5", "--enabled=yes")

            parameters.count shouldBe COUNT
            parameters.size shouldBe SIZE
            parameters.ratio shouldBe RATIO
            parameters.isEnabled shouldBe true
        }

        test("every accepted spelling of a boolean is read") {
            listOf("true", "TRUE", "yes", "on", "1").forEach { raw -> typed("--enabled=$raw").isEnabled shouldBe true }
            listOf("false", "no", "off", "0").forEach { raw -> typed("--enabled=$raw").isEnabled shouldBe false }
        }

        test("a boolean spelling it does not know is rejected rather than read as false") {
            typedFailure("--enabled=perhaps").shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
        }

        test("a long and a decimal number that are not numbers are rejected") {
            val size = typedFailure("--size=big")
            val ratio = typedFailure("--ratio=half")

            size.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            size.reason shouldBe "is not a valid long"
            ratio.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            ratio.reason shouldBe "is not a valid decimal number"
        }

        test("the parameter and the value it could not convert are reported") {
            val violation = typedFailure("--count=eighty")

            violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            violation.parameter.primaryName shouldBe "--count"
            violation.value shouldBe "eighty"
            violation.reason shouldBe "is not a valid integer"
        }
    }

    context("an enum") {
        test("matches a constant regardless of capitalisation") {
            typed("--colour=green").colour shouldBe Colour.Green
        }

        test("reports an unmatched value together with the ones that would have worked") {
            val violation = typedFailure("--colour=mauve")

            violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            violation.reason shouldBe "is not one of Red, Green, Blue"
        }
    }

    context("a duration") {
        test("is read in both the readable and the ISO form") {
            typed("--timeout=30s").timeout shouldBe TIMEOUT_SECONDS.seconds
            typed("--timeout=PT5M").timeout shouldBe TIMEOUT_MINUTES.minutes
        }

        test("that cannot be read is rejected") {
            typedFailure("--timeout=soon").shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
        }
    }

    context("a URI") {
        test("is read") {
            typed("--endpoint=$ENDPOINT").endpoint shouldBe URI(ENDPOINT)
        }

        test("that is malformed is rejected with the reason the parser gave") {
            val violation = typedFailure("--endpoint=http://a b")

            violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            violation.reason shouldStartWith "is not a valid URI: "
        }
    }

    context("a choice") {
        test("is restricted to what was declared") {
            typed("--level=high").level shouldBe "high"
            typedFailure("--level=medium").shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
        }
    }

    context("a split parameter") {
        test("splits one occurrence into several values") {
            typed("--tags=a,b", "--tags=c").tags shouldContainExactly listOf("a", "b", "c")
        }
    }

    context("a path") {
        test("accepts a directory that is there") {
            val directory = tempdir()
            val parameters = Paths()

            parameters.load(listOf("--dir=$directory"), NO_ENVIRONMENT)

            parameters.directory shouldBe directory.toPath()
        }

        test("reports a path that does not exist as part of the same report as everything else") {
            val failure = shouldThrow<KoreParameterException> {
                Paths().load(listOf("--dir=/definitely/not/here"), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            violation.reason shouldBe "does not exist"
        }

        test("reports a directory where a regular file is required") {
            val directory = tempdir()

            pathFailure("--regular=$directory").reason shouldBe "is not a regular file"
            pathFailure("--file=$directory").reason shouldBe "is not a regular file"
        }

        test("accepts a readable regular file") {
            val directory = tempdir().toPath()
            val file = directory.resolve("settings.conf").createFile()
            file.writeText("content")
            val parameters = RequiredPaths()

            parameters.load(listOf("--readable=$file", "--regular=$file", "--file=$file"), NO_ENVIRONMENT)

            parameters.readable shouldBe file
            parameters.regular shouldBe file
            parameters.file shouldBe file.toFile()
        }

        test("reports a readable path that does not exist as missing") {
            pathFailure("--readable=/definitely/not/here").reason shouldBe "does not exist"
        }
    }

    context("a converter the schema brought itself") {
        test("is applied") {
            val parameters = Custom()

            parameters.load(listOf("--id=id-42"), NO_ENVIRONMENT)

            parameters.identifier shouldBe "42"
        }

        test("reports what it rejected") {
            val failure = shouldThrow<KoreParameterException> { Custom().load(listOf("--id=42"), NO_ENVIRONMENT) }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            violation.reason shouldBe "must start with 'id-'"
        }

        test("names its type in the usage listing") {
            Custom().usage() shouldContain "--id <identifier>"
        }
    }
})
