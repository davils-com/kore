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

package com.davils.kore.core.arguments.conversion

import com.davils.kore.core.arguments.conversion.fixture.Release
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.ParameterException
import com.davils.kore.core.arguments.report.ParameterViolation
import com.davils.kore.core.version.SemanticVersion
import com.davils.kore.core.version.SemanticVersionRange
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

class ParametersVersionConversionTest : FunSpec({
    context("a version and a range") {
        test("are read") {
            val parameters = Release()

            parameters.load(listOf("--version=2.0.0-rc.1", "--accepts=^1.2 || ^2"), NO_ENVIRONMENT)

            parameters.version shouldBe SemanticVersion.parse("2.0.0-rc.1")
            parameters.accepts shouldBe SemanticVersionRange.parse("^1.2 || ^2")
        }

        test("that are malformed are both reported, with the reason and without the value") {
            val failure = shouldThrow<ParameterException> {
                Release().load(listOf("--version=v1.02", "--accepts=>2 <1"), NO_ENVIRONMENT)
            }

            failure.violations shouldHaveSize 2
            val reasons = failure.violations.map { violation ->
                violation.shouldBeInstanceOf<ParameterViolation.ConversionFailure>().reason
            }
            reasons shouldBe listOf(
                "is not a semantic version, expected something like 1.4.0 or 2.0.0-rc.1",
                "is not a version range, expected something like ^1.2 or >=1.0.0 <2.0.0",
            )
            reasons.forEach { reason -> reason shouldNotContain "v1.02" }
        }

        test("are named by their types in the usage listing") {
            val usage = Release().usage()

            usage shouldContain "<version>"
            usage shouldContain "<range>"
        }
    }
})
