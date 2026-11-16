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

package com.davils.kore.core.identity

import com.davils.kore.core.identity.generator.KoreUuidV7Generator
import kotlin.time.Instant
import kotlin.uuid.Uuid

private const val VERSION_NAME: String = "version 7"
private const val TIMESTAMP_SHIFT: Int = 16

/**
 * A version 7 UUID: a 48-bit timestamp in milliseconds since 1970-01-01T00:00:00Z, the version and variant fields of
 * RFC 9562 and 74 further bits.
 *
 * Identifiers minted by [random] in one process are strictly increasing in the order they are minted, also within one
 * millisecond and while the system clock goes backwards, so they suit keys that should be stored in the order they
 * were made; [KoreUuidV7Generator] describes how. Instances are immutable, safe to share between threads, and are
 * made by [random], [parse], the readers of [KoreUuid] and generators. Every instance carries version 7 in the variant
 * of RFC 9562, also one built through the constructor, which Kotlin code cannot call but code in Java or another JVM
 * language can: it refuses any other bits with an [IllegalArgumentException].
 *
 * Anyone who sees a version 7 identifier can read when it was minted, so do not use one where that moment is
 * confidential. The timestamp of an identifier that was read rather than minted is whatever its sender wrote into it:
 * treat [epochMilliseconds] and [instant] of such an identifier as data the sender controls, never as proof of when
 * something happened and never as the basis of an expiry, an authorisation or any other security decision. Like every
 * [KoreUuid], it must never serve as a capability token.
 *
 * @since 1.0.0
 */
public class KoreUuidV7 internal constructor(value: Uuid) : KoreUuid(value) {
    init {
        require(versionOf(value) == KoreUuidVersion.TimeOrdered) { "Not a $VERSION_NAME UUID" }
    }

    override val version: KoreUuidVersion get() = KoreUuidVersion.TimeOrdered

    /**
     * The milliseconds since 1970-01-01T00:00:00Z that the identifier carries in its upper 48 bits.
     *
     * The value lies between zero and 2^48 - 1, a moment in the year 10889. For an identifier read from outside the
     * process it is controlled by whoever wrote the identifier.
     *
     * @since 1.0.0
     */
    public val epochMilliseconds: Long get() = mostSignificantBits ushr TIMESTAMP_SHIFT

    /**
     * The moment the identifier carries, [epochMilliseconds] as an [Instant].
     *
     * For an identifier read from outside the process it is controlled by whoever wrote the identifier.
     *
     * @since 1.0.0
     */
    public val instant: Instant get() = Instant.fromEpochMilliseconds(epochMilliseconds)

    /**
     * Mints and reads version 7 identifiers.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Mints a new version 7 identifier from the system clock, greater than every identifier minted before it by
         * [random] or [KoreUuidV7Generator] in this process.
         *
         * Same as [KoreUuidV7Generator.next]; safe to call from any number of threads. Code that should be testable
         * with fixed identifiers takes a [com.davils.kore.core.identity.generator.KoreUuidGenerator] instead.
         *
         * @return a new version 7 identifier.
         * @throws IllegalStateException if the system clock reads a moment beyond the 48 bits of milliseconds, after
         * the year 10889, or every identifier up to the end of that range was already minted. The timestamp is never
         * cut off and never wraps around.
         * @since 1.0.0
         */
        public fun random(): KoreUuidV7 = KoreUuidV7Generator.next()

        /**
         * Reads the canonical text form of a version 7 identifier, as [KoreUuid.parse] does.
         *
         * The timestamp of the result is whatever the text carries; it is not compared with the clock.
         *
         * @param text the text to read, which may come from an untrusted source.
         * @return the version 7 identifier.
         * @throws IllegalArgumentException if [text] is not 36 characters long, is not in canonical form or is not a
         * version 7 identifier, a valid version 4 identifier included. The message names only the length or the
         * expected version, never the text.
         * @since 1.0.0
         */
        public fun parse(text: String): KoreUuidV7 = requireNotNull(parseOrNull(text)) { refusal(text, VERSION_NAME) }

        /**
         * Reads the canonical text form of a version 7 identifier, or returns `null` for anything else.
         *
         * Accepts and refuses exactly what [parse] accepts and refuses.
         *
         * @param text the text to read, which may come from an untrusted source.
         * @return the version 7 identifier, or `null` if [text] is not the canonical form of one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): KoreUuidV7? = KoreUuid.parseOrNull(text) as? KoreUuidV7
    }
}
