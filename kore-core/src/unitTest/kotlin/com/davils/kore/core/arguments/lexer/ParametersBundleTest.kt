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

package com.davils.kore.core.arguments.lexer

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.lexer.fixture.BundleCollecting
import com.davils.kore.core.arguments.lexer.fixture.BundleRejecting
import com.davils.kore.core.arguments.report.ParameterException
import com.davils.kore.core.arguments.report.ParameterViolation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PORT: Int = 8080
private const val BUNDLE: String = "-axc"
private const val UNDECLARED: String = "-x"

private fun rejected(argument: String): ParameterViolation = shouldThrow<ParameterException> {
    BundleRejecting().load(listOf(argument), NO_ENVIRONMENT)
}.violations.single()

class ParametersBundleTest : FunSpec({
    context("a bundle with an undeclared letter, when unknown arguments are rejected") {
        test("is reported by the letter and the argument it was in") {
            val violation = rejected(BUNDLE)

            violation.shouldBeInstanceOf<ParameterViolation.UnknownArgument>()
            violation.token shouldBe UNDECLARED
            violation.bundlePosition shouldBe 1
            violation.message shouldBe "is not a known parameter, so nothing in argument 1 was applied"
        }

        test("is reported even when the undeclared letter comes first") {
            val violation = rejected("-xac")

            violation.shouldBeInstanceOf<ParameterViolation.UnknownArgument>()
            violation.token shouldBe UNDECLARED
            violation.bundlePosition shouldBe 1
        }
    }

    context("a bundle with an undeclared letter, when unknown arguments are collected") {
        test("applies none of its letters") {
            val parameters = BundleCollecting()

            parameters.load(listOf(BUNDLE), NO_ENVIRONMENT)

            parameters.isAll shouldBe false
            parameters.isColoured shouldBe false
        }

        test("is collected as the token that was passed") {
            val parameters = BundleCollecting()

            parameters.load(listOf(BUNDLE), NO_ENVIRONMENT)

            parameters.unknown shouldContainExactly listOf(BUNDLE)
        }
    }

    context("a bundle of declared letters") {
        test("applies every one of them") {
            val parameters = BundleRejecting()

            parameters.load(listOf("-ac"), NO_ENVIRONMENT)

            parameters.isAll shouldBe true
            parameters.isColoured shouldBe true
        }

        test("may end in a parameter whose value is attached") {
            val parameters = BundleRejecting()

            parameters.load(listOf("-ap8080"), NO_ENVIRONMENT)

            parameters.isAll shouldBe true
            parameters.port shouldBe PORT
        }

        test("treats what follows a value-taking letter as its value, not as more letters") {
            rejected("-apxyz").shouldBeInstanceOf<ParameterViolation.ConversionFailure>()
        }

        test("may give a flag its value after an equals sign") {
            val parameters = BundleRejecting()

            parameters.load(listOf("-ca=false"), NO_ENVIRONMENT)

            parameters.isColoured shouldBe true
            parameters.isAll shouldBe false
        }
    }

    context("a single undeclared short name") {
        test("is reported as it is, without a bundle") {
            val violation = rejected(UNDECLARED)

            violation.shouldBeInstanceOf<ParameterViolation.UnknownArgument>()
            violation.bundlePosition.shouldBeNull()
            violation.message shouldBe "is not a known parameter"
        }
    }
})
