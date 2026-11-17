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

import com.davils.kore.core.limit.Limits
import com.davils.kore.core.text.fixture.CLIPBOARD_WRITE
import com.davils.kore.core.text.fixture.FORGED_LOG_LINE
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import com.davils.kore.core.text.fixture.shouldBeSafeToShowLineByLine
import com.davils.kore.core.validation.fixture.Probe
import com.davils.kore.core.validation.fixture.Shape
import com.davils.kore.core.validation.fixture.nestedChain
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.checkAll
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

private const val CASES: Int = 200
private const val SUBJECT: String = "shape"
private const val WRONG: String = "is wrong"
private const val HEADLINE_LINES: Int = 2
private const val UNCOLLECTED: Int = 5
private const val UNRENDERED: Int = 3
private const val DEPTH_OVERSHOOT: Int = 4

private fun roundTrip(failure: Throwable): Any? {
    val bytes = ByteArrayOutputStream()
    ObjectOutputStream(bytes).use { stream -> stream.writeObject(failure) }
    val input = ByteArrayInputStream(bytes.toByteArray())
    return java.io.ObjectInputStream(input).use { stream -> stream.readObject() }
}

class ValidationHardeningTest : FunSpec({
    context("untrusted keys") {
        test("are escaped in the path, so a key cannot pose as another part of the path") {
            val probe = Probe { each(Shape::sinks, mapOf("a\"].admin[\"b" to 1)) { fail(WRONG) } }

            val report = probe.validate(SUBJECT)
            val path = report.violations.single().path

            path.toString() shouldBe """sinks["a\"].admin[\"b"]"""
        }

        test("never bring an unsafe character into a rendered report") {
            checkAll(CASES, hostileText) { key ->
                val probe = Probe { each(Shape::sinks, mapOf(key to 1)) { fail(WRONG) } }

                val report = probe.validate(SUBJECT)

                report.render().shouldBeSafeToShowLineByLine()
            }
        }
    }

    context("untrusted messages") {
        test("cannot forge a line of a log or control a terminal through a report") {
            val probe = Probe { fail("was '$CLIPBOARD_WRITE$FORGED_LOG_LINE'") }

            val rendered = probe.validate(SUBJECT).render()

            rendered.shouldBeSafeToShowLineByLine()
            rendered.lines() shouldHaveSize HEADLINE_LINES + 1
        }

        test("are escaped in the text of a single violation as well") {
            checkAll(CASES, hostileText) { message ->
                val report = Probe { fail(message) }.validate(SUBJECT)
                val violation = report.violations.single()

                violation.toString().shouldBeSafeToShow()
            }
        }

        test("keep their original text for code that reads the violation") {
            val report = Probe { fail(FORGED_LOG_LINE) }.validate(SUBJECT)

            report.violations.single().message shouldBe FORGED_LOG_LINE
        }

        test("are shortened in a report once they exceed the message limit") {
            val probe = Probe { fail("x".repeat(Limits.MAX_MESSAGE_LENGTH + 10)) }

            probe.validate(SUBJECT).render() shouldContain "…(+10 chars)"
        }

        test("escape the subject of the report too") {
            val report = Probe { }.validate("shape$FORGED_LOG_LINE")

            report.render().shouldBeSafeToShowLineByLine()
        }
    }

    context("a validation that finds too many problems") {
        test("collects at most the limit and reports how many more it found") {
            val probe = Probe { repeat(Limits.MAX_VIOLATIONS + UNCOLLECTED) { fail(WRONG) } }

            val report = probe.validate(SUBJECT)

            report.violations shouldHaveSize Limits.MAX_VIOLATIONS + 1
            report.violations.last().code shouldBe ViolationCode.TOO_MANY
            report.violations.last().message shouldBe "$UNCOLLECTED further problems were found but not collected"
            report.isValid shouldBe false
        }

        test("counts more warnings than the limit as an error, so validateOrThrow refuses them") {
            val probe = Probe { repeat(Limits.MAX_VIOLATIONS + UNCOLLECTED) { warn(WRONG) } }

            val failure = shouldThrow<ValidationException> { probe.validateOrThrow(SUBJECT) }

            failure.report.errors
                .single()
                .code shouldBe ViolationCode.TOO_MANY
            failure.report.warnings shouldHaveSize Limits.MAX_VIOLATIONS
        }

        test("keeps a value with as many warnings as the limit valid") {
            val probe = Probe { repeat(Limits.MAX_VIOLATIONS) { warn(WRONG) } }

            probe.validateOrThrow(SUBJECT) shouldBeSameInstanceAs probe
        }

        test("renders only the first violations and names the rest") {
            val count = Limits.MAX_RENDERED_VIOLATIONS + UNRENDERED
            val probe = Probe { repeat(count) { fail(WRONG) } }

            val rendered = probe.validate(SUBJECT).render()

            rendered shouldContain "… and $UNRENDERED more"
            rendered.lines() shouldHaveSize HEADLINE_LINES + Limits.MAX_RENDERED_VIOLATIONS + 1
        }
    }

    context("a value that nests deeper than the limit") {
        test("is reported once at the deepest level instead of exhausting the stack") {
            val chain = nestedChain(Limits.MAX_PATH_DEPTH * DEPTH_OVERSHOOT)

            val report = chain.validate(SUBJECT)

            report.violations shouldHaveSize 1
            val violation = report.violations.single()
            violation.code shouldBe ViolationCode.TOO_DEEP
            violation.path.depth shouldBe Limits.MAX_PATH_DEPTH
        }
    }

    context("the collections of a report") {
        test("cannot be changed through a cast") {
            val probe = Probe {
                fail("one")
                fail("two")
            }
            val report = probe.validate(SUBJECT)

            shouldThrow<UnsupportedOperationException> {
                (report.violations as MutableList<*>).clear()
            }
            shouldThrow<UnsupportedOperationException> {
                (report.errors as MutableList<*>).clear()
            }
            val first = report.violations.first()
            shouldThrow<UnsupportedOperationException> {
                (first.path.segments as MutableList<*>).clear()
            }
        }
    }

    context("the failure of a validation") {
        test("renders its message only once it is read and keeps it safe to log") {
            val failure = shouldThrow<ValidationException> {
                Probe { fail(FORGED_LOG_LINE) }.validateOrThrow(SUBJECT)
            }

            failure.message.shouldBeSafeToShowLineByLine()
            failure.message shouldNotContain "\n2026"
        }

        test("is serialized as its rendered message only, never with the violations") {
            val failure = shouldThrow<ValidationException> {
                Probe { fail(WRONG) }.validateOrThrow(SUBJECT)
            }

            val restored = roundTrip(failure)

            restored.shouldBeInstanceOf<RuntimeException>()
            restored.message shouldBe failure.message
            restored.stackTrace.toList() shouldBe failure.stackTrace.toList()
        }
    }
})
