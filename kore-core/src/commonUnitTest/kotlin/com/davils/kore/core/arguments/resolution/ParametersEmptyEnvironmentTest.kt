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

import com.davils.kore.core.arguments.report.ParameterException
import com.davils.kore.core.arguments.report.ParameterViolation
import com.davils.kore.core.arguments.resolution.fixture.Mandated
import com.davils.kore.core.arguments.resolution.fixture.Prefixed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ParametersEmptyEnvironmentTest : FunSpec({
    context("an environment variable that is set to nothing") {
        test("does not satisfy a required parameter") {
            val failure = shouldThrow<ParameterException> { Mandated().load(emptyList(), mapOf("APP_TOKEN" to "")) }

            failure.violations.single().shouldBeInstanceOf<ParameterViolation.MissingRequired>()
        }

        test("leaves an optional parameter unset") {
            val parameters = Prefixed()

            parameters.load(emptyList(), mapOf("APP_DATA_DIR" to "", "APP_PORT" to ""))

            parameters.dataDir.shouldBeNull()
            parameters.port.shouldBeNull()
        }

        test("lets a flag keep its default") {
            val parameters = Prefixed()

            parameters.load(emptyList(), mapOf("APP_EAGER" to ""))

            parameters.isEager shouldBe false
        }
    }
})
