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

package com.davils.kore.core.text.encoding

import com.davils.kore.core.annotation.DavilsExperimental

/**
 * Why [decodeText] could not read bytes as text: where the first problem lies and what it is.
 *
 * The reason is constant text and never contains the bytes, so a failure is safe to log whatever was read.
 *
 * @property offset the position of the first byte that could not be read, counted from 0 at the start of the bytes,
 * the byte order mark included.
 * @property encoding the encoding the bytes were read in.
 * @property reason what is wrong, such as `ends in the middle of a character`.
 * @since 2.1.0
 */
@DavilsExperimental
public class DecodingFailure(public val offset: Long, public val encoding: TextEncoding, public val reason: String) {
    private val identity: List<Any> get() = listOf(offset, encoding, reason)

    /**
     * Tells whether [other] is a failure at the same offset, in the same encoding, for the same reason.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is equal.
     * @since 2.1.0
     */
    override fun equals(other: Any?): Boolean = other is DecodingFailure && identity == other.identity

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 2.1.0
     */
    override fun hashCode(): Int = identity.hashCode()

    /**
     * Returns the failure as one line, such as `Utf8 at byte 12: holds a sequence that is no character`.
     *
     * @return the failure as text.
     * @since 2.1.0
     */
    override fun toString(): String = "$encoding at byte $offset: $reason"
}
