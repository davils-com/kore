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

package com.davils.kore.core.arguments.conversion

import com.davils.kore.core.identity.Uuid
import com.davils.kore.core.identity.UuidV4
import com.davils.kore.core.identity.UuidV7
import com.davils.kore.core.size.DataSize
import com.davils.kore.core.size.kibibytes
import com.davils.kore.core.size.mebibytes
import com.davils.kore.core.text.fixture.hostileText
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

private const val CASES: Int = 300

class SizeAndUuidConvertersTest : FunSpec({
    context("the data size converter") {
        test("reads binary sizes within its bounds") {
            val converter = Converters.dataSize(min = 1.kibibytes, max = 64.mebibytes)

            converter.convert("512 KiB") shouldBe 512.kibibytes
            converter.convert("64MiB") shouldBe 64.mebibytes
            converter.typeName shouldBe "size"
        }

        test("reads back every size it shows") {
            checkAll(CASES, Arb.long(min = 0L, max = Long.MAX_VALUE)) { bytes ->
                val size = DataSize(bytes)

                Converters.dataSize().convert(size.toString()) shouldBe size
            }
        }

        test("refuses a size below or above its bounds and names the bound") {
            val converter = Converters.dataSize(min = 1.kibibytes, max = 1.mebibytes)

            shouldThrow<ConversionException> { converter.convert("512 B") }.message shouldBe "is smaller than 1 KiB"
            shouldThrow<ConversionException> { converter.convert("2 MiB") }.message shouldBe "is larger than 1 MiB"
        }

        test("refuses decimal units and hostile text without repeating it") {
            val converter = Converters.dataSize()

            shouldThrow<ConversionException> { converter.convert("1 GB") }
            checkAll(hostileText.filter { text -> DataSize.parseOrNull(text) == null }) { text ->
                val failure = shouldThrow<ConversionException> { converter.convert(text) }

                failure.message shouldBe "is not a data size, expected something like 512 KiB or 64 MiB"
            }
        }

        test("refuses bounds that contradict each other") {
            shouldThrow<IllegalArgumentException> { Converters.dataSize(min = 2.kibibytes, max = 1.kibibytes) }
        }
    }

    context("the UUID converter") {
        test("reads identifiers of version 4 and 7") {
            val four = UuidV4.random()
            val seven = UuidV7.random()

            Converters.uuid.convert(four.toString()) shouldBe four
            Converters.uuid.convert(seven.toString().uppercase()) shouldBe seven
            Converters.uuid.typeName shouldBe "uuid"
        }

        test("refuses everything else with a constant reason") {
            val reasons = listOf("", "not-a-uuid", "00000000-0000-1000-8000-000000000000").map { text ->
                shouldThrow<ConversionException> { Converters.uuid.convert(text) }.message
            }

            reasons.toSet().size shouldBe 1
        }

        test("reads back every identifier it accepts") {
            checkAll(CASES, Arb.long()) { _ ->
                val uuid: Uuid = UuidV4.random()

                Converters.uuid.convert(uuid.toString()) shouldBe uuid
            }
        }
    }
})
