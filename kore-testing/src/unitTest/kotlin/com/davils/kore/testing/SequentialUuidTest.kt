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

package com.davils.kore.testing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val SEQUENCE: Long = 5L
private const val BEFORE_THE_EPOCH: Long = -1L

class SequentialUuidTest : FunSpec({
    context("building an identifier from a timestamp and a sequence") {
        test("places both in a version 7 identifier") {
            val identifier = sequentialUuidV7(epochMilliseconds = MAX_TIMESTAMP, sequence = SEQUENCE)

            identifier.epochMilliseconds shouldBe MAX_TIMESTAMP
            identifier.leastSignificantBits and SEQUENCE shouldBe SEQUENCE
        }

        test("refuses a timestamp before the epoch") {
            val failure = shouldThrow<IllegalArgumentException> {
                sequentialUuidV7(epochMilliseconds = BEFORE_THE_EPOCH, sequence = 0L)
            }

            failure.message shouldContain "48 bits"
        }

        test("refuses a timestamp beyond the 48 bits a version 7 identifier holds") {
            shouldThrow<IllegalArgumentException> {
                sequentialUuidV7(epochMilliseconds = MAX_TIMESTAMP + 1, sequence = 0L)
            }
        }
    }
})
