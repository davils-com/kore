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

import com.davils.kore.core.Limits

/**
 * The part of one line of a text around a highlighted range, ready to be shown with [render], such as the place of a
 * syntax error in a configuration file.
 *
 * [LineMap.snippet] makes snippets from a text; a snippet can also be built by hand. The [line] is kept exactly as
 * it was taken from the text, without any escaping, so it may contain control characters, bidirectional overrides or
 * secrets of the input. [render] and [toString] escape it before they show it; code that shows [line] in any other way
 * must escape it itself. To hide the highlighted characters, such as a password, call [masked] before the snippet is
 * shown or logged.
 *
 * The constructor checks only that the highlight fits the line. It does not limit the length of the line: the snippets
 * of [LineMap.snippet] and their masked copies are at most [Limits.MAX_SNIPPET_WIDTH] code units long, but a
 * snippet built by hand keeps a line of any length, and [render] shows it in full.
 *
 * A snippet is an immutable value and safe to share between threads. Two snippets are equal when all five properties
 * are equal.
 *
 * @property position the position in the whole text where the highlighted range starts, counted from zero.
 * @property line the shown part of the line, as UTF-16 code units of the original text and without the line break.
 * @property highlight the highlighted range, as offsets into [line] rather than into the whole text.
 * @property isCutBefore whether the line goes on before the shown part, which [render] marks with `...`.
 * @property isCutAfter whether the line goes on after the shown part, which [render] marks with `...`.
 * @throws IllegalArgumentException if [highlight] ends after the end of [line]. The message names only the highlight
 * and the length of the line, never the line itself.
 * @since 1.0.0
 */
public class TextSnippet(
    public val position: TextPosition,
    public val line: String,
    public val highlight: TextRange,
    public val isCutBefore: Boolean,
    public val isCutAfter: Boolean,
) {
    init {
        require(highlight.end <= line.length) {
            "The highlight $highlight lies outside the line of length ${line.length}"
        }
    }

    private val identity: List<Any> get() = listOf(position, line, highlight, isCutBefore, isCutAfter)

    /**
     * Returns whether [other] is a snippet with the same position, line, highlight and cut marks.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal snippet, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is TextSnippet && identity == other.identity

    /**
     * Returns a hash code derived from the position, the line, the highlight and the cut marks.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = identity.hashCode()

    /**
     * Returns the snippet as one line, such as
     * `TextSnippet(position=1:7, line="port: 80", highlight=6..8, isCutBefore=false, isCutAfter=false)`.
     *
     * The line is quoted and escaped: control, format and separator characters, bidirectional controls, lone
     * surrogates, private-use and unassigned code points, variation selectors and the other invisible code points that
     * Unicode ignores by default are written as escapes such as `\n` or `\u{001B}`. Quotes and backslashes are escaped
     * as `\"` and `\\`, so two different lines are never shown alike unless both are shortened, and a combining mark is
     * written as an escape as well when it starts the line or follows an escaped character, so that it cannot change
     * how the opening quote or an escape looks. A line whose escaped form is longer than
     * [Limits.MAX_DISPLAY_LENGTH] is shortened, and a note such as `…(+12 chars)` after the closing quote says how
     * many code units of the line were left out. The result is therefore always one line that is safe to log, but it
     * shows the content of the line, highlight included: mask a snippet that holds a secret with [masked] first.
     *
     * @return the snippet with its line quoted and escaped.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shown = SafeDisplay.quoted(line)
        return "TextSnippet(position=$position, line=$shown, highlight=$highlight, " +
            "isCutBefore=$isCutBefore, isCutAfter=$isCutAfter)"
    }
}
