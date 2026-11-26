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

package com.davils.kore.core.concurrency

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GuardTest : FunSpec({
    context("a guard") {
        test("returns the result of the block") {
            guard().locked { "held" } shouldBe "held"
        }

        test("may be entered again by the caller that holds it") {
            val shared = guard()

            shared.locked { shared.locked { 2 } } shouldBe 2
        }

        test("is released when the block fails") {
            val shared = guard()

            shouldThrow<IllegalStateException> { shared.locked { error("The section failed") } }
            shared.locked { true } shouldBe true
        }
    }
})
