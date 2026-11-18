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

import com.davils.kore.core.Limits
import kotlin.uuid.Uuid as KotlinUuid

private const val ANY_VERSION: String = "version 4 or version 7"

/**
 * A UUID of version 4 or version 7 in the variant of RFC 9562, the only identifiers Kore reads and mints.
 *
 * Every instance is either a [UuidV4] or a [UuidV7]. It is immutable and safe to share between threads.
 * Two identifiers are equal when all 128 bits are equal, and they are ordered by their bits read as one unsigned
 * number, which is the order of their canonical text forms.
 *
 * Every reader refuses what is not version 4 or version 7 in the variant of RFC 9562, the nil and the max UUID
 * included. Text is read only in the canonical form of 36 characters, five groups of 8, 4, 4, 4 and 12 hexadecimal
 * digits in either case, separated by hyphens; braces, a `urn:uuid:` prefix, surrounding whitespace and the form
 * without hyphens are refused. The length is checked against [Limits.MAX_UUID_TEXT_LENGTH] before anything else,
 * so arbitrarily long input costs nothing. A refusal names only the length of the input or the versions it expected
 * and never repeats the input, so its message is safe to log.
 *
 * A UUID is an identifier, not a secret. Identifiers end up in logs, URLs, error messages and databases without
 * protection, so never use one as a capability token, session identifier, password reset token or any other value
 * whose knowledge alone grants access. A version 7 identifier also reveals when it was minted.
 *
 * @since 1.0.0
 */
public sealed class Uuid(private val value: KotlinUuid) : Comparable<Uuid> {
    /**
     * The version of this identifier: [UuidVersion.Random] for a [UuidV4] and [UuidVersion.TimeOrdered]
     * for a [UuidV7].
     *
     * @since 1.0.0
     */
    public abstract val version: UuidVersion

    /**
     * The upper 64 bits of the identifier as a signed [Long], laid out as in `java.util.UUID.getMostSignificantBits`.
     *
     * @since 1.0.0
     */
    public val mostSignificantBits: Long get() = value.toLongs { high, _ -> high }

    /**
     * The lower 64 bits of the identifier as a signed [Long], laid out as in `java.util.UUID.getLeastSignificantBits`.
     *
     * @since 1.0.0
     */
    public val leastSignificantBits: Long get() = value.toLongs { _, low -> low }

    /**
     * Compares this identifier with [other] by their 128 bits read as one unsigned number, upper half first.
     *
     * The order is the order of the canonical text forms and, for version 7 identifiers, first of all the order of
     * their timestamps. It is consistent with [equals]: the result is zero exactly when both identifiers are equal.
     *
     * @param other the identifier to compare with.
     * @return a negative number, zero or a positive number as this identifier is smaller than, equal to or greater
     * than [other].
     * @since 1.0.0
     */
    final override fun compareTo(other: Uuid): Int {
        val byHigh = mostSignificantBits.toULong().compareTo(other.mostSignificantBits.toULong())
        if (byHigh != 0) return byHigh
        return leastSignificantBits.toULong().compareTo(other.leastSignificantBits.toULong())
    }

    /**
     * Compares this identifier with [other] by all 128 bits.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is a [Uuid] with the same bits, `false` otherwise.
     * @since 1.0.0
     */
    final override fun equals(other: Any?): Boolean = other is Uuid && value == other.value

    /**
     * Returns a hash code derived from all 128 bits.
     *
     * @return the hash code of this identifier.
     * @since 1.0.0
     */
    final override fun hashCode(): Int = value.hashCode()

    /**
     * Returns the canonical text form in lower case, such as `017f22e2-79b0-7cc3-98c4-dc0c0c07398f`.
     *
     * The text consists only of hexadecimal digits and hyphens, so it is safe to show and to log as it is, and
     * [parse] reads it back as an equal identifier.
     *
     * @return the 36 characters of the canonical text form.
     * @since 1.0.0
     */
    final override fun toString(): String = value.toString()

