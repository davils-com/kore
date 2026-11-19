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

package com.davils.kore.core.concurrency

import com.davils.kore.core.annotation.DavilsExperimental
import com.davils.kore.core.failure.attempted
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Shares one execution of a block among callers that ask for the same key at the same time, such as concurrent
 * requests that would all load the same entry into a cache.
 *
 * The first caller of [run] for a key leads: it runs the block in its own coroutine, with its own context. Every
 * caller that asks for the same key while the block runs waits for it and receives the same result, or the same
 * exception, as the leader. Nothing is kept once the block has finished: the next call for the key runs the block
 * again. A cancelled caller only stops waiting. When the leader is cancelled, the block is cancelled with it, the
 * waiting callers are not, and one of them leads a new execution. Calls for different keys never wait for each other.
 *
 * The key must name everything the result depends on. A block runs with the context of its leader, which includes
 * the principal of `withPrincipal`, so a key without the identity of the caller hands one caller's result to another.
 * An exception the block throws reaches every waiting caller and may carry the input of the leader.
 *
 * Keys are compared by `equals` and `hashCode`. A single flight is safe to share between coroutines and keeps no
 * scope, thread or timer of its own. It is experimental: its shape may still change.
 *
 * @param K the type of the keys.
 * @param V the type of the results.
 * @since 2.1.0
 */
@DavilsExperimental
public class SingleFlight<K : Any, V> {
    private val lock: Mutex = Mutex()
    private val flights: HashMap<K, CompletableDeferred<Result<V>?>> = HashMap()

    /**
     * Runs [block] for [key], or waits for the execution another caller already runs for it, and returns its result.
     *
     * @param key what the result depends on.
     * @param block the work, run by the caller that leads.
     * @return the result of the execution this call led or waited for.
     * @throws Throwable the exception the execution threw, the same instance for every caller that shared it.
     * @throws kotlin.coroutines.cancellation.CancellationException if this caller is cancelled.
     * @since 2.1.0
     */
    public suspend fun run(key: K, block: suspend () -> V): V {
        var settled: Result<V>? = null
        while (settled == null) settled = attempt(key, block)
        return settled.getOrThrow()
    }

    private suspend fun attempt(key: K, block: suspend () -> V): Result<V>? {
        val fresh = CompletableDeferred<Result<V>?>()
        val flight = lock.withLock { flights.getOrPut(key) { fresh } }
        if (flight !== fresh) return flight.await()
        return lead(key = key, flight = flight, block = block)
    }

    private suspend fun lead(key: K, flight: CompletableDeferred<Result<V>?>, block: suspend () -> V): Result<V> {
        var result: Result<V>? = null
        try {
            result = attempted { block() }
        } finally {
            withContext(NonCancellable) { lock.withLock { flights.remove(key) } }
            flight.complete(result)
        }
        return result
    }
}
