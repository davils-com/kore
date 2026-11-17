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

package com.davils.kore.core.dsl

import com.davils.kore.core.dsl.fixture.ConsoleSinkData
import com.davils.kore.core.dsl.fixture.FileSinkBuilder
import com.davils.kore.core.dsl.fixture.FileSinkData
import com.davils.kore.core.dsl.fixture.LoggerData
import com.davils.kore.core.dsl.fixture.LoggingBuilder
import com.davils.kore.core.dsl.fixture.LoggingData
import com.davils.kore.core.dsl.fixture.ProbeBuilder
import com.davils.kore.core.dsl.fixture.QueueBuilder
import com.davils.kore.core.dsl.fixture.QueueData
import com.davils.kore.core.dsl.fixture.logging
import com.davils.kore.core.validation.fixture.lines
import com.davils.kore.core.validation.fixture.paths
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val LARGE_QUEUE: Int = 4_096
private const val THREE_BLANK_SINKS_AND_NO_VALID_ONE: Int = 4
private const val KTOR: String = "io.ktor"
private const val CONSOLE: String = "console"
private const val FIRST_SINK_NAME: String = "sinks[0].name"

class ValidatingBuilderTest : FunSpec({
    context("building") {
        test("builds a valid configuration from nested blocks") {
            val data = logging {
                console { isColoured = false }
                file { queue { size = LARGE_QUEUE } }
                logger(KTOR) { level = "WARN" }
            }

            data.sinks shouldContainExactly listOf(
                ConsoleSinkData(name = CONSOLE, isColoured = false),
                FileSinkData("file", FileSinkBuilder.DEFAULT_MAX_BYTES, QueueData(LARGE_QUEUE)),
            )
            data.loggers shouldBe mapOf(KTOR to LoggerData("WARN"))
        }

        test("builds despite warnings") {
            val data = logging {
                level = LoggingData.TRACE
                console()
            }

            data.level shouldBe LoggingData.TRACE
        }
    }

    context("reporting") {
        test("collects problems of every level and of the parent into one report") {
            val failure = shouldThrow<ValidationException> {
                logging {
                    console { name = "" }
                    file { queue { size = -1 } }
                    file()
                    file()
                }
            }

            failure.report.paths() shouldContainExactly listOf(FIRST_SINK_NAME, "sinks[1].queue.size", "sinks")
        }

        test("lets a nested block fail without stopping the blocks after it") {
            val failure = shouldThrow<ValidationException> {
                logging {
                    console { name = "" }
                    console { name = "" }
                    console { name = "" }
                }
            }

            failure.report.errors shouldHaveSize THREE_BLANK_SINKS_AND_NO_VALID_ONE
        }

        test("counts declared blocks, so a valid block keeps its position after an invalid one") {
            val failure = shouldThrow<ValidationException> {
                logging {
                    console { name = "" }
                    file { name = "kept" }
                    file { maxBytes = -1 }
                }
            }

            failure.report.paths() shouldContainExactly listOf(FIRST_SINK_NAME, "sinks[2].maxBytes")
        }

        test("leaves a failed block out, so the parent sees only valid values") {
            val failure = shouldThrow<ValidationException> {
                logging {
                    console()
                    file { maxBytes = 0 }
                }
            }

            failure.report.violations shouldHaveSize 1
        }

        test("reports a warning next to the errors that stopped the build") {
            val failure = shouldThrow<ValidationException> { logging { level = LoggingData.TRACE } }

            failure.report.errors shouldHaveSize 1
            failure.report.warnings
                .single()
                .severity shouldBe Severity.Warning
            failure.message shouldContain "1 warning:"
        }
    }

    context("keyed blocks") {
        test("reports a keyed block under its key") {
            val failure = shouldThrow<ValidationException> {
                logging {
                    console()
                    logger(KTOR) { level = "" }
                }
            }

            failure.report.paths() shouldContainExactly listOf("loggers[\"io.ktor\"].level")
        }

        test("rejects a key declared twice instead of overwriting it") {
            val failure = shouldThrow<ValidationException> {
                logging {
                    console()
                    logger(KTOR)
                    logger(KTOR) { level = "DEBUG" }
                }
            }

            val violation = failure.report.violations.single()
            violation.path.toString() shouldBe "loggers[\"io.ktor\"]"
            violation.code shouldBe ViolationCode.DUPLICATE
        }
    }

    context("nested blocks") {
        test("rejects a nested block declared twice instead of letting the second one win") {
            val failure = shouldThrow<ValidationException> {
                logging {
                    console()
                    file {
                        queue { size = 1 }
                        queue { size = 2 }
                    }
                }
            }

            val violation = failure.report.violations.single()
            violation.path.toString() shouldBe "sinks[1].queue"
            violation.code shouldBe ViolationCode.DUPLICATE
            violation.message shouldBe "is declared more than once"
        }

        test("keeps the first of two nested blocks and never runs the second") {
            val builder = ProbeBuilder()
            var isSecondRun = false

            val first = builder.queue { size = 1 }
            val second = builder.queue { isSecondRun = true }

            first shouldBe QueueData(1)
            second.shouldBeNull()
            isSecondRun shouldBe false
            builder.validate().paths() shouldContainExactly listOf("queue")
        }
    }

    context("numbered blocks") {
        test("number the blocks of each list on their own") {
            val builder = ProbeBuilder()
            builder.item(LoggingData::sinks) { size = 0 }
            builder.item(FileSinkData::queue) { size = 0 }
            builder.item(LoggingData::sinks) { size = 0 }

            builder.validate().paths() shouldContainExactly listOf("sinks[0].size", "queue[0].size", "sinks[1].size")
        }

        test("share one numbering between properties that share a path, so no two blocks share a path") {
            val builder = ProbeBuilder()
            builder.item(LoggingData::level) { size = 0 }
            builder.item(LoggerData::level) { size = 0 }

            builder.validate().paths() shouldContainExactly listOf("level[0].size", "level[1].size")
        }
    }

    context("validate") {
        test("reports the same problems without throwing") {
            val builder = LoggingBuilder()
            builder.console { name = "" }

            val report = shouldNotThrowAny { builder.validate() }

            report.isValid shouldBe false
            report.paths() shouldContainExactly listOf(FIRST_SINK_NAME, "sinks")
        }

        test("keeps a configuration with only warnings valid") {
            val builder = LoggingBuilder()
            builder.level = LoggingData.TRACE
            builder.console()

            val report = builder.validate()

            report.isValid shouldBe true
            report.warnings shouldHaveSize 1
        }
    }

    context("subject") {
        test("is called configuration unless the builder names it") {
            LoggingBuilder().validate().subject shouldBe "configuration"
            QueueBuilder().validate().subject shouldBe "queue"
        }
    }

    context("rendering") {
        test("lists problems of three levels in one report with the path of each") {
            val builder = LoggingBuilder()
            builder.level = LoggingData.TRACE
            builder.console { name = "" }
            builder.file { queue { size = -1 } }

            builder.validate().render() shouldBe lines(
                "Invalid configuration (3 problems):",
                "",
                "  sinks[0].name        is required but was not set",
                "  sinks[1].queue.size  '-1' must be greater than zero",
                "  sinks                at least one sink must be configured",
                "",
                "1 warning:",
                "",
                "  level  trace on the root logger is expensive in production",
            )
        }
    }
})
