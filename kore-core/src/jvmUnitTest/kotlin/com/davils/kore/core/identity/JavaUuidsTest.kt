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

package com.davils.kore.core.identity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.util.UUID

private const val RANDOM: Int = 4
private const val TIME_ORDERED: Int = 7

class JavaUuidsTest : FunSpec({
    context("interoperability with java.util.UUID") {
        test("hands out the Java identifier and reads it back") {
            val uuid = Uuid.parse(V4_TEXT)

            uuid.toJavaUuid() shouldBe UUID.fromString(V4_TEXT)
            Uuid.fromJavaUuid(uuid.toJavaUuid()) shouldBe uuid
        }

        test("refuses a foreign identifier of an unsupported version") {
            shouldThrow<IllegalArgumentException> { Uuid.fromJavaUuid(UUID.fromString(V1_TEXT)) }
        }

        test("exposes both halves of the 128 bits") {
            val uuid = Uuid.parse(V4_TEXT)
            val java = UUID.fromString(V4_TEXT)

            uuid.mostSignificantBits shouldBe java.mostSignificantBits
            uuid.leastSignificantBits shouldBe java.leastSignificantBits
        }

        test("every version 4 identifier survives the Java form") {
            checkAll(Arb.long(), Arb.long()) { high, low ->
                val uuid = v4Of(high = high, low = low)

                Uuid.fromJavaUuid(uuid.toJavaUuid()) shouldBe uuid
                uuid.toJavaUuid().version() shouldBe RANDOM
            }
        }

        test("hands out a version 7 identifier as a Java identifier of version 7") {
            val java = UuidV7.random().toJavaUuid()

            java.version() shouldBe TIME_ORDERED
        }
    }
})
