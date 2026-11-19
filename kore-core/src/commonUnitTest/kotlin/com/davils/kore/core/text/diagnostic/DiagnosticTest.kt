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

package com.davils.kore.core.text.diagnostic

import com.davils.kore.core.Limits
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import com.davils.kore.core.text.fixture.shouldBeSafeToShowLineByLine
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.property.checkAll

private const val SUBJECT: String = "settings.yaml"
private const val TEXT: String = "name: core\nport: x\r\nmode: y\rend"
private const val UNKNOWN: String = "unknown key"
private const val EARLY: Int = 17
private const val LATE: Int = 25
private const val WARNING_HEADLINE: Int = 3

private val unknownKey: ViolationCode = ViolationCode("acme.yaml.unknown-key")
private val notNumber: ViolationCode = ViolationCode("acme.yaml.not-a-number")
private val lines: LineMap = LineMap.of(TEXT)

private fun error(start: Int, message: String): Diagnostic =
    Diagnostic(code = unknownKey, severity = Severity.Error, range = TextRange.at(start), message = message)

private fun warning(start: Int, message: String): Diagnostic =
    Diagnostic(code = notNumber, severity = Severity.Warning, range = TextRange.at(start), message = message)

class DiagnosticTest : FunSpec({
    context("a diagnostic") {
        test("is equal to another with the same parts, with the same hash") {
            error(start = 1, message = UNKNOWN) shouldBe error(start = 1, message = UNKNOWN)
            error(start = 1, message = UNKNOWN).hashCode() shouldBe error(start = 1, message = UNKNOWN).hashCode()
        }

        test("differs from one with another part") {
            error(start = 1, message = UNKNOWN) shouldNotBe error(start = 2, message = UNKNOWN)
            error(start = 1, message = UNKNOWN) shouldNotBe warning(start = 1, message = UNKNOWN)
            error(start = 1, message = UNKNOWN) shouldNotBe UNKNOWN
        }

        test("shows its severity, range, code and message on one line") {
            error(start = 2, message = UNKNOWN).toString() shouldBe "Error at 2..2 (acme.yaml.unknown-key): unknown key"
        }

        test("escapes a hostile message") {
            checkAll(hostileText) { text ->
                val shown = error(start = 0, message = text).toString()

                shown.shouldBeSafeToShow()
            }
        }
    }

    context("rendering") {
        test("names a text without diagnostics valid") {
            renderDiagnostics(SUBJECT, emptyList(), lines) shouldBe "Valid settings.yaml."
        }

        test("lists errors before warnings, each by position, with line and column") {
            val diagnostics = listOf(
                warning(start = 0, message = "warned"),
                error(start = LATE, message = "late"),
                error(start = EARLY, message = "early"),
            )

            val expected = listOf(
                "Invalid settings.yaml (2 problems):",
                "  2:7 early [acme.yaml.unknown-key]",
                "  3:6 late [acme.yaml.unknown-key]",
                "",
                "1 warning:",
                "  1:1 warned [acme.yaml.not-a-number]",
            )

            renderDiagnostics(SUBJECT, diagnostics, lines) shouldBe expected.joinToString(separator = "\n")
        }

        test("names a single error and several warnings") {
            val diagnostics = listOf(
                error(start = 0, message = "a"),
                warning(start = 1, message = "b"),
                warning(start = 2, message = "c"),
            )

            val report = renderDiagnostics(SUBJECT, diagnostics, lines).lines()

            report.first() shouldBe "Invalid settings.yaml (1 problem):"
            report[WARNING_HEADLINE] shouldBe "2 warnings:"
        }

        test("shows a limited number of diagnostics per group and counts the rest") {
            val many = List(Limits.MAX_RENDERED_VIOLATIONS + 2) { index -> error(start = 0, message = "error $index") }

            renderDiagnostics(SUBJECT, many, lines) shouldEndWith "  … and 2 more"
        }

        test("escapes a hostile subject and hostile messages") {
            checkAll(hostileText) { text ->
                val report = renderDiagnostics(text, listOf(error(start = 0, message = text)), lines)

                report.shouldBeSafeToShowLineByLine()
            }
        }

        test("refuses a diagnostic beyond the end of the text") {
            shouldThrow<IllegalArgumentException> {
                renderDiagnostics(SUBJECT, listOf(error(start = TEXT.length + 1, message = UNKNOWN)), lines)
            }
        }
    }
})
