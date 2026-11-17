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

import com.davils.kore.core.identity.UuidV7
import com.davils.kore.core.identity.generator.UuidGenerator
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Instant

/**
 * A deterministic generator of version 7 identifiers for tests: the identifier at position n, counted from zero,
 * carries the millisecond [start] plus n and the sequence number n, and nothing random.
 *
 * Two generators with the same [start] hand out the same identifiers in the same order, so a test can predict every
 * identifier and compare them across runs. The identifiers are strictly increasing and valid version 7 identifiers
 * that every reader of Kore accepts. The generator is safe for concurrent use: every position is handed out exactly
 * once, and the identifiers each thread receives are increasing.
 *
 * The identifiers are predictable by design. The generator belongs in tests only, and `kore-testing` never belongs on
 * the class path of production code.
 *
 * @property start the moment of the first identifier, 1970-01-01T00:00:00Z by default; only whole milliseconds count.
 * @throws IllegalArgumentException if [start] lies before 1970-01-01T00:00:00Z or beyond the 48 bits of milliseconds
 * a version 7 identifier holds, after a moment in the year 10889.
 * @since 1.0.0
 */
public class KoreSequentialUuidV7Generator(
    public val start: Instant = Instant.fromEpochMilliseconds(0),
) : UuidGenerator<UuidV7> {
    private val startMilliseconds: Long = start.toEpochMilliseconds()
    private val sequence: AtomicLong = AtomicLong(0L)

    init {
        require(startMilliseconds in 0..MAX_TIMESTAMP) {
            "A version 7 identifier holds 48 bits of milliseconds, so the start must lie between " +
                "${Instant.fromEpochMilliseconds(0)} and ${Instant.fromEpochMilliseconds(MAX_TIMESTAMP)}, " +
                "but was $start"
        }
    }

    /**
     * How many identifiers this generator has handed out; a call of [next] that was refused does not count.
     *
     * @since 1.0.0
     */
    public val issued: Long get() = sequence.get()

    /**
     * Hands out the identifier at the next position: millisecond [start] plus [issued], sequence number [issued].
     *
     * @return the next identifier, greater than every identifier this generator handed out before.
     * @throws IllegalStateException once the generator has handed out an identifier for every millisecond up to the
     * end of the 48-bit range. It stays used up and never wraps around.
     * @since 1.0.0
     */
    override fun next(): UuidV7 {
        val capacity = MAX_TIMESTAMP - startMilliseconds + 1
        val position = sequence.getAndAccumulate(1L) { current, step -> minOf(a = current + step, b = capacity) }
        check(position < capacity) {
            "This generator started at $start and has handed out one identifier for every millisecond up to " +
                "${Instant.fromEpochMilliseconds(MAX_TIMESTAMP)}, the end of the 48-bit range"
        }
        return sequentialUuidV7(epochMilliseconds = startMilliseconds + position, sequence = position)
    }
}