    /**
     * Returns the 16 bytes of the identifier, most significant byte first.
     *
     * Every call returns a new array, so changing it changes neither this identifier nor any other caller's copy.
     *
     * @return a new array of 16 bytes that [fromByteArray] reads back as an equal identifier.
     * @since 1.0.0
     */
    public fun toByteArray(): ByteArray = value.toByteArray()

    /**
     * Returns this identifier as a [kotlin.uuid.Uuid] of the Kotlin standard library with the same bits.
     *
     * @return the identifier as an immutable [kotlin.uuid.Uuid].
     * @since 1.0.0
     */
    public fun toKotlinUuid(): KotlinUuid = value

    /**
     * Reads identifiers of either version from text, bytes and the UUID type of Kotlin.
     *
     * Each reader returns a [UuidV4] or a [UuidV7] according to the version the input carries. All readers are
     * free of side effects and safe to call from any thread.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the canonical text form of a version 4 or version 7 identifier.
         *
         * Hexadecimal digits are accepted in either case and the result always writes them in lower case. The length
         * of [text] is checked against [Limits.MAX_UUID_TEXT_LENGTH] before its content is looked at.
         *
         * @param text the text to read, which may come from an untrusted source.
         * @return the identifier, a [UuidV4] or a [UuidV7].
         * @throws IllegalArgumentException if [text] is not 36 characters long, is not in canonical form or carries
         * another version or variant. The message names only the length or the expected versions, never the text.
         * @since 1.0.0
         */
        public fun parse(text: String): Uuid = requireNotNull(parseOrNull(text)) { refusal(text, ANY_VERSION) }

        /**
         * Reads the canonical text form of a version 4 or version 7 identifier, or returns `null` for anything else.
         *
         * Accepts and refuses exactly what [parse] accepts and refuses.
         *
         * @param text the text to read, which may come from an untrusted source.
         * @return the identifier, or `null` if [text] is not the canonical form of a version 4 or 7 identifier.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Uuid? = uuidOf(text)?.let(::versioned)

        /**
         * Reads an identifier of version 4 or version 7 from its 16 bytes, most significant byte first, as
         * [toByteArray] writes them.
         *
         * The bytes are read once; changing the array afterwards does not change the identifier.
         *
         * @param bytes the bytes to read, which may come from an untrusted source.
         * @return the identifier, a [UuidV4] or a [UuidV7].
         * @throws IllegalArgumentException if [bytes] is not exactly 16 bytes long or carries another version or
         * variant. The message names only the length or the expected versions, never the bytes.
         * @since 1.0.0
         */
        public fun fromByteArray(bytes: ByteArray): Uuid =
            requireNotNull(fromByteArrayOrNull(bytes)) { byteRefusal(bytes, ANY_VERSION) }

        /**
         * Reads an identifier of version 4 or version 7 from its 16 bytes, or returns `null` for anything else.
         *
         * Accepts and refuses exactly what [fromByteArray] accepts and refuses.
         *
         * @param bytes the bytes to read, which may come from an untrusted source.
         * @return the identifier, or `null` if [bytes] is not 16 bytes of a version 4 or 7 identifier.
         * @since 1.0.0
         */
        public fun fromByteArrayOrNull(bytes: ByteArray): Uuid? = uuidOf(bytes)?.let(::versioned)

        /**
         * Takes over a [kotlin.uuid.Uuid] of the Kotlin standard library as an identifier of version 4 or version 7.
         *
         * @param uuid the identifier to take over.
         * @return the identifier with the same bits, a [UuidV4] or a [UuidV7].
         * @throws IllegalArgumentException if [uuid] carries another version or variant, such as the nil UUID.
         * @since 1.0.0
         */
        public fun fromKotlinUuid(uuid: KotlinUuid): Uuid =
            requireNotNull(versioned(uuid)) { "Not a $ANY_VERSION UUID" }
    }
}
