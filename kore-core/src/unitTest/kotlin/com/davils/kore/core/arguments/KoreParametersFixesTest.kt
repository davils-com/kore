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

package com.davils.kore.core.arguments

import com.davils.kore.core.arguments.fixture.GuardedPaths
import com.davils.kore.core.arguments.fixture.Lenient
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.RepeatedChoices
import com.davils.kore.core.arguments.fixture.ReversedGroup
import com.davils.kore.core.arguments.fixture.Strict
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreValueOrigin
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Path

private const val SECRET: String = "SECRET"
private const val READ_AS_PATH: String = "could not be read as path"

class KoreParametersFixesTest : FunSpec({
    context("an unknown long argument with a value, when unknown arguments are collected") {
        test("is collected whole rather than cut at its equals sign") {
            val parameters = Lenient()

            parameters.load(listOf("--name=kore", "--theirs=x", "stray"), NO_ENVIRONMENT)

            parameters.unknown shouldContainExactly listOf("--theirs=x", "stray")
        }
    }

    context("an unknown short token carrying more than its letter") {
        test("is reported by its letter and position without quoting the rest") {
            val failure = shouldThrow<KoreParameterException> { Strict().load(listOf("-p$SECRET"), NO_ENVIRONMENT) }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.UnknownArgument>()
            violation.token shouldBe "-p"
            violation.path.toString() shouldBe "-p"
            violation.bundlePosition shouldBe 1
            failure.message shouldNotContain SECRET
        }
    }

    context("a choice declared as the type of a repeatable parameter") {
        test("keeps its constraint for every given value") {
            val failure = shouldThrow<KoreParameterException> {
                RepeatedChoices().load(listOf("--level=low", "--level=medium"), NO_ENVIRONMENT)
            }

            failure.violations.single().message shouldBe "'medium' is not one of low, high"
        }
    }

    context("a path requirement check that throws") {
        test("is reported as a conversion failure for a given value and for a default") {
            val failure = shouldThrow<KoreParameterException> {
                GuardedPaths(Path.of("fallback")).load(listOf("--data=/srv"), NO_ENVIRONMENT)
            }

            val given = failure.violations.first()
            val fallback = failure.violations.last()
            given.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            given.reason shouldBe READ_AS_PATH
            given.cause.shouldBeInstanceOf<SecurityException>()
            fallback.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            fallback.origin shouldBe KoreValueOrigin.Default
            fallback.reason shouldBe READ_AS_PATH
        }
    }

    context("an exclusive group") {
        test("names its members in the order the group was declared") {
            val failure = shouldThrow<KoreParameterException> {
                ReversedGroup().load(listOf("--token=a", "--key-file=b"), NO_ENVIRONMENT)
            }

            failure.violations
                .single()
                .path
                .toString() shouldBe "--key-file, --token"
        }
    }
})
