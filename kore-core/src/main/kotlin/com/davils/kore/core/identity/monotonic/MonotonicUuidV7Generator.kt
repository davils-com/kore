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

package com.davils.kore.core.identity.monotonic

import com.davils.kore.core.identity.UuidV7
import com.davils.kore.core.identity.generator.UuidGenerator
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Clock
import kotlin.uuid.Uuid as KotlinUuid

private const val COUNTER_BITS: Int = 12
private const val COUNTER_MASK: Long = 0xFFFL
private const val TIMESTAMP_SHIFT: Int = 16
private const val VERSION_BITS: Long = 0x7000L
private const val VARIANT_BITS: Long = Long.MIN_VALUE
private const val RANDOM_MASK: Long = 0x3FFF_FFFF_FFFF_FFFFL
private const val SEED_BOUND: Int = 0x800
private const val MAX_TIMESTAMP: Long = 0xFFFF_FFFF_FFFFL
private const val MAX_STAMP: Long = 0x0FFF_FFFF_FFFF_FFFFL

internal class MonotonicUuidV7Generator(
    private val clock: Clock = Clock.System,
    private val random: SecureRandom = SecureRandom(),
) : UuidGenerator<UuidV7> {
    private val last: AtomicLong = AtomicLong(0L)

    override fun next(): UuidV7 {
        val stamp = advance()
        val timestamp = stamp ushr COUNTER_BITS
        val high = timestamp shl TIMESTAMP_SHIFT or VERSION_BITS or (stamp and COUNTER_MASK)
        val low = random.nextLong() and RANDOM_MASK or VARIANT_BITS
        return UuidV7(KotlinUuid.fromLongs(mostSignificantBits = high, leastSignificantBits = low))
    }

    private fun advance(): Long {
        val now = clock.now().toEpochMilliseconds()
        check(now <= MAX_TIMESTAMP) {
            "The clock reads $now milliseconds since the epoch, beyond the 48 bits a version 7 identifier holds"
        }
        return last.updateAndGet { previous -> following(previous, now) }
    }

    private fun following(previous: Long, now: Long): Long {
        if (now > previous ushr COUNTER_BITS) return now shl COUNTER_BITS or random.nextInt(SEED_BOUND).toLong()
        check(previous < MAX_STAMP) {
            "Every version 7 identifier up to the end of the 48-bit range of milliseconds was already handed out"
        }
        return previous + 1
    }
}
