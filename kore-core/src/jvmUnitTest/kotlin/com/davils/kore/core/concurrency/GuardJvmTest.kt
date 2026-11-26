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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.concurrent.thread

private const val THREADS: Int = 8
private const val INCREMENTS: Int = 10_000

class GuardJvmTest : FunSpec({
    context("a guard on the JVM") {
        test("lets one thread at a time into the section") {
            val shared = guard()
            var count = 0
            val workers = List(THREADS) {
                thread {
                    repeat(INCREMENTS) { shared.locked { count += 1 } }
                }
            }
            workers.forEach { worker -> worker.join() }

            count shouldBe THREADS * INCREMENTS
        }
    }
})
