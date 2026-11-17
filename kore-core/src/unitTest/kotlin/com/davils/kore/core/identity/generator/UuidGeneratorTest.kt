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

package com.davils.kore.core.identity.generator

import com.davils.kore.core.identity.Uuid
import com.davils.kore.core.identity.UuidV7
import com.davils.kore.core.identity.UuidVersion
import com.davils.kore.core.identity.fixture.V7_TEXT
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class UuidGeneratorTest : FunSpec({
    context("injection") {
        test("is a lambda, so a test can hand out fixed identifiers") {
            val fixed = UuidV7.parse(V7_TEXT)
            val generator = UuidGenerator { fixed }

            generator.next() shouldBe fixed
        }

        test("lets a generator of one version serve where any identifier is expected") {
            val generators: List<UuidGenerator<Uuid>> = listOf(UuidV4Generator, UuidV7Generator)

            generators.map { it.next().version } shouldBe listOf(
                UuidVersion.Random,
                UuidVersion.TimeOrdered,
            )
        }

        test("mints version 7 identifiers by default") {
            UuidV7Generator.next().shouldBeInstanceOf<UuidV7>()
        }
    }
})
