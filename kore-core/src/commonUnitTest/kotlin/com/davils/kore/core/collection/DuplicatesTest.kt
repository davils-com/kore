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

package com.davils.kore.core.collection

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

private const val SMALL: Int = 5
private const val LONGEST: Int = 40

private val smallNumbers: Arb<List<Int>> = Arb.list(Arb.int(0..SMALL), 0..LONGEST)

private fun repeatedByCount(elements: List<Int>): Set<Int> {
    val counts = elements.groupingBy { element -> element }.eachCount()
    return counts.filterValues { count -> count > 1 }.keys
}

class DuplicatesTest : FunSpec({
    context("duplicates") {
        test("is empty when every element is distinct") {
            val distinct = listOf("a", "b", "c")
            val nothing = emptyList<String>()

            distinct.duplicates().shouldBeEmpty()
            nothing.duplicates().shouldBeEmpty()
        }

        test("names each repeated element once, in the order of its second occurrence") {
            val repeated = listOf("b", "a", "a", "b", "b", "c").duplicates()

            repeated.toList() shouldContainExactly listOf("a", "b")
        }

        test("finds a repeated null") {
            val repeated = listOf(null, "a", null).duplicates()

            repeated.toList() shouldContainExactly listOf(null)
        }

        test("agrees with counting the elements") {
            checkAll(smallNumbers) { elements -> elements.duplicates() shouldBe repeatedByCount(elements) }
        }

        test("cannot be changed by a caller") {
            val repeated = listOf("a", "a").duplicates()

            shouldThrowAny { (repeated as MutableSet<String>).clear() }
            shouldThrowAny { (repeated.iterator() as MutableIterator<String>).remove() }
        }
        test("ends its iteration like any other set") {
            val repeated = listOf("a", "a").duplicates()
            val iterator = repeated.iterator()

            iterator.next() shouldBe "a"
            shouldThrow<NoSuchElementException> { iterator.next() }
        }
    }

    context("duplicatesBy") {
        test("compares the keys of the elements") {
            val names = listOf("port", "Port", "host", "HOST", "path")

            val repeated = names.duplicatesBy { name -> name.lowercase() }

            repeated.toList() shouldContainExactly listOf("port", "host")
        }
    }

    context("firstDuplicateOrNull") {
        test("returns the first element found twice") {
            val elements = listOf("b", "a", "a", "b")

            elements.firstDuplicateOrNull() shouldBe "a"
        }

        test("returns null when every element is distinct") {
            val elements = listOf("a", "b")

            elements.firstDuplicateOrNull().shouldBeNull()
        }

        test("agrees with the first element of duplicates") {
            checkAll(smallNumbers) { elements ->
                elements.firstDuplicateOrNull() shouldBe elements.duplicates().firstOrNull()
            }
        }
    }
})
