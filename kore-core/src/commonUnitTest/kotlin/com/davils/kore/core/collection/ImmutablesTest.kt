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
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ImmutablesTest : FunSpec({
    context("immutableList") {
        test("keeps the elements in order and is not changed by the original") {
            val original = mutableListOf("a", "b")

            val copy = original.immutableList()
            original += "c"

            copy shouldContainExactly listOf("a", "b")
        }

        test("cannot be changed by a caller") {
            val copy = listOf("a").immutableList()

            shouldThrowAny { (copy as MutableList<String>).clear() }
        }
    }

    context("immutableSet") {
        test("drops duplicates and cannot be changed by a caller") {
            val copy = listOf("a", "a", "b").immutableSet()

            copy shouldBe setOf("a", "b")
            shouldThrowAny { (copy as MutableSet<String>).clear() }
        }
    }

    context("immutableMap") {
        test("keeps the entries and cannot be changed by a caller") {
            val copy = mapOf("a" to 1).immutableMap()

            copy shouldBe mapOf("a" to 1)
            shouldThrowAny { (copy as MutableMap<String, Int>).clear() }
        }
    }
})
