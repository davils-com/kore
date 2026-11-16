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

package com.davils.kore.core.arguments.declaration

import com.davils.kore.core.arguments.declaration.fixture.DerivedCollision
import com.davils.kore.core.arguments.declaration.fixture.EmptyVariable
import com.davils.kore.core.arguments.declaration.fixture.ExplicitCollision
import com.davils.kore.core.arguments.declaration.fixture.MalformedVariable
import com.davils.kore.core.arguments.declaration.fixture.SeparatedByNoEnv
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val DASHED_VALUE: Int = 7

class KoreParametersEnvironmentClaimTest : FunSpec({
    context("an environment variable") {
        test("derived by two parameters is refused when the schema is constructed") {
            val failure = shouldThrow<IllegalArgumentException> { DerivedCollision() }

            failure.message shouldContain "'dashed' and 'underscored' both read the environment variable 'APP_DATA_DIR'"
        }

        test("declared by two parameters is refused when the schema is constructed") {
            shouldThrow<IllegalArgumentException> { ExplicitCollision() }.message shouldContain "'APP_TOKEN'"
        }

        test("that no shell could export is refused") {
            shouldThrow<IllegalArgumentException> { MalformedVariable() }.message shouldContain
                "is not a well-formed variable name"
        }

        test("with an empty name is refused") {
            shouldThrow<IllegalArgumentException> { EmptyVariable() }
        }

        test("is shared without a collision when one of the parameters opts out") {
            val parameters = SeparatedByNoEnv()

            parameters.load(emptyList(), mapOf("APP_DATA_DIR" to "7"))

            parameters.dashed shouldBe DASHED_VALUE
            parameters.underscored.shouldBeNull()
        }
    }
})
