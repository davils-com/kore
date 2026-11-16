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
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreParameterViolationCodes
import com.davils.kore.core.arguments.resolution.fixture.PlainName
import com.davils.kore.core.arguments.resolution.fixture.SecretToken
import com.davils.kore.core.arguments.resolution.fixture.WeakSecret
import com.davils.kore.core.arguments.resolution.fixture.reachableTexts
import com.davils.kore.core.validation.violation.KoreSeverity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

private const val PLACEHOLDER: String = "placeholder-not-a-token"
private const val WITHHELD: String = "failed validation - the detail is withheld because the parameter is sensitive"

private fun secretFailure(): KoreParameterException =
    shouldThrow<KoreParameterException> { SecretToken().load(listOf("--token=$PLACEHOLDER"), NO_ENVIRONMENT) }

private fun validationFailureOf(violations: List<KoreParameterViolation>): KoreParameterViolation.ValidationFailure =
    violations.filterIsInstance<KoreParameterViolation.ValidationFailure>().single()

private infix fun KoreParameterViolation.shouldHoldNothingOf(secret: String) {
    val texts = reachableTexts(this)
    texts shouldContain WITHHELD
    texts.forEach { text -> withClue(texts) { text shouldNotContain secret } }
}

class KoreParametersSensitiveValidationTest : FunSpec({
    context("a rule on a sensitive parameter") {
        test("does not put a value it quotes in the report") {
            secretFailure().message shouldNotContain PLACEHOLDER
        }

        test("is reported with a fixed message and its code") {
            val violation = validationFailureOf(secretFailure().violations)

            violation.message shouldBe WITHHELD
            violation.code shouldBe KoreParameterViolationCodes.VALIDATION_FAILED
        }

        test("keeps its own words in no field and no method a caller can reach") {
            validationFailureOf(secretFailure().violations) shouldHoldNothingOf PLACEHOLDER
        }

        test("that only warns keeps its own words out of the warnings of the loaded schema") {
            val parameters = WeakSecret()

            parameters.load(listOf("--token=$PLACEHOLDER"), NO_ENVIRONMENT)

            val warning = validationFailureOf(parameters.warnings)
            warning.severity shouldBe KoreSeverity.Warning
            warning.message shouldBe WITHHELD
            warning shouldHoldNothingOf PLACEHOLDER
        }
    }

    context("a rule on a parameter that is not sensitive") {
        test("is reported in its own words") {
            val failure = shouldThrow<KoreParameterException> { PlainName().load(listOf("--name=ab"), NO_ENVIRONMENT) }

            failure.violations.single().message shouldBe "'ab' is too short"
        }
    }
})
