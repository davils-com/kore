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

package com.davils.kore.core.arguments.report

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.fixture.Many
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

private const val HUGE_TOKEN_LENGTH: Int = 60_000
private const val TIMEOUT_SECONDS: Int = 10

private fun unknownOf(argument: String): ParameterViolation.UnknownArgument {
    val failure = shouldThrow<ParameterException> { Many().load(listOf(argument), NO_ENVIRONMENT) }
    return failure.violations.single().shouldBeInstanceOf<ParameterViolation.UnknownArgument>()
}

class ParametersSuggestionTest : FunSpec({
    context("an unknown argument") {
        test("that is a near miss still gets a suggestion") {
            unknownOf("--moed").suggestion shouldBe "--mode"
        }

        test("far longer than any name gets no suggestion") {
            unknownOf("--parameter-number-0-and-a-great-deal-more-text").suggestion.shouldBeNull()
        }

        test("that is too short to compare gets no suggestion") {
            unknownOf("--x").suggestion.shouldBeNull()
        }

        test("that is huge is reported without searching for a suggestion").config(timeout = TIMEOUT_SECONDS.seconds) {
            unknownOf("--" + "a".repeat(HUGE_TOKEN_LENGTH)).suggestion.shouldBeNull()
        }
    }
})
