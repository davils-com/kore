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

package com.davils.kore.core.validation.violation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val MESSAGE: String = "must not be blank"

private fun violation(
    message: String = MESSAGE,
    severity: Severity = Severity.Error,
    code: ViolationCode = ViolationCode.INVALID,
    path: ViolationPath = ViolationPath.named("name"),
): Violation = Violation(path = path, code = code, severity = severity, message = message)

class ViolationTest : FunSpec({
    context("equality") {
        test("treats violations with the same content as equal with the same hash") {
            violation() shouldBe violation()
            violation().hashCode() shouldBe violation().hashCode()
        }

        test("tells violations apart by every part") {
            violation(path = ViolationPath.named("owner")) shouldNotBe violation()
            violation(code = ViolationCode.REQUIRED) shouldNotBe violation()
            violation(severity = Severity.Warning) shouldNotBe violation()
            violation(message = "is unusual") shouldNotBe violation()
        }

        test("is never equal to something that is not a violation") {
            violation().equals(MESSAGE) shouldBe false
        }
    }

    context("rendering") {
        test("names severity, path, code and message") {
            violation().toString() shouldBe "Error at 'name' (kore.validation.invalid): must not be blank"
        }
    }

    context("relocation") {
        test("moves the violation under a prefix and keeps everything else") {
            val moved = violation().under(ViolationPath.named("server"))

            moved shouldBe violation(path = ViolationPath.named("server").name("name"))
        }
    }
})
