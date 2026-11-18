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

package com.davils.kore.testing

private const val HIGH_SURROGATE: Int = 0xD800
private const val LOW_SURROGATE: Int = 0xDC00

/**
 * Pieces of text an attacker could send: text that tries to forge a line of a log, to control a terminal, to hide
 * characters or to turn the direction of what follows, next to plain letters, digits, quotes and backslashes.
 *
 * A test of one attack uses a single fragment, such as [FORGED_LOG_LINE] in the value of a field. A property test
 * combines them, with Kotest for example as `Arb.list(Arb.element(HostileFragments.all), 0..64)` joined to one text,
 * and checks whatever the code under test shows with [unsafeCodePointsIn] or [isSafeToShow]. [all] lists every
 * fragment in a fixed order.
 *
 * @since 2.1.0
 */
public object HostileFragments {
    /**
     * The escape character `U+001B`, which starts every control sequence of a terminal.
     *
     * @since 2.1.0
     */
    public const val ESCAPE: String = "\u001B"

    /**
     * The right-to-left override `U+202E`, which shows everything after it in reverse.
     *
     * @since 2.1.0
     */
    public const val RIGHT_TO_LEFT_OVERRIDE: String = "‮"

    /**
     * The line separator `U+2028`, which some viewers show as a line break.
     *
     * @since 2.1.0
     */
    public const val LINE_SEPARATOR: String = " "

    /**
     * The soft hyphen `U+00AD`, which is invisible in most places.
     *
     * @since 2.1.0
     */
    public const val SOFT_HYPHEN: String = "­"

    /**
     * A high surrogate `U+D800` without the low surrogate that must follow it.
     *
     * It is no compile-time constant: the metadata of a Kotlin library stores constants as UTF-8, which has no lone
     * surrogates, so a constant would reach other modules as `U+FFFD` on Wasm.
     *
     * @since 2.1.0
     */
    public val LONE_HIGH_SURROGATE: String = Char(HIGH_SURROGATE).toString()

    /**
     * A low surrogate `U+DC00` without the high surrogate that must precede it.
     *
     * It is no compile-time constant, for the same reason as [LONE_HIGH_SURROGATE].
     *
     * @since 2.1.0
     */
    public val LONE_LOW_SURROGATE: String = Char(LOW_SURROGATE).toString()

    /**
     * The byte order mark `U+FEFF`, an invisible character that some tools strip and others keep.
     *
     * @since 2.1.0
     */
    public const val BYTE_ORDER_MARK: String = "﻿"

    /**
     * The tag character `U+E0041`, an invisible copy of the letter `A` that can smuggle text past a reader.
     *
     * @since 2.1.0
     */
    public const val TAG_LATIN_A: String = "󠁁"

    /**
     * The variation selector `U+FE0F`, which changes how the character before it is drawn.
     *
     * @since 2.1.0
     */
    public const val VARIATION_SELECTOR: String = "️"

    /**
     * The control sequence that clears the screen of a terminal.
     *
     * @since 2.1.0
     */
    public const val CLEAR_SCREEN: String = "$ESCAPE[2J"

    /**
     * The control sequence that writes text into the clipboard of a terminal.
     *
     * @since 2.1.0
     */
    public const val CLIPBOARD_WRITE: String = "$ESCAPE]52;c;aGVsbG8=\u0007"

    /**
     * A line break followed by a line that looks like a genuine entry of a log.
     *
     * @since 2.1.0
     */
    public const val FORGED_LOG_LINE: String = "\n2026-11-16 INFO auth ok user=admin"

    /**
     * The combining grapheme joiner `U+034F`, an invisible combining mark.
     *
     * @since 2.1.0
     */
    public const val COMBINING_GRAPHEME_JOINER: String = "͏"

    /**
     * The Hangul filler `U+3164`, a letter that is drawn as blank space.
     *
     * @since 2.1.0
     */
    public const val HANGUL_FILLER: String = "ㅤ"

    /**
     * The halfwidth Hangul filler `U+FFA0`, a letter that is drawn as blank space.
     *
     * @since 2.1.0
     */
    public const val HALFWIDTH_HANGUL_FILLER: String = "ﾠ"

    /**
     * The Hangul choseong filler `U+115F`, a letter that is drawn as nothing.
     *
     * @since 2.1.0
     */
    public const val HANGUL_CHOSEONG_FILLER: String = "ᅟ"

    /**
     * The Khmer inherent vowel `U+17B4`, a letter that is drawn as nothing.
     *
     * @since 2.1.0
     */
    public const val KHMER_VOWEL_INHERENT_AQ: String = "឴"

    /**
     * The Mongolian free variation selector one `U+180B`, which is invisible.
     *
     * @since 2.1.0
     */
    public const val MONGOLIAN_VARIATION_SELECTOR: String = "᠋"

    /**
     * The Mongolian free variation selector four `U+180F`, which is invisible.
     *
     * @since 2.1.0
     */
    public const val MONGOLIAN_VARIATION_SELECTOR_FOUR: String = "᠏"

    /**
     * The combining long solidus overlay `U+0338`, a combining mark that is safe after a base character but strikes
     * through whatever comes before it when it stands alone.
     *
     * @since 2.1.0
     */
    public const val COMBINING_LONG_SOLIDUS: String = "̸"

    /**
     * The Hebrew letter alef `U+05D0`, a printable letter that is written from right to left.
     *
     * @since 2.1.0
     */
    public const val HEBREW_ALEF: String = "א"

    /**
     * Every fragment: the values of this object and plain letters, digits, punctuation, quotes,
     * a backslash, a space, an umlaut, a CJK character, an emoji, the control characters `CR`, `LF`, `TAB`, `NUL`,
     * `DEL` and `NEL`, and the zero width space.
     *
     * The list cannot be changed, not even through a cast.
     *
     * @since 2.1.0
     */
    public val all: List<String> = listOf(
        "a", "Z", "7", ".", "[", "]", "\"", "\\", " ", "ä", "日", "😀",
        ESCAPE, RIGHT_TO_LEFT_OVERRIDE, LINE_SEPARATOR, SOFT_HYPHEN, LONE_HIGH_SURROGATE, LONE_LOW_SURROGATE,
        BYTE_ORDER_MARK, TAG_LATIN_A, VARIATION_SELECTOR, "\r", "\n", "\t", "\u0000", "\u007F", "\u0085", "​",
        CLEAR_SCREEN, CLIPBOARD_WRITE, FORGED_LOG_LINE, COMBINING_GRAPHEME_JOINER, HANGUL_FILLER,
        HALFWIDTH_HANGUL_FILLER, HANGUL_CHOSEONG_FILLER, KHMER_VOWEL_INHERENT_AQ, MONGOLIAN_VARIATION_SELECTOR,
        MONGOLIAN_VARIATION_SELECTOR_FOUR, COMBINING_LONG_SOLIDUS, HEBREW_ALEF,
    ).immutableCopy()
}
