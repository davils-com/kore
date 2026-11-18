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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ViolationCodeTest : FunSpec({
    context("format") {
        test("accepts dotted lowercase segments with hyphens and digits") {
            ViolationCode("kore.parameter.missing-required2").value shouldBe "kore.parameter.missing-required2"
        }

        context("rejects a code that cannot serve as a stable key") {
            listOf("", "kore", "Kore.validation", "kore..invalid", "kore.validation.", "kore.in valid", "1kore.x")
                .forEach { code ->
                    test("rejects '$code'") {
                        shouldThrow<IllegalArgumentException> { ViolationCode(code) }
                    }
                }
        }
    }

    context("rendering") {
        test("shows the code itself") {
            ViolationCode.INVALID.toString() shouldBe "kore.validation.invalid"
        }
    }

    context("predefined codes") {
        test("live in the kore validation namespace") {
            listOf(
                ViolationCode.INVALID,
                ViolationCode.REQUIRED,
                ViolationCode.DEPRECATED,
                ViolationCode.DUPLICATE,
            ).map { it.value } shouldBe listOf(
                "kore.validation.invalid",
                "kore.validation.required",
                "kore.validation.deprecated",
                "kore.validation.duplicate",
            )
        }
    }
})
