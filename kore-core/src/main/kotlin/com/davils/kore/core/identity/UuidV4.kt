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

import com.davils.kore.core.identity.generator.UuidV4Generator
import kotlin.uuid.Uuid as KotlinUuid

private const val VERSION_NAME: String = "version 4"

/**
 * A version 4 UUID: 122 random bits plus the version and variant fields of RFC 9562.
 *
 * A version 4 identifier carries neither a timestamp nor an order of minting; [compareTo] orders it by its bits only.
 * The bits of [random] come from the cryptographically secure generator of the Kotlin standard library, so two
 * identifiers practically never collide. Instances are immutable, safe to share between threads, and are made by
 * [random], [parse] and the readers of [Uuid]. Every instance carries version 4 in the variant of RFC 9562, also
 * one built through the constructor, which Kotlin code cannot call but code in Java or another JVM language can: it
 * refuses any other bits with an [IllegalArgumentException].
 *
 * Unpredictable bits do not make the identifier a secret: like every [Uuid], it must never serve as a capability
 * token or any other value whose knowledge alone grants access.
 *
 * @since 1.0.0
 */
public class UuidV4 internal constructor(value: KotlinUuid) : Uuid(value) {
    init {
        require(versionOf(value) == UuidVersion.Random) { "Not a $VERSION_NAME UUID" }
    }

    override val version: UuidVersion get() = UuidVersion.Random

    /**
     * Mints and reads version 4 identifiers.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Mints a new version 4 identifier from 122 bits of a cryptographically secure random generator.
         *
         * Same as [UuidV4Generator.next]; safe to call from any number of threads. Code that should be testable
         * with fixed identifiers takes a [com.davils.kore.core.identity.generator.UuidGenerator] instead.
         *
         * @return a new version 4 identifier.
         * @since 1.0.0
         */
        public fun random(): UuidV4 = UuidV4Generator.next()

        /**
         * Reads the canonical text form of a version 4 identifier, as [Uuid.parse] does.
         *
         * @param text the text to read, which may come from an untrusted source.
         * @return the version 4 identifier.
         * @throws IllegalArgumentException if [text] is not 36 characters long, is not in canonical form or is not a
         * version 4 identifier, a valid version 7 identifier included. The message names only the length or the
         * expected version, never the text.
         * @since 1.0.0
         */
        public fun parse(text: String): UuidV4 = requireNotNull(parseOrNull(text)) { refusal(text, VERSION_NAME) }

        /**
         * Reads the canonical text form of a version 4 identifier, or returns `null` for anything else.
         *
         * Accepts and refuses exactly what [parse] accepts and refuses.
         *
         * @param text the text to read, which may come from an untrusted source.
         * @return the version 4 identifier, or `null` if [text] is not the canonical form of one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): UuidV4? = Uuid.parseOrNull(text) as? UuidV4
    }
}
