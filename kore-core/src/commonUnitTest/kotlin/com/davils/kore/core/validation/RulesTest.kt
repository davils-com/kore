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

package com.davils.kore.core.validation

import com.davils.kore.core.validation.fixture.Probe
import com.davils.kore.core.validation.fixture.Shape
import com.davils.kore.core.validation.fixture.paths
import com.davils.kore.core.validation.report.ValidationReport
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val PORT_MAX: Int = 65_535
private const val LONGEST: Int = 4
private const val SECRET: String = "hunter2-secret"
private const val TOO_BIG: Int = 70_000
private const val SECRET_NUMBER: Long = 4_242_424_242L

private val passed: MutableList<Boolean> = mutableListOf()

private fun report(rules: ValidationScope.() -> Unit): ValidationReport = Probe(rules).validate("probe")

private fun codesOf(rules: ValidationScope.() -> Unit): List<ViolationCode> =
    report(rules).violations.map { violation -> violation.code }

private fun messageOf(rules: ValidationScope.() -> Unit): String = report(rules).violations.single().message

class RulesTest : FunSpec({
    beforeTest { passed.clear() }

    context("numbers") {
        test("pass positive and non-negative values") {
            val result = report {
                passed += positive(Shape::queueSize, 1)
                passed += positive(Shape::queueSize, 1L)
                passed += nonNegative(Shape::queueSize, 0)
                passed += nonNegative(Shape::queueSize, 0L)
            }

            result.violations.shouldBeEmpty()
            passed shouldContainExactly listOf(true, true, true, true)
        }

        test("refuse zero and negative values where they must be positive") {
            val codes = codesOf {
                passed += positive(Shape::queueSize, 0)
                passed += positive(Shape::queueSize, Long.MIN_VALUE)
                passed += nonNegative(Shape::queueSize, Int.MIN_VALUE)
                passed += nonNegative(Shape::queueSize, -1L)
            }

            codes shouldContainExactly List(LONGEST) { ViolationCode.OUT_OF_RANGE }
            passed shouldContainExactly listOf(false, false, false, false)
        }

        test("name the rule without the value") {
            messageOf { positive(Shape::queueSize, 0) } shouldBe "must be greater than 0"
            messageOf { nonNegative(Shape::queueSize, -1) } shouldBe "must not be negative"
        }
    }

    context("durations") {
        test("pass positive, finite durations and zero where it may be zero") {
            val result = report {
                passed += positive(Shape::level, 1.seconds)
                passed += nonNegative(Shape::level, Duration.ZERO)
            }

            result.violations.shouldBeEmpty()
            passed shouldContainExactly listOf(true, true)
        }

        test("refuse zero, negative and infinite durations") {
            val codes = codesOf {
                positive(Shape::level, Duration.ZERO)
                positive(Shape::level, Duration.INFINITE)
                positive(Shape::level, -1.seconds)
                nonNegative(Shape::level, Duration.INFINITE)
                nonNegative(Shape::level, -Duration.INFINITE)
                nonNegative(Shape::level, -1.seconds)
            }

            codes shouldContainExactly List(LONGEST + 2) { ViolationCode.OUT_OF_RANGE }
        }

        test("name the rule without the value") {
            messageOf { positive(Shape::level, Duration.INFINITE) } shouldBe "must be a positive, finite duration"
            messageOf { nonNegative(Shape::level, -1.seconds) } shouldBe "must be zero or a positive, finite duration"
        }
    }

    context("ranges") {
        test("pass values on both bounds") {
            val result = report {
                passed += atLeast(property = Shape::queueSize, value = 1, minimum = 1)
                passed += atMost(property = Shape::queueSize, value = PORT_MAX, maximum = PORT_MAX)
                passed += inRange(Shape::queueSize, PORT_MAX, 1..PORT_MAX)
            }

            result.violations.shouldBeEmpty()
            passed shouldContainExactly listOf(true, true, true)
        }

        test("refuse values beyond the bounds and name the bounds") {
            messageOf { atLeast(property = Shape::queueSize, value = 0, minimum = 1) } shouldBe "must be at least 1"
            messageOf { atMost(property = Shape::level, value = 2.seconds, maximum = 1.seconds) } shouldBe
                "must be at most 1s"
            messageOf { inRange(Shape::queueSize, TOO_BIG, 1..PORT_MAX) } shouldBe "must lie between 1 and 65535"
            val codes = codesOf { inRange(Shape::queueSize, 0, 1..PORT_MAX) }

            codes shouldContainExactly listOf(ViolationCode.OUT_OF_RANGE)
        }
    }

    context("text") {
        test("refuses blank text as missing") {
            val result = report {
                passed += notBlank(Shape::level, "info")
                passed += notBlank(Shape::level, " \t")
            }

            passed shouldContainExactly listOf(true, false)
            result.violations.single().code shouldBe ViolationCode.REQUIRED
            result.violations.single().message shouldBe "must not be blank"
        }

        test("refuses text longer than its maximum") {
            val result = report {
                passed += maxLength(Shape::level, "abcd", LONGEST)
                passed += maxLength(Shape::level, "abcde", LONGEST)
            }

            passed shouldContainExactly listOf(true, false)
            result.violations.single().code shouldBe ViolationCode.TOO_LONG
            result.violations.single().message shouldBe "must be at most 4 characters long"
        }

        test("refuses a negative maximum as a programming error") {
            shouldThrow<IllegalArgumentException> { report { maxLength(Shape::level, "a", -1) } }
        }
    }

    context("collections") {
        test("refuse an empty collection as missing") {
            val result = report {
                passed += notEmpty(Shape::sinks, listOf("console"))
                passed += notEmpty(Shape::sinks, emptyList<String>())
            }

            passed shouldContainExactly listOf(true, false)
            result.violations.single().code shouldBe ViolationCode.REQUIRED
        }

        test("report every later duplicate at its position with the position of the first") {
            val names = listOf("a", "B", "b", "c", "A", "a")

            val result = report { passed += unique(Shape::sinks, names) { name -> name.lowercase() } }

            passed shouldContainExactly listOf(false)
            result.paths() shouldContainExactly listOf("sinks[2]", "sinks[4]", "sinks[5]")
            result.violations.map { violation -> violation.message } shouldContainExactly listOf(
                "duplicates the entry at index 1",
                "duplicates the entry at index 0",
                "duplicates the entry at index 0",
            )
        }

        test("pass a list without duplicates") {
            val result = report { passed += unique(Shape::sinks, listOf("a", "b")) { name -> name } }

            result.violations.shouldBeEmpty()
            passed shouldContainExactly listOf(true)
        }
    }

    context("secrecy") {
        test("never quotes the value in a message") {
            checkAll(Arb.int()) { number ->
                val result = report {
                    positive(Shape::queueSize, -SECRET_NUMBER)
                    atMost(property = Shape::queueSize, value = SECRET_NUMBER, maximum = 0L)
                    maxLength(Shape::level, SECRET, 1)
                    notBlank(Shape::level, "")
                    inRange(Shape::queueSize, number, 0..-1)
                }

                result.violations.forEach { violation ->
                    violation.message shouldNotContain SECRET
                    violation.message shouldNotContain SECRET_NUMBER.toString()
                }
            }
        }
    }
})
