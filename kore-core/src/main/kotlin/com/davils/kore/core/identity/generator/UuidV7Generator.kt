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

package com.davils.kore.core.identity.generator

import com.davils.kore.core.identity.UuidV7
import com.davils.kore.core.identity.internal.MonotonicUuidV7Generator

/**
 * The generator of version 7 identifiers for production: it reads the system clock and keeps every identifier it
 * mints strictly greater than the one before.
 *
 * Each identifier carries the millisecond the system clock reads, a 12-bit counter in the `rand_a` field of RFC 9562
 * and 62 bits of [java.security.SecureRandom] in the `rand_b` field. In every new millisecond the counter starts at a
 * random value below 2048. While the clock stays in the same millisecond, goes backwards or reads a moment before
 * 1970, the counter counts on from the last identifier, and when it overflows it carries into the timestamp, so an
 * identifier is never smaller than the one minted before it. A burst of more than 2048 identifiers in one millisecond
 * can therefore carry the timestamp ahead of the clock, and after the clock is set back the timestamp stays at the
 * last millisecond until the clock passes it again.
 *
 * The generator is one instance with one counter for every caller of the loaded copy of Kore, normally the whole
 * process, and it is safe for any number of threads. [UuidV7.random] mints through it. Identifiers minted close
 * together share their timestamp and follow each other in the counter, so they are partly predictable from each
 * other; they are identifiers, never secrets.
 *
 * @since 1.0.0
 */
public object UuidV7Generator : UuidGenerator<UuidV7> {
    private val monotonic: MonotonicUuidV7Generator = MonotonicUuidV7Generator()

    /**
     * Mints a new version 7 identifier, greater than every identifier this generator minted before.
     *
     * @return a new version 7 identifier.
     * @throws IllegalStateException if the system clock reads a moment beyond the 48 bits of milliseconds, after the
     * year 10889, or every identifier up to the end of that range was already minted. The timestamp is never cut off
     * and never wraps around.
     * @since 1.0.0
     */
    override fun next(): UuidV7 = monotonic.next()
}
