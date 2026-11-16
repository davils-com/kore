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

package com.davils.kore.core.arguments.report

import com.davils.kore.core.arguments.fixture.FixedSource
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.fixture.Decorated
import com.davils.kore.core.arguments.report.fixture.Echoing
import com.davils.kore.core.arguments.resolution.fixture.Grouped
import com.davils.kore.core.text.fixture.CLEAR_SCREEN
import com.davils.kore.core.text.fixture.FORGED_LOG_LINE
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import com.davils.kore.core.text.fixture.shouldBeSafeToShowLineByLine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.checkAll

private const val CASES: Int = 200
private const val REPORT_LINES: Int = 3

private fun methodNamesOf(type: Class<*>): List<String> = type.methods.map { method -> method.name }

class KoreParametersRenderingTest : FunSpec({
    context("a hostile value") {
        test("from the command line is described safely") {
            checkAll(CASES, hostileText) { hostile ->
                val parameters = Echoing()

                parameters.load(listOf("--name=$hostile", "--alias=$hostile", "--alias=x"), NO_ENVIRONMENT)

                parameters.describe().shouldBeSafeToShowLineByLine()
            }
        }

        test("from the environment or a source is described safely, and so is the source") {
            checkAll(CASES, hostileText) { hostile ->
                val parameters = Echoing()
                val source = FixedSource(name = hostile, value = hostile)

                parameters.load(listOf("--count=1"), listOf(source), mapOf("APP_REGION" to hostile))

                parameters.describe().shouldBeSafeToShowLineByLine()
            }
        }

        test("that cannot be converted is reported safely") {
            checkAll(CASES, hostileText) { hostile ->
                val failure = shouldThrow<KoreParameterException> {
                    Echoing().load(listOf("--count=x$hostile"), NO_ENVIRONMENT)
                }

                failure.message.shouldBeSafeToShowLineByLine()
                failure.violations.forEach { violation -> violation.toString().shouldBeSafeToShow() }
            }
        }

        test("cannot forge a line of the report or clear the screen") {
            val failure = shouldThrow<KoreParameterException> {
                Echoing().load(listOf("--count=$FORGED_LOG_LINE$CLEAR_SCREEN"), NO_ENVIRONMENT)
            }

            failure.message.lines().size shouldBe REPORT_LINES
            failure.message shouldNotContain CLEAR_SCREEN
        }
    }

    context("a hostile default, description, type or advice") {
        test("is listed safely in the usage") {
            checkAll(CASES, hostileText) { hostile ->
                val usage = Decorated(hostile).usage()

                usage.shouldBeSafeToShowLineByLine()
            }
        }

        test("is described safely") {
            checkAll(CASES, hostileText) { hostile ->
                val parameters = Decorated(hostile)

                parameters.load(emptyList(), NO_ENVIRONMENT)

                parameters.describe().shouldBeSafeToShowLineByLine()
            }
        }
    }

    context("an origin") {
        test("is printed safely whatever it names") {
            checkAll(CASES, hostileText) { hostile ->
                val environment = KoreValueOrigin.Environment(hostile)
                val external = KoreValueOrigin.External(hostile)

                environment.toString().shouldBeSafeToShow()
                external.toString().shouldBeSafeToShow()
            }
        }

        test("from the environment or a source is a plain class without copies or components") {
            val generated = setOf("copy", "component1")

            val environment = methodNamesOf(KoreValueOrigin.Environment::class.java)
            val external = methodNamesOf(KoreValueOrigin.External::class.java)

            environment.filter { name -> name in generated }.shouldBeEmpty()
            external.filter { name -> name in generated }.shouldBeEmpty()
            KoreValueOrigin.External("vault") shouldNotBe KoreValueOrigin.Environment("vault")
        }
    }

    context("the lists a failure hands out") {
        test("cannot be changed by casting them") {
            val failure = shouldThrow<KoreParameterException> {
                Grouped().load(listOf("--token=a", "--key-file=b", "--password=c"), NO_ENVIRONMENT)
            }

            val exclusive = failure.violations.filterIsInstance<KoreParameterViolation.MutuallyExclusive>().single()
            val dependency = failure.violations.filterIsInstance<KoreParameterViolation.MissingDependency>().single()
            shouldThrow<UnsupportedOperationException> { (failure.violations as MutableList<*>).clear() }
            shouldThrow<UnsupportedOperationException> { (exclusive.parameters as MutableList<*>).clear() }
            shouldThrow<UnsupportedOperationException> { (dependency.missing as MutableList<*>).clear() }
        }
    }

    context("a violation") {
        test("is printed with its path, code and message") {
            val failure = shouldThrow<KoreParameterException> {
                Echoing().load(listOf("--count=eighty"), NO_ENVIRONMENT)
            }

            val printed = failure.violations.single().toString()
            printed shouldContain "--count"
            printed shouldContain "kore.parameter.conversion-failed"
        }
    }
})
