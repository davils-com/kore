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

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.Simple
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val READER_THREADS: Int = 8
private const val READS_PER_THREAD: Int = 2000
private const val SHUTDOWN_SECONDS: Long = 10
private const val LOADED_PORT: Int = 4242

class ParametersThreadsTest : FunSpec({
    context("a loaded schema") {
        test("reads correctly from many threads at once") {
            val parameters = Simple()
            parameters.load(listOf("--port=4242"), NO_ENVIRONMENT)
            val pool = Executors.newFixedThreadPool(READER_THREADS)
            val mistakes = AtomicInteger()

            repeat(READER_THREADS) {
                pool.execute {
                    repeat(READS_PER_THREAD) { if (parameters.port != LOADED_PORT) mistakes.incrementAndGet() }
                }
            }
            pool.shutdown()

            pool.awaitTermination(SHUTDOWN_SECONDS, TimeUnit.SECONDS) shouldBe true
            mistakes.get() shouldBe 0
        }
    }
})
