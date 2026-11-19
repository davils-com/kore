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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

private const val REFUSED_MESSAGE: String = "The gate admits no new work, because it is draining or closed"

/**
 * Admits work while open and waits for the admitted work when it drains, such as the requests of a service that shuts
 * down gracefully or the operations of a client that is being closed.
 *
 * A gate starts [GateState.Open]. [tryEnter] admits a piece of work with a [Permit], and the work gives the permit back
 * when it is done; [admitted] does both around a block. [drain] stops admitting, [awaitIdle] suspends until the last
 * permit is back, and [close] stops admitting for good. The state and the count of admitted work change together in
 * one atomic step, so no work is admitted after [drain] has returned and the count never drops below zero. A gate
 * keeps no timer of its own: a caller that cannot wait indefinitely wraps [awaitIdle] in `withTimeoutOrNull`.
 *
 * A gate is safe to share between threads and coroutines. It is experimental: its shape may still change.
 *
 * @since 2.1.0
 */
@DavilsExperimental
public class AdmissionGate {
    private val snapshot: MutableStateFlow<GateSnapshot> = MutableStateFlow(GateSnapshot(GateState.Open, 0))

    /**
     * The current state of the gate.
     *
     * @since 2.1.0
     */
    public val state: GateState get() = snapshot.value.state

    /**
     * The number of admitted pieces of work whose permit is not back yet.
     *
     * @since 2.1.0
     */
    public val inFlight: Int get() = snapshot.value.inFlight

    /**
     * Admits a piece of work if the gate is open.
     *
     * @return a permit that the work closes when it is done, or `null` if the gate is draining or closed, or already
     * holds [Int.MAX_VALUE] permits.
     * @since 2.1.0
     */
    public fun tryEnter(): Permit? {
        if (!admit()) return null
        return Permit(this)
    }

    /**
     * Runs [block] as admitted work and gives its permit back when it returns or throws.
     *
     * [block] may suspend, because the function is inline.
     *
     * @param R the type of the result.
     * @param block the work.
     * @return the result of [block].
     * @throws IllegalStateException if the gate is draining or closed; [block] does not run then.
     * @since 2.1.0
     */
    public inline fun <R> admitted(block: () -> R): R = enter().use { block() }

    /**
     * Stops admitting new work and lets the admitted work finish; a gate that is already draining or closed stays as
     * it is.
     *
     * @since 2.1.0
     */
    public fun drain() {
        snapshot.update { current -> current.drained() }
    }

    /**
     * Suspends until the gate admits no new work and every admitted piece of work has given its permit back.
     *
     * It returns at once for a closed gate and for a draining gate without work in flight; for an open gate it waits
     * until someone calls [drain] or [close]. A cancellation of the caller ends the wait and changes nothing.
     *
     * @since 2.1.0
     */
    public suspend fun awaitIdle() {
        snapshot.first { current -> current.isIdle }
    }

    /**
     * Stops admitting new work for good. Permits still in flight may still be given back.
     *
     * @since 2.1.0
     */
    public fun close() {
        snapshot.update { current -> GateSnapshot(GateState.Closed, current.inFlight) }
    }

    internal fun leave() {
        snapshot.update { current -> GateSnapshot(current.state, current.inFlight - 1) }
    }

    @PublishedApi
    internal fun enter(): Permit {
        check(admit()) { REFUSED_MESSAGE }
        return Permit(this)
    }

    private fun admit(): Boolean {
        var isAdmitted = false
        snapshot.update { current ->
            isAdmitted = current.isAdmitting
            current.withOneMore(isAdmitted)
        }
        return isAdmitted
    }
}
