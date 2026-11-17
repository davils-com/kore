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
import com.davils.kore.core.arguments.resolution.fixture.Racy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val ATTEMPTS: Int = 200
private const val TIMEOUT_SECONDS: Long = 30
private const val RETRIED_PORT: Int = 9

class ParametersConcurrentLoadTest : FunSpec({
    context("two threads loading one schema at once") {
        test("let exactly one win and refuse the other") {
            val pool = Executors.newFixedThreadPool(2)
            try {
                repeat(ATTEMPTS) {
                    val parameters = Racy()
                    val barrier = CyclicBarrier(2)
                    val futures = listOf("1", "2").map { port ->
                        pool.submit<Result<Unit>> {
                            barrier.await()
                            runCatching { parameters.load(listOf("--port=$port"), NO_ENVIRONMENT) }
                        }
                    }
                    val outcomes = futures.map { future -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }

                    val lost = outcomes.single { outcome -> outcome.isFailure }
                    val won = outcomes.indexOfFirst { outcome -> outcome.isSuccess }

                    lost.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
                    parameters.port shouldBe won + 1
                }
            } finally {
                pool.shutdownNow()
                pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }
        }
    }

    context("a load that failed") {
        test("can be retried") {
            val parameters = Racy()
            runCatching { parameters.load(emptyList(), NO_ENVIRONMENT) }

            parameters.load(listOf("--port=9"), NO_ENVIRONMENT)

            parameters.port shouldBe RETRIED_PORT
        }
    }
})
