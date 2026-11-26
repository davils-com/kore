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

package com.davils.kore.core.random

import com.davils.kore.core.Limits

private const val NON_NEGATIVE_SHIFT: Int = 33
private const val NON_NEGATIVE_RANGE: Long = 1L shl 31
private const val BITS_PER_BYTE: Int = 8

/**
 * A source of random bits, such as the jitter of a backoff, the key of a hash or the order of a shuffle.
 *
 * Production code draws from [SecureEntropy], the cryptographically secure generator of the platform. A test may pass
 * a predictable source, such as a lambda over a fixed sequence, to make what depends on randomness repeatable; code
 * that keeps a secret or picks something an attacker must not predict never takes a predictable source.
 *
 * Only [nextLong] has to be written; [nextInt] and [nextBytes] are derived from it, without bias. Whether a source is
 * safe to share between threads is up to the source; [SecureEntropy] is.
 *
 * @since 2.4.0
 */
public fun interface Entropy {
    /**
     * Returns 64 random bits.
     *
     * @return the bits.
     * @since 2.4.0
     */
    public fun nextLong(): Long

    /**
     * Returns a random number from zero up to [bound], exclusive, every number equally likely.
     *
     * The number is drawn by rejection from 31 bits of [nextLong], so no number is favoured by a modulo; a source that
     * keeps returning bits that are rejected keeps the call drawing.
     *
     * @param bound the exclusive upper bound, at least one.
     * @return the number.
     * @throws IllegalArgumentException if [bound] is zero or negative.
     * @since 2.4.0
     */
    public fun nextInt(bound: Int): Int {
        require(bound > 0) { "The bound of a random number must be positive, but was $bound" }
        val acceptedRange = NON_NEGATIVE_RANGE / bound * bound
        var candidate = nextLong() ushr NON_NEGATIVE_SHIFT
        while (candidate >= acceptedRange) candidate = nextLong() ushr NON_NEGATIVE_SHIFT
        return (candidate % bound).toInt()
    }

    /**
     * Returns [count] random bytes.
     *
     * Each call of [nextLong] fills eight bytes, least significant byte first.
     *
     * @param count the number of bytes, from zero up to [Limits.MAX_ENTROPY_BYTES].
     * @return a new array of [count] bytes.
     * @throws IllegalArgumentException if [count] is negative or larger than [Limits.MAX_ENTROPY_BYTES]; nothing is
     * drawn then.
     * @since 2.4.0
     */
    public fun nextBytes(count: Int): ByteArray {
        require(count in 0..Limits.MAX_ENTROPY_BYTES) {
            "The number of random bytes must be between 0 and ${Limits.MAX_ENTROPY_BYTES}, but was $count"
        }
        val bytes = ByteArray(count)
        var bits = 0L
        for (index in 0 until count) {
            val offset = index % Long.SIZE_BYTES
            if (offset == 0) bits = nextLong()
            bytes[index] = (bits ushr offset * BITS_PER_BYTE).toByte()
        }
        return bytes
    }
}
