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

package com.davils.kore.core.version

import com.davils.kore.core.Limits
import com.davils.kore.core.validation.validate
import com.davils.kore.core.version.fixture.Manifest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val SUBJECT: String = "manifest"
private const val VERSION: String = "1.4.0"
private const val NAME: String = "plugin"
private const val TOO_MANY_COMPARATORS: Int = Limits.MAX_RANGE_COMPARATORS + 1

class VersionValidationsTest : FunSpec({
    context("reading a version and a range inside a validation") {
        test("adds nothing to the report for a well-formed version and range") {
            val report = Manifest(version = VERSION, requiresShell = "[1.2, 2.0)", name = NAME).validate(SUBJECT)

            report.violations.isEmpty() shouldBe true
        }

        test("reports a malformed version and range beside every other problem") {
            val report = Manifest(version = "v1.4", requiresShell = ">=", name = "").validate(SUBJECT)

            report.errors.map { it.path.toString() } shouldContainExactly listOf("version", "requiresShell", "name")
            report.errors.map { it.code } shouldContainExactly listOf(
                VersionViolationCodes.INVALID_VERSION,
                VersionViolationCodes.INVALID_RANGE,
                report.errors.last().code,
            )
        }

        test("reads a range in npm notation as well") {
            val report = Manifest(version = VERSION, requiresShell = "^1.2 || ^2", name = NAME).validate(SUBJECT)

            report.violations.isEmpty() shouldBe true
        }

        test("says why a range is refused") {
            val report = Manifest(version = VERSION, requiresShell = "[2.0, 1.0)", name = NAME).validate(SUBJECT)

            report.errors.single().message shouldBe "is not a version range: it has an interval that is empty, " +
                "because its lower bound is not below its upper"
        }

        test("says that a range has too many comparators, without quoting it") {
            val tooMany = List(TOO_MANY_COMPARATORS) { ">=1" }.joinToString(separator = " ")

            val report = Manifest(version = VERSION, requiresShell = tooMany, name = NAME).validate(SUBJECT)

            report.errors.single().message shouldBe
                "is not a version range: it has more than ${Limits.MAX_RANGE_COMPARATORS} comparators"
        }

        test("says why a version is refused") {
            val report = Manifest(version = "01.4.0", requiresShell = "^1", name = NAME).validate(SUBJECT)

            report.errors.single().message shouldBe "is not a semantic version: it has a number with a leading zero"
        }
    }

    context("the violation codes") {
        test("are stable identifiers in the kore namespace") {
            VersionViolationCodes.INVALID_VERSION.value shouldBe "kore.version.invalid-version"
            VersionViolationCodes.INVALID_RANGE.value shouldBe "kore.version.invalid-range"
        }
    }
})
