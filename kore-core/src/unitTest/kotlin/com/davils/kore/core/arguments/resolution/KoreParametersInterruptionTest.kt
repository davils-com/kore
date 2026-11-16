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

package com.davils.kore.core.arguments.resolution

import com.davils.kore.core.arguments.fixture.FromSources
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.resolution.fixture.Interrupting
import com.davils.kore.core.arguments.resolution.fixture.InterruptingSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.coroutines.cancellation.CancellationException

private fun interruptionWasRestored(): Boolean = Thread.interrupted()

class KoreParametersInterruptionTest : FunSpec({
    context("an interruption") {
        test("thrown by a converter escapes the load and keeps the thread interrupted") {
            shouldThrow<InterruptedException> { Interrupting().load(listOf("--value=x"), NO_ENVIRONMENT) }

            interruptionWasRestored() shouldBe true
        }

        test("thrown by a rule escapes the load and keeps the thread interrupted") {
            shouldThrow<InterruptedException> { Interrupting().load(listOf("--checked=x"), NO_ENVIRONMENT) }

            interruptionWasRestored() shouldBe true
        }

        test("thrown by a value source escapes the load and keeps the thread interrupted") {
            shouldThrow<InterruptedException> {
                FromSources().load(emptyList(), listOf(InterruptingSource()), NO_ENVIRONMENT)
            }

            interruptionWasRestored() shouldBe true
        }

        test("leaves the schema loadable again") {
            val parameters = Interrupting()
            shouldThrow<InterruptedException> { parameters.load(listOf("--value=x"), NO_ENVIRONMENT) }
            interruptionWasRestored()

            parameters.load(emptyList(), NO_ENVIRONMENT)

            parameters.isLoaded shouldBe true
        }
    }

    context("containing foreign code") {
        test("rethrows an interruption after restoring the flag") {
            shouldThrow<InterruptedException> { catchingForeign { throw InterruptedException() } }

            interruptionWasRestored() shouldBe true
        }

        test("rethrows a cancellation and a failure of the machine untouched") {
            shouldThrow<CancellationException> { catchingForeign { throw CancellationException("stop") } }
            shouldThrow<StackOverflowError> { catchingForeign { throw StackOverflowError() } }
            shouldThrow<OutOfMemoryError> { catchingForeign { throw OutOfMemoryError() } }
            interruptionWasRestored() shouldBe false
        }

        test("contains an error that is not a failure of the machine as a failed result") {
            val errors = listOf(AssertionError("assumed"), NotImplementedError("later"), NoClassDefFoundError("gone"))

            errors.forEach { error ->
                catchingForeign { throw error }.exceptionOrNull() shouldBe error
            }
        }

        test("contains an ordinary exception as a failed result") {
            val result = catchingForeign { throw IllegalStateException("broken") }

            result.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
        }
    }
})
