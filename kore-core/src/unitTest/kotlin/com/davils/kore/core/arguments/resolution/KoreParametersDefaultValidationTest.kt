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

import com.davils.kore.core.arguments.fixture.GOOD_DEFAULT_PORT
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.PORT_MESSAGE
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreValueOrigin
import com.davils.kore.core.arguments.resolution.fixture.BadDefault
import com.davils.kore.core.arguments.resolution.fixture.BadDefaultList
import com.davils.kore.core.arguments.resolution.fixture.GoodDefault
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val GIVEN_PORT: Int = 9090

class KoreParametersDefaultValidationTest : FunSpec({
    context("a default") {
        test("that fails the parameter's own rules is reported") {
            val failure = shouldThrow<KoreParameterException> { BadDefault().load(emptyList(), NO_ENVIRONMENT) }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.ValidationFailure>()
            violation.message shouldBe PORT_MESSAGE
            violation.origin shouldBe KoreValueOrigin.Default
        }

        test("that satisfies them is used as before") {
            val parameters = GoodDefault()

            parameters.load(emptyList(), NO_ENVIRONMENT)

            parameters.port shouldBe GOOD_DEFAULT_PORT
        }

        test("list is checked element by element") {
            val failure = shouldThrow<KoreParameterException> { BadDefaultList().load(emptyList(), NO_ENVIRONMENT) }

            failure.violations.map { it.message } shouldContainExactly listOf(PORT_MESSAGE)
        }

        test("is overridden by a value given on the command line, which is checked itself") {
            val parameters = GoodDefault()

            parameters.load(listOf("--port", "9090"), NO_ENVIRONMENT)

            parameters.port shouldBe GIVEN_PORT
        }
    }
})
