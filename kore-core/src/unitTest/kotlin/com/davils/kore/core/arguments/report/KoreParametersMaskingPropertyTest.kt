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

package com.davils.kore.core.arguments.report

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.fixture.Masked
import com.davils.kore.core.arguments.report.fixture.NegatableColour
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val SHORTEST_SECRET: Int = 12
private const val LONGEST_SECRET: Int = 32
private const val CASES: Int = 200
private const val MASK: String = "****"
private const val TOKEN: String = "--token"

private val secrets: Arb<String> = Arb.string(
    minSize = SHORTEST_SECRET,
    maxSize = LONGEST_SECRET,
    codepoints = Codepoint.alphanumeric(),
)

private val unparseable: Arb<String> = Arb.string(
    minSize = SHORTEST_SECRET,
    maxSize = LONGEST_SECRET,
    codepoints = Codepoint.az(),
)

private fun tokenOf(secret: String): String = "$TOKEN=$secret"

private fun describedAfter(argv: List<String>, environment: Map<String, String>): String {
    val parameters = Masked()
    parameters.load(argv, environment)
    return parameters.describe()
}

class KoreParametersMaskingPropertyTest : FunSpec({
    context("a sensitive value") {
        test("never reaches the resolved state report") {
            checkAll(CASES, secrets) { secret ->
                val description = describedAfter(listOf(tokenOf(secret)), NO_ENVIRONMENT)

                withClue(description) {
                    description shouldNotContain secret
                    description shouldContain MASK
                }
            }
        }

        test("never reaches the report when it came from the environment") {
            checkAll(CASES, secrets) { secret ->
                val description = describedAfter(emptyList(), mapOf("APP_TOKEN" to secret))

                withClue(description) { description shouldNotContain secret }
            }
        }

        test("never reaches the report from inside a list") {
            checkAll(CASES, secrets) { secret ->
                val description = describedAfter(
                    listOf(tokenOf(secret), "--tag=$secret", "--tag=$secret"),
                    NO_ENVIRONMENT,
                )

                withClue(description) { description shouldNotContain secret }
            }
        }

        test("is not carried by a conversion failure about it") {
            checkAll(CASES, unparseable) { rejected ->
                val failure = shouldThrow<KoreParameterException> {
                    Masked().load(listOf("--token=irrelevant-but-long-enough", "--port=$rejected"), NO_ENVIRONMENT)
                }

                val report = failure.message
                withClue(report) { report shouldNotContain rejected }
            }
        }

        test("never reaches the usage listing, which carries no value at all") {
            checkAll(CASES, secrets) { secret ->
                val parameters = Masked()
                parameters.load(listOf(tokenOf(secret)), NO_ENVIRONMENT)

                val usage = parameters.usage()
                withClue(usage) { usage shouldNotContain secret }
            }
        }
    }

    context("a malformed argument") {
        test("never carries whatever followed its equals sign") {
            checkAll(CASES, secrets) { secret ->
                val failure = shouldThrow<KoreParameterException> {
                    Masked().load(
                        listOf("--=$secret"),
                        NO_ENVIRONMENT,
                    )
                }

                val report = failure.message
                withClue(report) { report shouldNotContain secret }
            }
        }

        test("on a negated flag given a value never carries the value") {
            checkAll(CASES, secrets) { secret ->
                val failure = shouldThrow<KoreParameterException> {
                    NegatableColour().load(listOf("--no-colour=$secret"), NO_ENVIRONMENT)
                }

                val report = failure.message
                withClue(report) {
                    report shouldNotContain secret
                    report shouldContain "--no-colour"
                }
            }
        }
    }
})
