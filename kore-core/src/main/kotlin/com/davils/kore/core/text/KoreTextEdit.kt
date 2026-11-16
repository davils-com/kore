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

package com.davils.kore.core.text

private const val HASH_FACTOR: Int = 31

/**
 * One change of a text: the characters of [range] are replaced by [replacement].
 *
 * An empty range inserts the replacement at its offset, an empty replacement deletes the range, and an edit with both
 * empty changes nothing. The range refers to the text before any edit is applied; [edited] applies a set of edits to a
 * text and [mapOffset] follows an offset through them. An edit is not bound to a text, so the constructor accepts any
 * range and a replacement of any length; the functions that apply it check that it fits.
 *
 * An edit is an immutable value and safe to share between threads. Two edits are equal when their ranges and their
 * replacements are equal.
 *
 * The replacement may come from untrusted input or hold a secret, so [toString] shows only its length and never its
 * content, and the refusals of [edited] and [mapOffset] name only ranges, offsets and lengths.
 *
 * @property range the range of the original text that is replaced.
 * @property replacement the text that takes the place of the range, kept exactly as given.
 * @since 1.0.0
 */
public class KoreTextEdit(public val range: KoreTextRange, public val replacement: String) {
    /**
     * How many UTF-16 code units the edit adds to the length of the text; negative when it removes more than it adds.
     *
     * @since 1.0.0
     */
    public val delta: Int get() = replacement.length - range.length

    /**
     * Whether the edit replaces no character and so only inserts its replacement.
     *
     * An edit with an empty range and an empty replacement is both an insertion and a [deletion][isDeletion].
     *
     * @since 1.0.0
     */
    public val isInsertion: Boolean get() = range.isEmpty

    /**
     * Whether the replacement is empty, so that the edit only removes the characters of its range.
     *
     * @since 1.0.0
     */
    public val isDeletion: Boolean get() = replacement.isEmpty()

    /**
     * Returns whether [other] is an edit of the same range with the same replacement.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal edit, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is KoreTextEdit && range == other.range && replacement == other.replacement

    /**
     * Returns a hash code derived from the range and the replacement.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * range.hashCode() + replacement.hashCode()

    /**
     * Returns the edit as one line, such as `KoreTextEdit(range=2..4, replacement=3 chars)`.
     *
     * The text names the range and the number of UTF-16 code units of the replacement, but never the replacement
     * itself, so it is safe to log even when the replacement is a secret or contains control characters.
     *
     * @return the range and the length of the replacement.
     * @since 1.0.0
     */
    override fun toString(): String = "KoreTextEdit(range=$range, replacement=${replacement.length} chars)"
}
