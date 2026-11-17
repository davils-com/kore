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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.core.platform

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

private const val ACCESS_DENIED: Long = 5L
private const val TRANSFERRED: Long = 42L
private const val HRESULT_ACCESS_DENIED: Long = 0x8007_0005L

class NativeResultTest : FunSpec({
    context("an encoded result") {
        test("of more than zero is a success carrying its value") {
            val result = NativeResult.fromEncoded(TRANSFERRED)

            result.isSuccess shouldBe true
            result.value shouldBe TRANSFERRED
            result.errorCode shouldBe 0L
        }

        test("of zero is always a success, never a failure without a code") {
            val result = NativeResult.fromEncoded(0L)

            result.isSuccess shouldBe true
            result.value shouldBe 0L
        }

        test("that is negative is a failure carrying the operating system error code") {
            val result = NativeResult.fromEncoded(-ACCESS_DENIED)

            result.isSuccess shouldBe false
            result.errorCode shouldBe ACCESS_DENIED
        }

        test("keeps an error code above the largest Int without cutting it off") {
            val result = NativeResult.fromEncoded(-HRESULT_ACCESS_DENIED)

            result.errorCode shouldBe HRESULT_ACCESS_DENIED
        }

        test("of the smallest Long is refused, because it negates to no error code") {
            shouldThrow<IllegalArgumentException> { NativeResult.fromEncoded(Long.MIN_VALUE) }
        }

        test("is a success exactly when it is zero or more, and a failure always carries a code above zero") {
            checkAll(Arb.long(min = Long.MIN_VALUE + 1, max = Long.MAX_VALUE)) { encoded ->
                val result = NativeResult.fromEncoded(encoded)

                result.isSuccess shouldBe (encoded >= 0L)
                if (!result.isSuccess) result.errorCode shouldBeGreaterThan 0L
            }
        }
    }

    context("value") {
        test("cannot be read from a failure") {
            val failure = NativeResult.failure(ACCESS_DENIED)

            shouldThrow<IllegalStateException> { failure.value }.message shouldContain "carries no value"
        }
    }

    context("success") {
        test("encodes its value as it is") {
            NativeResult.success(TRANSFERRED) shouldBe NativeResult.fromEncoded(TRANSFERRED)
        }

        test("rejects a negative value") {
            shouldThrow<IllegalArgumentException> { NativeResult.success(-1L) }
        }
    }

    context("failure") {
        test("encodes its error code negated") {
            NativeResult.failure(ACCESS_DENIED) shouldBe NativeResult.fromEncoded(-ACCESS_DENIED)
        }

        test("carries a code above the largest Int, such as an HRESULT") {
            NativeResult.failure(HRESULT_ACCESS_DENIED).errorCode shouldBe HRESULT_ACCESS_DENIED
        }

        test("carries the largest error code a Long holds") {
            NativeResult.failure(Long.MAX_VALUE).errorCode shouldBe Long.MAX_VALUE
        }

        test("rejects an error code of zero, so a failure never looks like a success") {
            shouldThrow<IllegalArgumentException> { NativeResult.failure(0L) }
        }

        test("rejects a negative error code") {
            shouldThrow<IllegalArgumentException> { NativeResult.failure(-ACCESS_DENIED) }
        }
    }

    context("rendering") {
        test("names the value of a success and the code of a failure") {
            NativeResult.success(TRANSFERRED).toString() shouldBe "NativeResult(value=42)"
            NativeResult.failure(ACCESS_DENIED).toString() shouldBe "NativeResult(errorCode=5)"
        }
    }
})
