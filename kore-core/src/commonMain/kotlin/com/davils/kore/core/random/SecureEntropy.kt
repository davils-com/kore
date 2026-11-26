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

import com.davils.kore.core.random.internal.platformEntropy

/**
 * The cryptographically secure random generator of the platform: `java.security.SecureRandom` on the JVM and
 * `crypto.getRandomValues` on Wasm, in the browser and under Node.js.
 *
 * It is the default [Entropy] of every Davils library. It holds no state a caller can change, never reseeds from a
 * value a caller gives it and is safe for any number of threads. The bits it returns are fit for keys, tokens and
 * anything an attacker must not predict.
 *
 * @since 2.4.0
 */
public object SecureEntropy : Entropy {
    private val source: Entropy = platformEntropy()

    /**
     * Returns 64 secure random bits.
     *
     * @return the bits.
     * @since 2.4.0
     */
    override fun nextLong(): Long = source.nextLong()

    /**
     * Returns [count] secure random bytes.
     *
     * @param count the number of bytes, from zero up to [com.davils.kore.core.Limits.MAX_ENTROPY_BYTES].
     * @return a new array of [count] bytes.
     * @throws IllegalArgumentException if [count] is negative or larger than
     * [com.davils.kore.core.Limits.MAX_ENTROPY_BYTES].
     * @since 2.4.0
     */
    override fun nextBytes(count: Int): ByteArray = source.nextBytes(count)

    /**
     * Returns `SecureEntropy`, never a random value.
     *
     * @return the name of the generator.
     * @since 2.4.0
     */
    override fun toString(): String = "SecureEntropy"
}
