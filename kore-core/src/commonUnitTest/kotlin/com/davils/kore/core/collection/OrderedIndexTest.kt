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

package com.davils.kore.core.collection

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.fixture.collidingKeys
import com.davils.kore.core.collection.fixture.fastestRun
import com.davils.kore.core.collection.fixture.shouldGrowAtMostBy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

private const val FEW_BITS: Int = 13
private const val MANY_BITS: Int = 15
private const val FEW: Int = 1 shl FEW_BITS
private const val MANY: Int = 1 shl MANY_BITS
private const val GROWTH: Int = 5
private const val MOST_KEYS: Int = 300
private const val KEY_RANGE: Int = 60

private fun ascending(count: Int): OrderedIndex<Int> {
    val index = OrderedIndex<Int>()
    repeat(count) { key -> index.ordinalFor(key) }
    return index
}

private fun descending(count: Int): OrderedIndex<Int> {
    val index = OrderedIndex<Int>()
    repeat(count) { key -> index.ordinalFor(count - key) }
    return index
}

private fun indexOf(keys: List<String>): OrderedIndex<String> {
    val index = OrderedIndex<String>()
    keys.forEach(index::ordinalFor)
    return index
}

class OrderedIndexTest : FunSpec({
    context("ordinalFor and ordinalOf") {
        test("hand out dense ordinals in the order the keys first came and find them again") {
            val index = OrderedIndex<String>()

            index.ordinalFor("b") shouldBe 0
            index.ordinalFor("a") shouldBe 1
            index.ordinalFor("b") shouldBe 0
            index.ordinalOf("a") shouldBe 1
            index.ordinalOf("c").shouldBeNull()
            index.size shouldBe 2
        }

        test("find nothing in an empty index") {
            val index = OrderedIndex<String>()

            index.ordinalOf("a").shouldBeNull()
            index.size shouldBe 0
        }

        test("agree with a map of the same keys in any order") {
            checkAll(Arb.list(Arb.int(0 until KEY_RANGE), 0..MOST_KEYS)) { keys ->
                val index = OrderedIndex<Int>()
                val expected = LinkedHashMap<Int, Int>()
                keys.forEach { key -> index.ordinalFor(key) shouldBe expected.getOrPut(key) { expected.size } }

                for (key in 0 until KEY_RANGE) index.ordinalOf(key) shouldBe expected[key]
                index.size shouldBe expected.size
            }
        }
    }

    context("keys with the same hash code") {
        test("stay distinct keys") {
            val keys = collidingKeys(FEW_BITS)
            val index = indexOf(keys)
            val hashCodes = keys.mapTo(mutableSetOf(), String::hashCode)

            hashCodes.size shouldBe 1
            index.size shouldBe FEW
            keys.forEachIndexed { ordinal, key -> index.ordinalOf(key) shouldBe ordinal }
        }

        test("take time that grows with the number of keys and not with its square") {
            val few = collidingKeys(FEW_BITS)
            val many = collidingKeys(MANY_BITS)

            val fewTime = fastestRun { indexOf(few) }
            val manyTime = fastestRun { indexOf(many) }

            manyTime.shouldGrowAtMostBy(GROWTH, from = fewTime)
        }
    }

    context("growth") {
        test("stays balanced for keys that come in ascending order") {
            val fewTime = fastestRun { ascending(FEW) }
            val manyTime = fastestRun { ascending(MANY) }

            manyTime.shouldGrowAtMostBy(GROWTH, from = fewTime)
            ascending(MANY).ordinalOf(MANY - 1) shouldBe MANY - 1
        }

        test("stays balanced for keys that come in descending order") {
            val fewTime = fastestRun { descending(FEW) }
            val manyTime = fastestRun { descending(MANY) }

            manyTime.shouldGrowAtMostBy(GROWTH, from = fewTime)
            descending(MANY).ordinalOf(1) shouldBe MANY - 1
        }
    }
})
