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
import com.davils.kore.core.arguments.resolution.fixture.EmptyDelimiter
import com.davils.kore.core.arguments.resolution.fixture.SplitTags
import com.davils.kore.core.arguments.resolution.fixture.TypedEmptyDelimiter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain

private const val HTTP: Int = 80
private const val HTTPS: Int = 443

private fun tags(argument: String): SplitTags {
    val parameters = SplitTags()
    parameters.load(listOf(argument), NO_ENVIRONMENT)
    return parameters
}

class ParametersSplitTest : FunSpec({
    context("an empty delimiter") {
        test("is refused when the schema is constructed") {
            shouldThrow<IllegalArgumentException> { EmptyDelimiter() }.message shouldContain "must not be empty"
        }

        test("is refused on a typed repeatable parameter as well") {
            shouldThrow<IllegalArgumentException> { TypedEmptyDelimiter() }
        }
    }

    context("a split value") {
        test("adds no empty value for a trailing delimiter") {
            tags("--tag=a,b,").tags shouldContainExactly listOf("a", "b")
        }

        test("adds no empty value for two delimiters in a row") {
            tags("--tag=a,,b").tags shouldContainExactly listOf("a", "b")
        }

        test("never hands an empty piece to the converter") {
            tags("--port=80,,443,").ports shouldContainExactly listOf(HTTP, HTTPS)
        }
    }
})
