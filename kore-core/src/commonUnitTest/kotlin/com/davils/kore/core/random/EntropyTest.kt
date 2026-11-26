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

package com.davils.kore.core.random

import com.davils.kore.core.Limits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

private const val DRAWS: Int = 1_000
private const val ALL_ONES: Long = -1L
private const val REJECTED_THEN_SEVEN: Long = 7L shl 33
private const val COUNTING_BYTES: Long = 0x0807_0605_0403_0201L
private const val LAST_BYTE: Byte = 0x0A
private const val TOKEN_BYTES: Int = 32

private fun scripted(vararg values: Long): Entropy {
    var index = 0
    return Entropy { values[index++ % values.size] }
}

class EntropyTest : FunSpec({
    context("a number below a bound") {
        test("stays within the bound for every source") {
            checkAll(Arb.long(), Arb.int(min = 1, max = Int.MAX_VALUE)) { bits, bound ->
                val number = scripted(bits, 0L).nextInt(bound)

                number shouldBeGreaterThanOrEqual 0
                number shouldBeLessThan bound
            }
        }

        test("draws again for bits that would favour small numbers") {
            val bound = 3
            val source = scripted(ALL_ONES, REJECTED_THEN_SEVEN)

            source.nextInt(bound) shouldBe 1
        }

        test("refuses a bound that is not positive") {
            shouldThrow<IllegalArgumentException> { SecureEntropy.nextInt(0) }
            shouldThrow<IllegalArgumentException> { SecureEntropy.nextInt(-1) }
        }
    }

    context("random bytes") {
        test("fill eight bytes from every draw, least significant first") {
            val bytes = scripted(COUNTING_BYTES, LAST_BYTE.toLong()).nextBytes(Long.SIZE_BYTES + 1)
            val counted = (1..Long.SIZE_BYTES).map { value -> value.toByte() }

            bytes.toList() shouldBe counted + LAST_BYTE
        }

        test("are as many as asked for, up to the limit") {
            SecureEntropy.nextBytes(0).size shouldBe 0
            SecureEntropy.nextBytes(Limits.MAX_ENTROPY_BYTES).size shouldBe Limits.MAX_ENTROPY_BYTES
        }

        test("refuse a count that is negative or beyond the limit before drawing") {
            var draws = 0
            val counting = Entropy { draws++.toLong() }

            shouldThrow<IllegalArgumentException> { counting.nextBytes(-1) }
            shouldThrow<IllegalArgumentException> { counting.nextBytes(Limits.MAX_ENTROPY_BYTES + 1) }
            draws shouldBe 0
        }
    }

    context("the secure entropy of the platform") {
        test("does not repeat itself") {
            val drawn = List(DRAWS) { SecureEntropy.nextLong() }

            drawn.toSet() shouldHaveSize DRAWS
        }

        test("gives different bytes on every call") {
            SecureEntropy.nextBytes(TOKEN_BYTES).toList() shouldNotBe SecureEntropy.nextBytes(TOKEN_BYTES).toList()
        }

        test("never shows a random value") {
            SecureEntropy.toString() shouldBe "SecureEntropy"
        }
    }
})
