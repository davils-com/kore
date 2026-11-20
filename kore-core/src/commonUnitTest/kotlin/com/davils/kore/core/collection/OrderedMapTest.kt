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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll

private const val MOST_STEPS: Int = 300
private const val KEY_RANGE: Int = 30
private const val REMOVAL: Int = 0
private const val THIRD: Int = 3
private const val FEW_BITS: Int = 13
private const val MANY_BITS: Int = 15
private const val GROWTH: Int = 5

private fun keyOf(number: Int): String = "k$number"

private fun filledMap(keys: List<String>): OrderedMap<String, Int> {
    val map = OrderedMap<String, Int>()
    keys.forEachIndexed { value, key -> map[key] = value }
    return map
}

class OrderedMapTest : FunSpec({
    context("set, get and contains") {
        test("store, replace and find values by key") {
            val map = OrderedMap<String, Int>()
            map["b"] = 1
            map["a"] = 2
            map["b"] = THIRD

            map["b"] shouldBe THIRD
            map["a"] shouldBe 2
            map["c"].shouldBeNull()
            ("a" in map) shouldBe true
            ("c" in map) shouldBe false
            map.size shouldBe 2
        }
    }

    context("keys and entries") {
        test("list the keys and entries in the order the keys first came") {
            val map = OrderedMap<String, Int>()
            map["b"] = 1
            map["a"] = 2
            map["b"] = THIRD

            map.keys shouldContainExactly listOf("b", "a")
            map.entries shouldContainExactly listOf("b" to THIRD, "a" to 2)
        }

        test("are copies that later changes do not reach") {
            val map = OrderedMap<String, Int>()
            map["a"] = 1
            val keys = map.keys
            val entries = map.entries
            map["b"] = 2

            keys shouldContainExactly listOf("a")
            entries shouldContainExactly listOf("a" to 1)
        }
    }

    context("getOrPut") {
        test("puts a value only when the key has none") {
            val map = OrderedMap<String, Int>()

            map.getOrPut("a") { 1 } shouldBe 1
            map.getOrPut("a") { 2 } shouldBe 1
            map.size shouldBe 1
        }

        test("leaves the map unchanged when the value cannot be made") {
            val map = OrderedMap<String, Int>()

            shouldThrow<IllegalStateException> { map.getOrPut("a") { error("no value") } }

            map.size shouldBe 0
            map.keys.shouldBeEmpty()
        }
    }

    context("remove") {
        test("returns the removed value and nothing for a key without one") {
            val map = OrderedMap<String, Int>()
            map["a"] = 1
            map["b"] = 2

            map.remove("a") shouldBe 1
            map.remove("a").shouldBeNull()
            map.remove("c").shouldBeNull()
            map["a"].shouldBeNull()
            map.size shouldBe 1
            map.keys shouldContainExactly listOf("b")
        }

        test("keeps the place of a key that comes back") {
            val map = OrderedMap<String, Int>()
            map["a"] = 1
            map["b"] = 2
            map.remove("a")
            map["a"] = THIRD

            map["a"] shouldBe THIRD
            map.size shouldBe 2
            map.keys shouldContainExactly listOf("a", "b")
        }
    }

    context("clear") {
        test("forgets every key and its place") {
            val map = OrderedMap<String, Int>()
            map["a"] = 1
            map["b"] = 2
            map.clear()

            map.size shouldBe 0
            map.entries.shouldBeEmpty()
            map["a"].shouldBeNull()
            map["b"] = 2
            map["a"] = 1
            map.keys shouldContainExactly listOf("b", "a")
        }
    }

    context("a sequence of changes") {
        test("agrees with a hash map of the same changes") {
            val step = Arb.pair(k = Arb.int(0 until KEY_RANGE), v = Arb.int(REMOVAL..KEY_RANGE))
            checkAll(Arb.list(step, 0..MOST_STEPS)) { steps ->
                val map = OrderedMap<String, Int>()
                val expected = HashMap<String, Int>()
                steps.forEach { (number, value) ->
                    val key = keyOf(number)
                    if (value == REMOVAL) map.remove(key) shouldBe expected.remove(key)
                    if (value != REMOVAL) map[key] = value
                    if (value != REMOVAL) expected[key] = value
                }

                map.size shouldBe expected.size
                for (number in 0 until KEY_RANGE) map[keyOf(number)] shouldBe expected[keyOf(number)]
            }
        }
    }

    context("keys with the same hash code") {
        test("keep their own values") {
            val keys = collidingKeys(FEW_BITS)
            val map = filledMap(keys)

            map.size shouldBe keys.size
            keys.forEachIndexed { value, key -> map[key] shouldBe value }
        }

        test("take time that grows with the number of keys and not with its square") {
            val few = collidingKeys(FEW_BITS)
            val many = collidingKeys(MANY_BITS)

            val fewTime = fastestRun { filledMap(few) }
            val manyTime = fastestRun { filledMap(many) }

            manyTime.shouldGrowAtMostBy(GROWTH, from = fewTime)
        }
    }
})
