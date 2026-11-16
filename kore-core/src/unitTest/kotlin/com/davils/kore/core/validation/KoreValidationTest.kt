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

package com.davils.kore.core.validation

import com.davils.kore.core.validation.fixture.ServerForm
import com.davils.kore.core.validation.fixture.Slot
import com.davils.kore.core.validation.fixture.paths
import com.davils.kore.core.validation.report.KoreValidationException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val SUBJECT: String = "server"

private val crowded = ServerForm(
    slots = listOf(Slot(name = "a", size = 1), Slot(name = "b", size = 1), Slot(name = "c", size = 1)),
)

class KoreValidationTest : FunSpec({
    context("validate") {
        test("reports nothing for a value with nothing wrong") {
            val report = ServerForm().validate(SUBJECT)

            report.isValid shouldBe true
            report.violations shouldHaveSize 0
        }

        test("runs every rule, so all problems are reported at once") {
            val form = ServerForm(name = "", owner = "", slots = listOf(Slot(name = "first", size = 0)))

            form.validate(SUBJECT).paths() shouldContainExactly listOf("name", "owner", "slots[0].size")
        }

        test("reports map entries under their key") {
            val form = ServerForm(labels = mapOf("rack" to "", "row" to "a"))

            form.validate(SUBJECT).paths() shouldContainExactly listOf("labels[\"rack\"]")
        }

        test("keeps a value with warnings valid") {
            val report = crowded.validate(SUBJECT)

            report.isValid shouldBe true
            report.warnings.single().code shouldBe ServerForm.TOO_MANY_SLOTS
        }
    }

    context("validateOrThrow") {
        test("hands the value back, so a boundary reads as one step") {
            val form = ServerForm()

            form.validateOrThrow(SUBJECT) shouldBe form
        }

        test("does not throw over warnings alone") {
            shouldNotThrowAny { crowded.validateOrThrow(SUBJECT) }
        }

        test("carries the whole report, not the first problem") {
            val failure = shouldThrow<KoreValidationException> {
                ServerForm(name = "", owner = "").validateOrThrow(SUBJECT)
            }

            failure.report.errors shouldHaveSize 2
            failure.message shouldContain "name"
            failure.message shouldContain "owner"
        }
    }
})
