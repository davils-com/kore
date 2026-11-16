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

import com.davils.kore.core.identity.fixture.v4Of
import com.davils.kore.core.identity.fixture.v7Of
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.math.sign

private val versionSevenIdentifiers: Arb<KoreUuid> = arbitrary {
    v7Of(high = Arb.long().bind(), low = Arb.long().bind())
}

class KoreUuidPropertyTest : FunSpec({
    context("round trips") {
        test("every version 4 identifier survives the text, byte, Kotlin and Java forms") {
            checkAll(Arb.long(), Arb.long()) { high, low ->
                val uuid = v4Of(high = high, low = low)

                KoreUuidV4.parse(uuid.toString()) shouldBe uuid
                KoreUuid.fromByteArray(uuid.toByteArray()) shouldBe uuid
                KoreUuid.fromKotlinUuid(uuid.toKotlinUuid()) shouldBe uuid
                KoreUuid.fromJavaUuid(uuid.toJavaUuid()) shouldBe uuid
            }
        }

        test("every version 7 identifier survives the text and byte forms") {
            checkAll(Arb.long(), Arb.long()) { high, low ->
                val uuid = v7Of(high = high, low = low)

                KoreUuidV7.parse(uuid.toString()) shouldBe uuid
                KoreUuid.fromByteArray(uuid.toByteArray()) shouldBe uuid
            }
        }
    }

    context("ordering") {
        test("agrees with the order of the text forms") {
            checkAll(versionSevenIdentifiers, versionSevenIdentifiers) { left, right ->
                left.compareTo(right).sign shouldBe left.toString().compareTo(right.toString()).sign
            }
        }
    }
})
