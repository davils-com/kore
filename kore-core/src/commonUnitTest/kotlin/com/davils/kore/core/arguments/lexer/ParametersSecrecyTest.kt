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

package com.davils.kore.core.arguments.lexer

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.lexer.fixture.Collecting
import com.davils.kore.core.arguments.lexer.fixture.Credentials
import com.davils.kore.core.arguments.lexer.fixture.Login
import com.davils.kore.core.arguments.report.ParameterException
import com.davils.kore.core.arguments.report.ParameterViolation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PLACEHOLDER: String = "not-a-real-key-value"
private const val STRAY_POSITION: Int = 3
private const val PASSWORD: String = "--password"

class ParametersSecrecyTest : FunSpec({
    context("a mistyped sensitive parameter") {
        test("does not put its value in the report") {
            val failure = shouldThrow<ParameterException> {
                Credentials().load(listOf("--api-kye", PLACEHOLDER), NO_ENVIRONMENT)
            }

            val message = failure.message.shouldNotBeNull()
            message shouldNotContain PLACEHOLDER
            message shouldContain "--api-kye"
            message shouldContain "did you mean --api-key?"
            message shouldContain "argument 2"
        }
    }

    context("a sensitive parameter followed by an argument that is not a value") {
        test("skips that argument instead of reporting it") {
            val followers = listOf("--$PLACEHOLDER", "-X$PLACEHOLDER", "--verbos3", "-vvvv", "--user=$PLACEHOLDER")

            followers.forEach { follower ->
                val failure = shouldThrow<ParameterException> {
                    Login().load(listOf(PASSWORD, follower), NO_ENVIRONMENT)
                }

                withClue(follower) {
                    val violation = failure.violations.single()
                    violation.shouldBeInstanceOf<ParameterViolation.MissingValue>()
                    violation.token shouldBe PASSWORD
                    failure.message.shouldNotBeNull() shouldNotContain follower.drop(1)
                }
            }
        }

        test("given by its short name skips that argument too") {
            val failure = shouldThrow<ParameterException> {
                Login().load(listOf("-p", "-vvvv", "--user=kore"), NO_ENVIRONMENT)
            }

            failure.violations.single().shouldBeInstanceOf<ParameterViolation.MissingValue>()
        }

        test("reports nothing more when it is the last argument") {
            val failure = shouldThrow<ParameterException> { Login().load(listOf(PASSWORD), NO_ENVIRONMENT) }

            failure.violations.single().shouldBeInstanceOf<ParameterViolation.MissingValue>()
        }
    }

    context("a parameter that is not sensitive followed by an argument that is not a value") {
        test("leaves that argument to be read on its own") {
            val failure = shouldThrow<ParameterException> {
                Login().load(listOf("--user", "--unknown"), NO_ENVIRONMENT)
            }

            failure.violations.map { violation -> violation::class.simpleName } shouldBe
                listOf("MissingValue", "UnknownArgument")
        }
    }

    context("a stray value") {
        test("is reported by where it was") {
            val failure = shouldThrow<ParameterException> {
                Credentials().load(listOf("--api-key", PLACEHOLDER, "leftover"), NO_ENVIRONMENT)
            }

            failure.violations
                .filterIsInstance<ParameterViolation.StrayValue>()
                .single()
                .position shouldBe STRAY_POSITION
            failure.message.shouldNotBeNull() shouldNotContain "leftover"
        }

        test("is handed over as it was when unknown arguments are collected") {
            val parameters = Collecting()

            parameters.load(listOf("--name=kore", PLACEHOLDER), NO_ENVIRONMENT)

            parameters.unknown shouldBe listOf(PLACEHOLDER)
        }
    }
})
