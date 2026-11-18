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

package com.davils.kore.core.concurrency.fixture

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val PATIENCE_SECONDS: Long = 5

internal class SingleThread : AutoCloseable {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    val dispatcher: ExecutorCoroutineDispatcher = executor.asCoroutineDispatcher()

    fun occupyUntil(release: CountDownLatch) {
        executor.execute { release.await(PATIENCE_SECONDS, TimeUnit.SECONDS) }
    }

    fun awaitIdle() {
        val marker = executor.submit(Runnable {})
        marker.get(PATIENCE_SECONDS, TimeUnit.SECONDS)
    }

    override fun close() {
        dispatcher.close()
    }
}

internal suspend fun <T> withSingleThread(block: suspend (SingleThread) -> T): T =
    SingleThread().use { thread -> block(thread) }
