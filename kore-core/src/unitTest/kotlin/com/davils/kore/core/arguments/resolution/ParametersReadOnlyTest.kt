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

package com.davils.kore.core.arguments.resolution

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.ParameterViolationCodes
import com.davils.kore.core.arguments.resolution.fixture.Tagged
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val HTTP: Int = 80
private const val HTTPS: Int = 443
private const val TAG: String = "--tag"

private fun loaded(vararg argv: String): Tagged {
    val parameters = Tagged()
    parameters.load(argv.toList(), NO_ENVIRONMENT)
    return parameters
}

class ParametersReadOnlyTest : FunSpec({
    context("the values a loaded schema hands out") {
        test("a repeated parameter's values are the same for every reader") {
            val parameters = loaded(TAG, "a", TAG, "b")

            parameters.tags shouldContainExactly listOf("a", "b")
            parameters.tags shouldBe parameters.tags
        }

        test("a repeated parameter's default is a copy of the declared one") {
            loaded().ports shouldContainExactly listOf(HTTP, HTTPS)
        }

        test("the arguments after the terminator are kept in order") {
            loaded("--", "one", "two").rest shouldContainExactly listOf("one", "two")
        }

        test("the collected unknown arguments are kept in order") {
            loaded("--one", "--two").unknown shouldContainExactly listOf("--one", "--two")
        }

        test("the warnings name what was deprecated") {
            loaded("--legacy").warnings.single().code shouldBe ParameterViolationCodes.DEPRECATED
        }
    }

    context("a schema loaded with other arguments") {
        test("does not change what an earlier schema hands out") {
            val first = loaded(TAG, "a")

            loaded(TAG, "b")

            first.tags shouldContainExactly listOf("a")
        }
    }
})
