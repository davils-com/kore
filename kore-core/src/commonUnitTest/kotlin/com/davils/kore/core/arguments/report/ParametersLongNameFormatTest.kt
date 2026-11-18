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
import com.davils.kore.core.arguments.fixture.lines
import com.davils.kore.core.arguments.report.fixture.LongNames
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParametersLongNameFormatTest : FunSpec({
    context("a name longer than the column") {
        test("is still separated from its message and its annotations") {
            val expected = lines(
                "Invalid program parameters (2 problems):",
                "",
                "  --a-parameter-with-a-really-long-name is required but was not given",
                "  --b                           is required but was not given",
            )

            val failure = shouldThrow<ParameterException> { LongNames().load(emptyList(), NO_ENVIRONMENT) }

            failure.message shouldBe expected
        }

        test("is separated from its value when described") {
            val expected = lines(
                "Parameters:",
                "  --a-parameter-with-a-really-long-name 1 (from the command line)",
                "  --b                                 2 (from the command line)",
            )
            val parameters = LongNames()

            parameters.load(listOf("--a-parameter-with-a-really-long-name=1", "--b=2"), NO_ENVIRONMENT)

            parameters.describe() shouldBe expected
        }
    }
})
