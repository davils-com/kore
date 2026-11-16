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

package com.davils.kore.core.size

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val COUNT: Int = 3
private const val COUNT_IN_BYTES: Long = 3L
private const val KIBIBYTES_IN_BYTES: Long = 3_072L
private const val MEBIBYTES_IN_BYTES: Long = 3_145_728L
private const val GIBIBYTES_IN_BYTES: Long = 3_221_225_472L
private const val HALF: Int = 512
private const val ODD: Int = 1_536
private const val LARGE: Int = 64
private const val TWICE: Int = 2
private const val BEYOND_LARGEST: String = "exceeds the largest data size"

private val sizes: Arb<KoreDataSize> = Arb.long(min = 0L, max = Long.MAX_VALUE).map(::KoreDataSize)

class KoreDataSizeTest : FunSpec({
    context("units") {
        test("converts every binary unit into bytes") {
            COUNT.bytes.bytes shouldBe COUNT_IN_BYTES
            COUNT_IN_BYTES.bytes.bytes shouldBe COUNT_IN_BYTES
            COUNT.kibibytes.bytes shouldBe KIBIBYTES_IN_BYTES
            COUNT.mebibytes.bytes shouldBe MEBIBYTES_IN_BYTES
            COUNT.gibibytes.bytes shouldBe GIBIBYTES_IN_BYTES
        }

        test("refuses a negative size") {
            shouldThrow<IllegalArgumentException> { KoreDataSize(-1L) }
        }
    }

    context("arithmetic") {
        test("adds and subtracts sizes") {
            1.mebibytes + HALF.kibibytes shouldBe ODD.kibibytes
            1.mebibytes - HALF.kibibytes shouldBe HALF.kibibytes
        }

        test("multiplies a size") {
            HALF.kibibytes * TWICE shouldBe 1.mebibytes
            HALF.kibibytes * TWICE.toLong() shouldBe 1.mebibytes
            1.gibibytes * 0 shouldBe KoreDataSize.ZERO
        }

        test("compares by bytes") {
            1.gibibytes shouldBeGreaterThan HALF.mebibytes
        }

        test("refuses a difference below zero and says so") {
            shouldThrow<IllegalArgumentException> { 1.kibibytes - COUNT.kibibytes }
                .message shouldContain "cannot be negative"
        }

        test("refuses a negative factor") {
            shouldThrow<IllegalArgumentException> { 1.kibibytes * -1 }
        }
    }

    context("overflow") {
        test("refuses a sum beyond the largest data size with that reason, not as a negative size") {
            val message = shouldThrow<ArithmeticException> { Long.MAX_VALUE.bytes + 1.bytes }.message

            message shouldBe "9223372036854775807 B plus 1 B exceeds the largest data size of 9223372036854775807 B"
            message shouldNotContain "negative"
        }

        test("refuses a product beyond the largest data size with that reason, not as a negative size") {
            val message = shouldThrow<ArithmeticException> { Long.MAX_VALUE.bytes * TWICE }.message

            message shouldContain BEYOND_LARGEST
            message shouldNotContain "negative"
        }

        test("adds exactly as Math.addExact does and fails exactly where it fails") {
            checkAll(sizes, sizes) { left, right ->
                val exact = runCatching { Math.addExact(left.bytes, right.bytes) }
                val sum = runCatching { (left + right).bytes }

                sum.getOrNull() shouldBe exact.getOrNull()
                sum.exceptionOrNull()?.message?.shouldContain(BEYOND_LARGEST)
            }
        }

        test("multiplies exactly as Math.multiplyExact does and fails exactly where it fails") {
            checkAll(sizes, Arb.long(min = 0L, max = Long.MAX_VALUE)) { size, factor ->
                val exact = runCatching { Math.multiplyExact(size.bytes, factor) }
                val product = runCatching { (size * factor).bytes }

                product.getOrNull() shouldBe exact.getOrNull()
                product.exceptionOrNull()?.message?.shouldContain(BEYOND_LARGEST)
            }
        }
    }

    context("rendering") {
        test("renders the largest exact unit") {
            LARGE.kibibytes.toString() shouldBe "64 KiB"
            COUNT.mebibytes.toString() shouldBe "3 MiB"
            COUNT.gibibytes.toString() shouldBe "3 GiB"
        }

        test("renders bytes when no unit divides the size") {
            ODD.bytes.toString() shouldBe "1536 B"
            KoreDataSize.ZERO.toString() shouldBe "0 B"
        }
    }
})
