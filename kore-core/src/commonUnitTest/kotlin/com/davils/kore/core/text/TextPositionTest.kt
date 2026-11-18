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

package com.davils.kore.core.text

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val LINE: Int = 3
private const val COLUMN: Int = 7

class TextPositionTest : FunSpec({
    context("construction") {
        test("refuses a negative line") {
            shouldThrow<IllegalArgumentException> { TextPosition(line = -1, column = 0) }
        }

        test("refuses a negative column") {
            shouldThrow<IllegalArgumentException> { TextPosition(line = 0, column = -1) }
        }
    }

    context("order") {
        test("sorts by line and then by column") {
            val positions = listOf(
                TextPosition(line = LINE, column = 0),
                TextPosition(line = 0, column = COLUMN),
                TextPosition(line = LINE, column = COLUMN),
            )

            positions.sorted() shouldBe listOf(
                TextPosition(line = 0, column = COLUMN),
                TextPosition(line = LINE, column = 0),
                TextPosition(line = LINE, column = COLUMN),
            )
        }
    }

    context("text form") {
        test("counts lines and columns from one") {
            TextPosition(line = LINE, column = COLUMN).toString() shouldBe "4:8"
        }

        test("counts the last possible line and column from one without overflowing") {
            val last = TextPosition(line = Int.MAX_VALUE, column = Int.MAX_VALUE)

            last.toString() shouldBe "2147483648:2147483648"
        }
    }

    context("value semantics") {
        test("equals a position with the same line and column and shares its hash code") {
            val same = TextPosition(line = LINE, column = COLUMN)

            same shouldBe TextPosition(line = LINE, column = COLUMN)
            same.hashCode() shouldBe TextPosition(line = LINE, column = COLUMN).hashCode()
        }

        test("differs from a position on another line or in another column") {
            val position = TextPosition(line = LINE, column = COLUMN)

            position shouldNotBe TextPosition(line = 0, column = COLUMN)
            position shouldNotBe TextPosition(line = LINE, column = 0)
        }
    }
})
