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
 * Text that [decodeText] read from bytes, with the encoding it was read in.
 *
 * @property text the decoded text, without a byte order mark.
 * @property encoding the encoding the bytes were read in.
 * @property hasByteOrderMark whether the bytes started with the byte order mark of [encoding], which is not part of
 * [text].
 * @since 2.1.0
 */
@DavilsExperimental
public class DecodedText(
    public val text: String,
    public val encoding: TextEncoding,
    public val hasByteOrderMark: Boolean,
) {
    private val identity: List<Any> get() = listOf(text, encoding, hasByteOrderMark)

    /**
     * Tells whether [other] is decoded text with the same text, encoding and mark.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is equal.
     * @since 2.1.0
     */
    override fun equals(other: Any?): Boolean = other is DecodedText && identity == other.identity

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 2.1.0
     */
    override fun hashCode(): Int = identity.hashCode()

    /**
     * Describes the decoded text without showing it, such as `DecodedText(Utf8, 120 characters, with mark)`.
     *
     * @return the description.
     * @since 2.1.0
     */
    override fun toString(): String {
        val mark = "with mark".takeIf { hasByteOrderMark } ?: "without mark"
        return "DecodedText($encoding, ${text.length} characters, $mark)"
    }
}
