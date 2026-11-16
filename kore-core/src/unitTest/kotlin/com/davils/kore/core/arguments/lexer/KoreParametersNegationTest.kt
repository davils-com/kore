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
import com.davils.kore.core.arguments.lexer.fixture.CacheFirst
import com.davils.kore.core.arguments.lexer.fixture.NegationPlain
import com.davils.kore.core.arguments.lexer.fixture.NoCacheFirst
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val NEGATED_PORT: String = "--no-port"
private const val NO_CACHE: String = "--no-cache"

class KoreParametersNegationTest : FunSpec({
    context("a parameter that is not negatable") {
        test("does not answer to its negated form") {
            val failure = shouldThrow<KoreParameterException> {
                NegationPlain().load(listOf(NEGATED_PORT, "9090"), NO_ENVIRONMENT)
            }

            val unknown = failure.violations.first()
            unknown.shouldBeInstanceOf<KoreParameterViolation.UnknownArgument>()
            unknown.token shouldBe NEGATED_PORT
            val stray = failure.violations.last()
            stray.shouldBeInstanceOf<KoreParameterViolation.StrayValue>()
            stray.position shouldBe 2
        }

        test("is never set through its negated form") {
            val parameters = NegationPlain()

            shouldThrow<KoreParameterException> { parameters.load(listOf(NEGATED_PORT, "9090"), NO_ENVIRONMENT) }

            parameters.isLoaded shouldBe false
        }
    }

    context("a negatable flag") {
        test("answers to its negated form") {
            val parameters = NegationPlain()

            parameters.load(listOf("--no-colour"), NO_ENVIRONMENT)

            parameters.isColoured shouldBe false
        }

        test("refuses a value on its negated form") {
            val failure = shouldThrow<KoreParameterException> {
                NegationPlain().load(listOf("--no-colour=yes"), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.MalformedArgument>()
            violation.reason shouldBe "is a negated flag and does not take a value"
        }
    }

    context("a declared --no- parameter") {
        test("wins over the phantom form, whichever is declared first") {
            val first = CacheFirst()
            first.load(listOf(NO_CACHE), NO_ENVIRONMENT)
            val second = NoCacheFirst()
            second.load(listOf(NO_CACHE), NO_ENVIRONMENT)

            first.isNotCaching shouldBe true
            first.isCaching shouldBe false
            second.isNotCaching shouldBe true
            second.isCaching shouldBe false
        }
    }
})
