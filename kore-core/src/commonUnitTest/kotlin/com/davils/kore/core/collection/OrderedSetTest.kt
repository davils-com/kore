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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val COLLIDING_BITS: Int = 10
private const val ELEMENTS: Int = 3

class OrderedSetTest : FunSpec({
    context("add and contains") {
        test("hold every element once, in the order it first came") {
            val set = OrderedSet(listOf("b", "a", "b"))

            set.add("c") shouldBe true
            set.add("a") shouldBe false
            set.toList() shouldContainExactly listOf("b", "a", "c")
            ("a" in set) shouldBe true
            ("d" in set) shouldBe false
            set.size shouldBe ELEMENTS
        }

        test("start empty without elements") {
            val set = OrderedSet<String>()

            set.size shouldBe 0
            set.toList().shouldBeEmpty()
        }

        test("keep elements with the same hash code apart") {
            val keys = collidingKeys(COLLIDING_BITS)
            val set = OrderedSet(keys)

            set.size shouldBe keys.size
            set.toList() shouldContainExactly keys
        }
    }

    context("remove and clear") {
        test("remove an element and report whether it was there") {
            val set = OrderedSet(listOf("a", "b"))

            set.remove("a") shouldBe true
            set.remove("a") shouldBe false
            set.toList() shouldContainExactly listOf("b")
            set.size shouldBe 1
        }

        test("forget every element when cleared") {
            val set = OrderedSet(listOf("a", "b"))
            set.clear()

            set.toList().shouldBeEmpty()
            set.add("b") shouldBe true
        }
    }

    context("iterator") {
        test("walks a copy that later changes do not reach") {
            val set = OrderedSet(listOf("a", "b"))
            val iterator = set.iterator()
            set.add("c")
            set.remove("a")

            iterator.asSequence().toList() shouldContainExactly listOf("a", "b")
        }
    }

    context("equality") {
        test("equals every set with the same elements in any order") {
            val set = OrderedSet(listOf("b", "a"))

            set shouldBe setOf("a", "b")
            set.hashCode() shouldBe setOf("a", "b").hashCode()
            set shouldNotBe setOf("a")
        }
    }
})
