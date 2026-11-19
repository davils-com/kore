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

package com.davils.kore.core.failure

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.coroutines.cancellation.CancellationException

private fun interruptionWasRestored(): Boolean = Thread.interrupted()

private fun brokenCall(failure: Throwable): Int = throw failure

class FatalityTest : FunSpec({
    context("a failure of the virtual machine") {
        test("passes through contained unchanged") {
            shouldThrow<StackOverflowError> { contained { brokenCall(StackOverflowError()) } }
            shouldThrow<OutOfMemoryError> { contained { brokenCall(OutOfMemoryError()) } }
        }

        test("passes through attempted unchanged") {
            shouldThrow<OutOfMemoryError> { attempted { brokenCall(OutOfMemoryError()) } }
        }

        test("leaves the thread not interrupted") {
            shouldThrow<StackOverflowError> { StackOverflowError().rethrowIfFatal() }

            interruptionWasRestored() shouldBe false
        }
    }

    context("an interruption") {
        test("passes through contained after restoring the flag") {
            shouldThrow<InterruptedException> { contained { brokenCall(InterruptedException()) } }

            interruptionWasRestored() shouldBe true
        }

        test("passes through attempted after restoring the flag") {
            shouldThrow<InterruptedException> { attempted { brokenCall(InterruptedException()) } }

            interruptionWasRestored() shouldBe true
        }
    }

    context("an error of the code that threw it") {
        test("is contained as it was thrown") {
            val errors = listOf(AssertionError("assumed"), NotImplementedError("later"), NoClassDefFoundError("gone"))

            errors.forEach { error -> contained { brokenCall(error) }.exceptionOrNull() shouldBeSameInstanceAs error }
        }
    }

    context("a cancellation from a Java future") {
        test("passes through contained") {
            shouldThrow<CancellationException> {
                contained { brokenCall(java.util.concurrent.CancellationException("cancelled")) }
            }
        }
    }
})
