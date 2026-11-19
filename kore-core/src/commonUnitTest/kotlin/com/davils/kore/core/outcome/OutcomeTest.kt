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

package com.davils.kore.core.outcome

import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.checkAll

private const val ANSWER: Int = 42
private const val OTHER: Int = 7
private const val PROBLEM: String = "too long"

private val success: Outcome<Int, String> = Outcome.Success(ANSWER)
private val failure: Outcome<Int, String> = Outcome.Failure(PROBLEM)

class OutcomeTest : FunSpec({
    context("kinds") {
        test("tell a success from a failure") {
            success.isSuccess shouldBe true
            success.isFailure shouldBe false
            failure.isSuccess shouldBe false
            failure.isFailure shouldBe true
        }
    }

    context("equality") {
        test("treats outcomes of the same kind with equal contents as equal with the same hash") {
            Outcome.Success(ANSWER) shouldBe Outcome.Success(ANSWER)
            Outcome.Success(ANSWER).hashCode() shouldBe Outcome.Success(ANSWER).hashCode()
            Outcome.Failure(PROBLEM) shouldBe Outcome.Failure(PROBLEM)
            Outcome.Failure(PROBLEM).hashCode() shouldBe Outcome.Failure(PROBLEM).hashCode()
        }

        test("tells outcomes apart by kind and by contents") {
            Outcome.Success(ANSWER) shouldNotBe Outcome.Failure(ANSWER)
            Outcome.Failure(ANSWER) shouldNotBe Outcome.Success(ANSWER)
            Outcome.Success(ANSWER) shouldNotBe Outcome.Success(OTHER)
            Outcome.Failure(PROBLEM) shouldNotBe Outcome.Failure(OTHER)
        }

        test("allows a success without a value") {
            Outcome.Success(null) shouldBe Outcome.Success(null)
            Outcome.Success(null).hashCode() shouldBe Outcome.Success(null).hashCode()
        }
    }

    context("rendering") {
        test("names the kind and the contents") {
            success.toString() shouldBe "Success(42)"
            failure.toString() shouldBe "Failure(too long)"
        }

        test("escapes hostile contents") {
            checkAll(hostileText) { text ->
                val succeeded = Outcome.Success(text)
                val failed = Outcome.Failure(text)

                succeeded.toString().shouldBeSafeToShow()
                failed.toString().shouldBeSafeToShow()
            }
        }
    }

    context("transformations") {
        test("map the value of a success only") {
            success.map { value -> value + 1 } shouldBe Outcome.Success(ANSWER + 1)
            failure.map { value -> value + 1 } shouldBe failure
        }

        test("map the failure of a failure only") {
            success.mapFailure { problem -> problem.length } shouldBe success
            failure.mapFailure { problem -> problem.length } shouldBe Outcome.Failure(PROBLEM.length)
        }

        test("continue a success with the next operation only") {
            success.flatMap { value -> Outcome.Failure("refused $value") } shouldBe Outcome.Failure("refused 42")
            failure.flatMap { value -> Outcome.Success(value + 1) } shouldBe failure
        }

        test("recover a failure with a value") {
            success.recover { problem -> problem.length } shouldBe success
            failure.recover { problem -> problem.length } shouldBe Outcome.Success(PROBLEM.length)
        }

        test("turn a result into an outcome") {
            val cause = IllegalStateException("broken")

            Result.success(ANSWER).toOutcome() shouldBe Outcome.Success(ANSWER)
            Result.failure<Int>(cause).toOutcome() shouldBe Outcome.Failure(cause)
        }
    }

    context("accessors") {
        test("fold either kind into one result") {
            success.fold(onSuccess = { value -> "value $value" }, onFailure = { problem -> problem }) shouldBe
                "value 42"
            failure.fold(onSuccess = { value -> "value $value" }, onFailure = { problem -> problem }) shouldBe PROBLEM
        }

        test("run an action for the matching kind only") {
            val seen = mutableListOf<String>()

            listOf(success, failure).forEach { outcome ->
                val passed = outcome.onSuccess { value -> seen += "success $value" }
                passed.onFailure { problem -> seen += problem }
            }

            seen shouldBe listOf("success 42", PROBLEM)
        }

        test("give the value or a fallback") {
            success.getOrElse { OTHER } shouldBe ANSWER
            failure.getOrElse { OTHER } shouldBe OTHER
        }

        test("give the value or null") {
            success.getOrNull() shouldBe ANSWER
            failure.getOrNull().shouldBeNull()
        }

        test("give the failure or null") {
            success.failureOrNull().shouldBeNull()
            failure.failureOrNull() shouldBe PROBLEM
        }

        test("throw the throwable of a failure as it is") {
            val cause = IllegalStateException("broken")
            val thrown: Outcome<Int, Throwable> = Outcome.Failure(cause)

            shouldThrow<IllegalStateException> { thrown.getOrThrow() } shouldBeSameInstanceAs cause
            Outcome.Success(ANSWER).getOrThrow() shouldBe ANSWER
        }
    }
})
