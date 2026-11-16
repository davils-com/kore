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

import com.davils.kore.core.arguments.declaration.fixture.Sound
import com.davils.kore.core.arguments.declaration.fixture.StrayBesideBadInput
import com.davils.kore.core.arguments.declaration.fixture.StrayDependency
import com.davils.kore.core.arguments.declaration.fixture.StrayExclusive
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.KoreParameterException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val QUIET: String = "--quiet"

class KoreParametersGroupDeclarationTest : FunSpec({
    context("a parameter group naming a property that is not a parameter") {
        test("is refused when it is an exclusive group") {
            val failure = shouldThrow<IllegalArgumentException> { StrayExclusive().load(listOf(QUIET), NO_ENVIRONMENT) }

            failure.message shouldContain "'timeout'"
            failure.message shouldContain "not a declared parameter"
        }

        test("is refused when it is a dependency") {
            val failure = shouldThrow<IllegalArgumentException> {
                StrayDependency().load(listOf("--user", "kore"), NO_ENVIRONMENT)
            }

            failure.message shouldContain "'password'"
        }

        test("is refused before the resolving pass rather than during it") {
            val schema = StrayBesideBadInput()

            shouldThrow<IllegalArgumentException> { schema.load(listOf(QUIET), NO_ENVIRONMENT) }

            schema.isLoaded shouldBe false
        }
    }

    context("a group naming only declared parameters") {
        test("loads and still reports what the input got wrong") {
            val failure = shouldThrow<KoreParameterException> {
                Sound().load(listOf(QUIET, "--loud"), NO_ENVIRONMENT)
            }

            failure.violations.size shouldBe 1
        }
    }
})
