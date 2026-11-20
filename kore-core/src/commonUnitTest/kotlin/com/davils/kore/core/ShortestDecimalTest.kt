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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.core

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.math.absoluteValue

private const val TWELVE_AND_A_HALF: Double = 12.5
private const val ONE_AND_A_HALF: Double = 1.5
private const val TWO_AND_A_HALF: Double = 2.5
private const val FIFTEEN: Double = 15.0
private const val MOST_DIGITS: Int = 17
private const val LOWEST_EXPONENT: Int = -324
private const val HIGHEST_EXPONENT: Int = 308

private fun sample(value: Double, scientific: String, plain: String): Triple<Double, String, String> =
    Triple(first = value, second = scientific, third = plain)

private val samples: List<Triple<Double, String, String>> = listOf(
    sample(value = 0.0, scientific = "0E0", plain = "0"),
    sample(value = -0.0, scientific = "-0E0", plain = "-0"),
    sample(value = 1.0, scientific = "1E0", plain = "1"),
    sample(value = 1.5, scientific = "1.5E0", plain = "1.5"),
    sample(value = -2.5, scientific = "-2.5E0", plain = "-2.5"),
    sample(value = 100.0, scientific = "1E2", plain = "100"),
    sample(value = 0.1, scientific = "1E-1", plain = "0.1"),
    sample(value = 0.001, scientific = "1E-3", plain = "0.001"),
    sample(value = 1e-7, scientific = "1E-7", plain = "0.0000001"),
    sample(value = 0.1 + 0.2, scientific = "3.0000000000000004E-1", plain = "0.30000000000000004"),
    sample(value = 123_456.789, scientific = "1.23456789E5", plain = "123456.789"),
    sample(value = 9_999_999.0, scientific = "9.999999E6", plain = "9999999"),
    sample(value = 1e21, scientific = "1E21", plain = "1000000000000000000000"),
    sample(value = 1e23, scientific = "1E23", plain = "100000000000000000000000"),
    sample(value = Long.MAX_VALUE.toDouble(), scientific = "9.223372036854776E18", plain = "9223372036854776000"),
    sample(
        value = Double.MAX_VALUE,
        scientific = "1.7976931348623157E308",
        plain = "17976931348623157" + "0".repeat(292),
    ),
    sample(value = Double.MIN_VALUE, scientific = "5E-324", plain = "0." + "0".repeat(323) + "5"),
    sample(value = -Double.MIN_VALUE, scientific = "-5E-324", plain = "-0." + "0".repeat(323) + "5"),
    sample(value = Double.MIN_VALUE * 2, scientific = "1E-323", plain = "0." + "0".repeat(322) + "1"),
    sample(value = Double.MIN_VALUE * 3, scientific = "1.5E-323", plain = "0." + "0".repeat(322) + "15"),
)

private val finiteDoubles: Arb<Double> = Arb.double().filter(Double::isFinite)

private val doublesOfAllBits: Arb<Double> = Arb.long().map(Double::fromBits)

private val doublesOfAnyBits: Arb<Double> = doublesOfAllBits.filter(Double::isFinite)

private fun neighboursWithOneDigitLess(decimal: ShortestDecimal): List<String> {
    val shorter = decimal.digits.length - 1
    val kept = decimal.digits.take(shorter)
    val truncated = kept.toLong()
    val scale = decimal.exponent - shorter + 1
    return listOf("${truncated}e$scale", "${truncated + 1}e$scale")
}

private fun shouldBeShortestOf(decimal: ShortestDecimal, value: Double) {
    val magnitude = value.absoluteValue
    parseDecimalOrNull(decimal.toString()) shouldBe value
    parseDecimalOrNull(decimal.toPlainString()) shouldBe value
    (decimal.digits.length in 1..MOST_DIGITS) shouldBe true
    (decimal.exponent in LOWEST_EXPONENT..HIGHEST_EXPONENT) shouldBe true
    if (decimal.digits.length == 1) return
    decimal.digits.last() shouldNotBe '0'
    neighboursWithOneDigitLess(decimal).forEach { text -> parseDecimalOrNull(text) shouldNotBe magnitude }
}

class ShortestDecimalTest : FunSpec({
    context("of") {
        samples.forEach { (value, scientific, plain) ->
            test("writes $scientific with the same digits on every target") {
                val decimal = ShortestDecimal.of(value)

                decimal.toString() shouldBe scientific
                decimal.toPlainString() shouldBe plain
            }
        }

        test("splits a double into its sign, digits and exponent") {
            val decimal = ShortestDecimal.of(-TWELVE_AND_A_HALF)

            decimal.isNegative shouldBe true
            decimal.digits shouldBe "125"
            decimal.exponent shouldBe 1
        }

        test("writes one digit where Java writes two, if one reads back as the double") {
            ShortestDecimal.of(Double.MIN_VALUE).digits shouldBe "5"
            ShortestDecimal.of(Double.MIN_VALUE * 2).digits shouldBe "1"
            ShortestDecimal.of(ONE_AND_A_HALF).digits shouldBe "15"
        }

        test("finds the shortest decimal that reads back as any double") {
            checkAll(finiteDoubles) { value -> shouldBeShortestOf(ShortestDecimal.of(value), value) }
        }

        test("finds the shortest decimal that reads back as a double of any bits") {
            checkAll(doublesOfAnyBits) { value -> shouldBeShortestOf(ShortestDecimal.of(value), value) }
        }

        test("refuses a double that is not finite") {
            listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
                shouldThrow<IllegalArgumentException> { ShortestDecimal.of(value) }
            }
        }
    }

    context("equality") {
        test("holds for the same sign, digits and exponent") {
            val decimal = ShortestDecimal.of(ONE_AND_A_HALF)

            decimal shouldBe ShortestDecimal.of(ONE_AND_A_HALF)
            decimal.hashCode() shouldBe ShortestDecimal.of(ONE_AND_A_HALF).hashCode()
            decimal shouldNotBe ShortestDecimal.of(-ONE_AND_A_HALF)
            decimal shouldNotBe ShortestDecimal.of(FIFTEEN)
            decimal shouldNotBe ShortestDecimal.of(TWO_AND_A_HALF)
            decimal.equals("1.5E0") shouldBe false
        }
    }
})
