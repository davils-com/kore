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

package com.davils.kore.core.validation.report

import com.davils.kore.core.validation.KoreValidationScope
import com.davils.kore.core.validation.fixture.Probe
import com.davils.kore.core.validation.fixture.Shape
import com.davils.kore.core.validation.fixture.lines
import com.davils.kore.core.validation.validate
import com.davils.kore.core.validation.violation.KoreViolationPath
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val SUBJECT: String = "probe"
private const val NOT_EMPTY: String = "must not be empty"
private const val UNUSUAL: String = "is unusual"
private const val LONG_NAME: Int = 40
private const val CAPPED_GAP: Int = 25

private fun report(rules: KoreValidationScope.() -> Unit): KoreValidationReport = Probe(rules).validate(SUBJECT)

class KoreValidationReportTest : FunSpec({
    context("rendering") {
        test("says that a report without violations is valid") {
            report { }.render() shouldBe "Valid probe."
        }

        test("counts a single problem and a single warning in the singular") {
            val rendered = report {
                fail(Shape::sinks, NOT_EMPTY)
                warn(Shape::level, UNUSUAL)
            }.render()

            rendered shouldBe lines(
                "Invalid probe (1 problem):",
                "",
                "  sinks  must not be empty",
                "",
                "1 warning:",
                "",
                "  level  is unusual",
            )
        }

        test("counts several warnings in the plural and lists only warnings when nothing failed") {
            val rendered = report {
                warn(Shape::level, UNUSUAL)
                warn(Shape::sinks, UNUSUAL)
            }.toString()

            rendered shouldBe lines("2 warnings:", "", "  level  is unusual", "  sinks  is unusual")
        }

        test("caps the path column, so one long path does not push every message aside") {
            val longPath = KoreViolationPath.named("a".repeat(LONG_NAME))
            val rendered = report {
                include(Probe { fail(NOT_EMPTY) }.validate(SUBJECT).violations, longPath)
                fail(Shape::sinks, NOT_EMPTY)
            }.render()

            rendered shouldBe lines(
                "Invalid probe (2 problems):",
                "",
                "  ${"a".repeat(LONG_NAME)} must not be empty",
                "  sinks${" ".repeat(CAPPED_GAP)}must not be empty",
            )
        }
    }

    context("throwIfInvalid") {
        test("does nothing for a valid report") {
            shouldNotThrowAny { report { warn(UNUSUAL) }.throwIfInvalid() }
        }

        test("throws the report as an exception when an error was reported") {
            val invalid = report { fail(NOT_EMPTY) }

            shouldThrow<KoreValidationException> { invalid.throwIfInvalid() }.report shouldBe invalid
        }
    }

    context("subject") {
        test("is kept for the headline") {
            report { }.subject shouldBe SUBJECT
        }
    }
})
