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

import com.davils.kore.core.arguments.conversion.fixture.Converted
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.Stage
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.numericDouble
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.net.URI
import java.nio.file.Path
import kotlin.time.Duration

private const val CASES: Int = 300
private const val SHORTEST_REJECTED: Int = 10
private const val LONGEST_REJECTED: Int = 20
private const val LONGEST_NAME: Int = 12
private const val MOST_SECONDS: Long = 1_000_000

private val unparseable: Arb<String> = Arb.string(
    minSize = SHORTEST_REJECTED,
    maxSize = LONGEST_REJECTED,
    codepoints = Codepoint.az(),
)

private val names: Arb<String> = Arb.string(minSize = 1, maxSize = LONGEST_NAME, codepoints = Codepoint.az())

private fun loaded(argument: String): Converted {
    val parameters = Converted()
    parameters.load(listOf(argument), NO_ENVIRONMENT)
    return parameters
}

private fun rejectionOf(parameter: String, rejected: String) {
    val failure = shouldThrow<KoreParameterException> { loaded("$parameter=$rejected") }
    val conversion = failure.violations.filterIsInstance<KoreParameterViolation.ConversionFailure>().single()

    withClue(conversion.message) {
        conversion.reason.contains(rejected) shouldBe false
        conversion.value shouldBe rejected
    }
}

class KoreConvertersPropertyTest : FunSpec({
    context("a value survives being written down and converted back") {
        test("as an int") {
            checkAll(CASES, Arb.int()) { value -> loaded("--count=$value").count shouldBe value }
        }

        test("as a long") {
            checkAll(CASES, Arb.long()) { value -> loaded("--size=$value").size shouldBe value }
        }

        test("as a double") {
            checkAll(CASES, Arb.numericDouble()) { value -> loaded("--ratio=$value").ratio shouldBe value }
        }

        test("as a duration") {
            checkAll(CASES, Arb.long(min = 0, max = MOST_SECONDS)) { seconds ->
                val value = Duration.parse("${seconds}s")

                loaded("--timeout=$value").timeout shouldBe value
            }
        }

        test("as a path") {
            checkAll(CASES, names) { name -> loaded("--directory=/tmp/$name").directory shouldBe Path.of("/tmp/$name") }
        }

        test("as a URI") {
            checkAll(CASES, names) { host ->
                loaded(
                    "--endpoint=https://$host.example.com/x",
                ).endpoint shouldBe URI.create("https://$host.example.com/x")
            }
        }

        test("as an enum constant") {
            checkAll(
                CASES,
                Arb.enum<Stage>(),
            ) { value -> loaded("--stage=${value.name.lowercase()}").stage shouldBe value }
        }
    }

    context("a converter says what it expected without repeating what it rejected") {
        test("for an int") {
            checkAll(CASES, unparseable) { rejected -> rejectionOf("--count", rejected) }
        }

        test("for a long") {
            checkAll(CASES, unparseable) { rejected -> rejectionOf("--size", rejected) }
        }

        test("for a double") {
            checkAll(CASES, unparseable) { rejected -> rejectionOf("--ratio", rejected) }
        }

        test("for a duration") {
            checkAll(CASES, unparseable) { rejected -> rejectionOf("--timeout", rejected) }
        }
    }
})
