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
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.internal.SafeDisplay

private const val LAST_CODE_POINT: Int = 0x10FFFF

/**
 * Shows text that may come from an attacker so that it can neither forge a line of a log nor control a terminal, for
 * the Davils libraries that build on Kore.
 *
 * Every function escapes control, format and separator characters, private use and unassigned code points, lone
 * surrogates, bidirectional overrides, invisible fillers and combining marks without a base character: a line break
 * becomes `\n`, a carriage return `\r`, a tab `\t` and every other such code point `\u{XXXX}`. The result is then
 * shortened to the length a caller asks for and marked with the number of characters left out, so the length of
 * what is shown stays bounded however long the input is. The functions are pure, keep no state and are safe to call
 * from any thread.
 *
 * Text is escaped exactly once, at the place where it is shown: a message, a report or a `toString`. Escaping text
 * that is then stored and shown again escapes it twice. Canonical values with a restricted alphabet, such as versions
 * and UUIDs, need no escaping.
 *
 * @since 2.1.0
 */
@DavilsInternal
public object SafeText {
    /**
     * The mask that stands in for a sensitive value wherever it would be shown.
     *
     * It is the same for every value, so it reveals neither the length of a value nor how many values it hides.
     *
     * @since 2.1.0
     */
    public const val REDACTED: String = SafeDisplay.REDACTED

    /**
     * Returns [value] escaped and shortened to at most [maxLength] UTF-16 code units, plus a mark of the characters
     * left out.
     *
     * @param value the text to show, which may contain anything.
     * @param maxLength the most code units of escaped text to show, from zero to [Limits.MAX_MESSAGE_LENGTH];
     * [Limits.MAX_DISPLAY_LENGTH] by default.
     * @return the escaped text, followed by `…(+n chars)` when `n` characters of [value] were left out.
     * @throws IllegalArgumentException if [maxLength] is negative or larger than [Limits.MAX_MESSAGE_LENGTH].
     * @since 2.1.0
     */
    public fun text(value: CharSequence, maxLength: Int = Limits.MAX_DISPLAY_LENGTH): String {
        requireShownLength(maxLength)
        return SafeDisplay.text(value, maxLength)
    }

    /**
     * Returns [value] escaped, shortened like [text] and put in double quotes, with every quote and backslash of
     * [value] escaped as well, so the shown value cannot end its quotes early.
     *
     * @param value the text to show, which may contain anything.
     * @param maxLength the most code units of escaped text to show inside the quotes, from zero to
     * [Limits.MAX_MESSAGE_LENGTH]; [Limits.MAX_DISPLAY_LENGTH] by default.
     * @return the quoted, escaped text, followed by `…(+n chars)` when `n` characters of [value] were left out.
     * @throws IllegalArgumentException if [maxLength] is negative or larger than [Limits.MAX_MESSAGE_LENGTH].
     * @since 2.1.0
     */
    public fun quoted(value: CharSequence, maxLength: Int = Limits.MAX_DISPLAY_LENGTH): String {
        requireShownLength(maxLength)
        return SafeDisplay.quoted(value, maxLength)
    }

    /**
     * Returns whether [codePoint] is shown as it is, rather than as an escape.
     *
     * A combining mark counts as safe here, although [text] escapes one that has no base character before it.
     *
     * @param codePoint the Unicode code point to check; a value outside the range of code points is never safe.
     * @return `true` if [text] shows [codePoint] unchanged after a base character, `false` otherwise.
     * @since 2.1.0
     */
    public fun isSafe(codePoint: Int): Boolean {
        if (codePoint !in 0..LAST_CODE_POINT) return false
        return SafeDisplay.isSafe(codePoint)
    }

    private fun requireShownLength(maxLength: Int) {
        require(maxLength in 0..Limits.MAX_MESSAGE_LENGTH) {
            "The length a value is shown with must lie between 0 and ${Limits.MAX_MESSAGE_LENGTH}, but was $maxLength"
        }
    }
}
