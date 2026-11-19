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

private const val ZERO: Byte = 0
private const val PATTERN_LENGTH: Int = 4
private const val ZERO_MARK: String = "0"
private const val MARK_SEPARATOR: String = " "
private const val HEX: Int = 16
private const val OTHER_MARK: String = "x"

/**
 * An encoding of Unicode text in bytes that [decodeText] reads.
 *
 * @since 2.1.0
 */
@DavilsExperimental
public enum class TextEncoding(markHex: String) {
    /**
     * UTF-8, the encoding of most text on the web and in files.
     *
     * @since 2.1.0
     */
    Utf8("EF BB BF"),

    /**
     * UTF-16 with the most significant byte first.
     *
     * @since 2.1.0
     */
    Utf16Be("FE FF"),

    /**
     * UTF-16 with the least significant byte first, the encoding Windows often writes.
     *
     * @since 2.1.0
     */
    Utf16Le("FF FE"),

    /**
     * UTF-32 with the most significant byte first.
     *
     * @since 2.1.0
     */
    Utf32Be("00 00 FE FF"),

    /**
     * UTF-32 with the least significant byte first.
     *
     * @since 2.1.0
     */
    Utf32Le("FF FE 00 00"),
    ;

    private val mark: IntArray = markOf(markHex)

    /**
     * The number of bytes of the byte order mark of this encoding.
     *
     * @since 2.1.0
     */
    public val byteOrderMarkLength: Int get() = mark.size

    internal fun isMarkedIn(bytes: ByteArray): Boolean =
        bytes.size >= mark.size && mark.indices.all { index -> bytes[index].toInt() and BYTE_MASK == mark[index] }

    /**
     * Detects encodings.
     *
     * @since 2.1.0
     */
    public companion object {
        private val patterns: Map<String, TextEncoding> = mapOf(
            "000x" to Utf32Be,
            "x000" to Utf32Le,
            "0x" to Utf16Be,
            "x0" to Utf16Le,
        )

        /**
         * Detects the encoding of [bytes] as YAML 1.2 and XML 1.0 do.
         *
         * A byte order mark decides first; a UTF-32 mark is tested before the UTF-16 mark it starts with. Without a
         * mark, the pattern of zero bytes among the first four decides: a text that starts with an ASCII character
         * has zero bytes around it in UTF-16 and UTF-32. Every other text is taken to be UTF-8.
         *
         * @param bytes the encoded text; only its first four bytes are read.
         * @return the encoding of [bytes].
         * @since 2.1.0
         */
        public fun detect(bytes: ByteArray): TextEncoding {
            val marked = listOf(Utf32Le, Utf32Be, Utf8, Utf16Be, Utf16Le).firstOrNull { it.isMarkedIn(bytes) }
            return marked ?: patternOf(bytes)
        }

        private fun patternOf(bytes: ByteArray): TextEncoding {
            val marks = (0 until PATTERN_LENGTH).map { index -> markOf(bytes.getOrNull(index)) }
            val pattern = marks.joinToString(separator = "")
            return patterns[pattern] ?: patterns[pattern.take(2)] ?: Utf8
        }

        private fun markOf(byte: Byte?): String = ZERO_MARK.takeIf { byte == ZERO } ?: OTHER_MARK
    }
}

internal const val BYTE_MASK: Int = 0xFF

private fun markOf(hex: String): IntArray {
    val pairs = hex.split(MARK_SEPARATOR)
    return IntArray(pairs.size) { index -> pairs[index].toInt(HEX) }
}
