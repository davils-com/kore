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

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

private const val MOST_ELEMENTS: Int = 64
private const val ORDERS_OF_THREE: Int = 6
private const val TRIALS: Int = 600

class ShufflesTest : FunSpec({
    context("a shuffle") {
        test("keeps every element exactly once") {
            checkAll(Arb.list(Arb.int(), range = 0..MOST_ELEMENTS), Arb.long()) { elements, seed ->
                var bits = seed
                val entropy = Entropy {
                    bits = bits * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
                    bits
                }

                elements.shuffled(entropy) shouldContainExactlyInAnyOrder elements
            }
        }

        test("follows its source, so a fixed source gives a fixed order") {
            val first = listOf("a", "b", "c", "d").shuffled(Entropy { 0L })
            val second = listOf("a", "b", "c", "d").shuffled(Entropy { 0L })

            first shouldBe second
            first shouldBe listOf("b", "c", "d", "a")
        }

        test("reaches every order of three elements with secure entropy") {
            val orders = List(TRIALS) { listOf(1, 2, 3).shuffled(SecureEntropy) }.toSet()

            orders.size shouldBe ORDERS_OF_THREE
        }

        test("leaves the original collection as it is and hands out a list no caller can change") {
            val original = mutableListOf("a", "b", "c")
            val shuffled = original.shuffled(SecureEntropy)

            original shouldBe listOf("a", "b", "c")
            shouldThrowAny { (shuffled as MutableList<String>).add("d") }
        }
    }
})
