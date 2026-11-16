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

import com.davils.kore.core.validation.fixture.Probe
import com.davils.kore.core.validation.fixture.Shape
import com.davils.kore.core.validation.fixture.paths
import com.davils.kore.core.validation.report.KoreValidationReport
import com.davils.kore.core.validation.violation.KoreSeverity
import com.davils.kore.core.validation.violation.KoreViolationCode
import com.davils.kore.core.validation.violation.KoreViolationPath
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private const val NOT_EMPTY: String = "must not be empty"
private const val UNKNOWN_LEVEL: String = "is not a known level"
private const val SINKS: String = "sinks"
private const val EXPENSIVE: String = "trace is expensive in production"
private val ownCode: KoreViolationCode = KoreViolationCode("test.custom")

private fun report(rules: KoreValidationScope.() -> Unit): KoreValidationReport = Probe(rules).validate("probe")

class KoreValidationScopeTest : FunSpec({
    context("failing") {
        test("without a property reports at the root") {
            val violation = report { fail(NOT_EMPTY) }.violations.single()

            violation.path shouldBe KoreViolationPath.ROOT
            violation.message shouldBe NOT_EMPTY
            violation.severity shouldBe KoreSeverity.Error
        }

        test("with a property reports at that property") {
            report { fail(Shape::sinks, NOT_EMPTY) }.paths() shouldContainExactly listOf(SINKS)
        }

        test("keeps every report in the order it was made") {
            val result = report {
                fail(Shape::sinks, NOT_EMPTY)
                fail(Shape::level, UNKNOWN_LEVEL)
                fail("the configuration as a whole makes no sense")
            }

            result.paths() shouldContainExactly listOf(SINKS, "level", "")
        }
    }

    context("codes") {
        test("uses the default code unless one is given") {
            report { fail(NOT_EMPTY) }.violations.single().code shouldBe KoreViolationCode.INVALID
            report { fail(NOT_EMPTY, ownCode) }.violations.single().code shouldBe ownCode
            report { fail(Shape::sinks, NOT_EMPTY, ownCode) }.violations.single().code shouldBe ownCode
        }
    }

    context("warning") {
        test("does not invalidate the report") {
            val result = report { warn(Shape::level, EXPENSIVE) }

            result.isValid shouldBe true
            result.warnings shouldHaveSize 1
            result.errors shouldHaveSize 0
        }

        test("without a property reports at the root") {
            report { warn(EXPENSIVE, ownCode) }.violations.single().path shouldBe KoreViolationPath.ROOT
        }

        test("next to one error still leaves the report invalid") {
            val result = report {
                warn(Shape::level, EXPENSIVE)
                fail(Shape::sinks, NOT_EMPTY)
            }

            result.isValid shouldBe false
            result.errors shouldHaveSize 1
            result.warnings shouldHaveSize 1
        }
    }

    context("descending") {
        test("prefixes everything reported inside") {
            val result = report {
                within(Shape::sinks) {
                    fail(Shape::level, UNKNOWN_LEVEL)
                    fail("must be configured")
                }
            }

            result.paths() shouldContainExactly listOf("sinks.level", SINKS)
        }

        test("nests to any depth") {
            val result = report {
                within(Shape::sinks) {
                    within(Shape::level) { fail(UNKNOWN_LEVEL) }
                }
            }

            result.paths() shouldContainExactly listOf("sinks.level")
        }
    }

    context("each") {
        test("reports an element of a list under its index") {
            val result = report {
                each(Shape::sinks, listOf("a", "b", "c")) { item ->
                    if (item != "b") fail(Shape::level, "unexpected $item")
                }
            }

            result.paths() shouldContainExactly listOf("sinks[0].level", "sinks[2].level")
        }

        test("reports an entry of a map under its key") {
            val result = report {
                each(Shape::sinks, mapOf("io.ktor" to "", "kore" to "INFO")) { level ->
                    if (level.isBlank()) fail(Shape::level, UNKNOWN_LEVEL)
                }
            }

            result.paths() shouldContainExactly listOf("sinks[\"io.ktor\"].level")
        }
    }

    context("including") {
        test("reports violations from elsewhere under the path they are included at") {
            val inner = report { fail(Shape::level, UNKNOWN_LEVEL) }
            val outer = report { include(inner.violations, KoreViolationPath.named(SINKS).index(4)) }

            outer.paths() shouldContainExactly listOf("sinks[4].level")
        }

        test("keeps the code and the severity") {
            val inner = report { warn(Shape::level, "is unusual", ownCode) }
            val violation = report { include(inner.violations) }.violations.single()

            violation.code shouldBe ownCode
            violation.severity shouldBe KoreSeverity.Warning
        }
    }
})
