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

import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.text.escape.SafeDisplay

private const val CUT_MARK: String = "..."
private const val GUTTER_SEPARATOR: String = " | "
private const val CARET: Char = '^'
private const val TAB: Char = '\t'
private const val SPACE: Char = ' '

/**
 * Returns a copy of this snippet in which the highlighted characters, and everything after them on the shown line,
 * are replaced by the fixed mask `****`.
 *
 * Use it before a snippet that points at a secret, such as a password in a configuration file, is rendered, logged or
 * returned to a client. The mask always has the same four characters and even replaces an empty highlight, and what
 * followed the highlight is dropped, so the copy reveals neither the hidden characters nor how many there were.
 *
 * For a snippet that [KoreLineMap.snippet] made for a range starting where the secret starts, the masked copy is built
 * from the text before the secret alone, at any width, and depends neither on the length nor on the content of the
 * secret, with one exception: when the text before the secret ends with a carriage return and the secret starts with
 * a line feed, the two form one line break, so the position and the line of the copy differ from those of any other
 * secret and reveal that the secret starts with a line feed. A caller that must not reveal even that starts the range
 * of such a secret at the carriage return.
 *
 * The highlight of the copy covers the mask, [isCutAfter][KoreTextSnippet.isCutAfter] is `true`, and the position and
 * [isCutBefore][KoreTextSnippet.isCutBefore] stay as they are. Only the highlight and what follows it are masked: the
 * characters before the highlight are kept unchanged, and the position still shows where the secret starts. The copy
 * is unescaped like any snippet, so it is shown through [render] or [toString][KoreTextSnippet.toString].
 *
 * @return the masked copy; this snippet is not changed.
 * @since 1.0.0
 */
public fun KoreTextSnippet.masked(): KoreTextSnippet {
    val before = line.substring(startIndex = 0, endIndex = highlight.start)
    return KoreTextSnippet(
        position = position,
        line = before + SafeDisplay.REDACTED,
        highlight = KoreTextRange.of(start = before.length, length = SafeDisplay.REDACTED.length),
        isCutBefore = isCutBefore,
        isCutAfter = true,
    )
}

/**
 * Renders this snippet as two lines, the line of text and a line of carets below its highlight, separated by a line
 * feed:
 *
 * ```
 * 3 | ...port: eighty
 *   |          ^^^^^^
 * ```
 *
 * The first line starts with the number of the line, counted from one and computed without overflow, and marks a cut
 * line with `...` at each cut end. The second line puts one `^` under every character the highlight is shown with,
 * escapes included, and a single `^` at the place of an empty highlight.
 *
 * The parts before, inside and after the highlight are escaped separately: control, format and separator characters,
 * bidirectional controls, lone surrogates, private-use and unassigned code points, variation selectors and the other
 * invisible code points that Unicode ignores by default, such as the Hangul fillers, are written as escapes such as
 * `\r`, `\n` or `\u{001B}`, and the carets are placed under the escaped text. A combining mark is written as an escape
 * as well when it starts the line or follows a tab or an escaped character, so that it cannot change how the gutter,
 * the cut mark, a tab or an escape looks. Tabs before the highlight are kept, in both lines, so that the carets stay
 * aligned below them; tabs inside and after the highlight are written as `\t`, so that every one of them has carets
 * below it. The result therefore always has exactly two lines, and a hostile line can neither forge a line of a
 * log, nor send control sequences to a terminal, nor override the order in which it is shown with bidirectional
 * controls. Backslashes are shown as they are, so a line that already contains the text of an escape looks the same as
 * one that contains the escaped character.
 *
 * The highlight is shown in full: call [masked] first when it is a secret. The output is never shortened, so that the
 * carets stay correct, and it grows with the line, by up to ten characters for each escaped character. For a snippet
 * of [KoreLineMap.snippet], whose line is at most [KoreLimits.MAX_SNIPPET_WIDTH] code units long, it is therefore
 * bounded by a fixed multiple of that limit; a snippet built by hand from a long untrusted line is rendered at full
 * length.
 *
 * The carets assume that the line is shown from left to right, one column for every UTF-16 code unit of the escaped
 * text. Wide characters and combining marks can shift them in a terminal. Right-to-left letters, such as Hebrew or
 * Arabic ones, are shown as they are, and a viewer that applies the Unicode bidirectional algorithm shows them, and
 * the digits, spaces and punctuation next to them, in a different order, even in a line shown from left to right: the
 * carets may then stand below other characters than the highlighted ones, and two numbers next to such letters may
 * appear swapped.
 *
 * @return the rendered snippet, two lines without a final line break.
 * @since 1.0.0
 */
public fun KoreTextSnippet.render(): String {
    val number = (position.line.toLong() + 1).toString()
    val (before, marked, after) = SafeDisplay.lineParts(line, highlight)
    val lead = CUT_MARK.takeIf { isCutBefore }.orEmpty()
    val tail = CUT_MARK.takeIf { isCutAfter }.orEmpty()
    val gutter = SPACE.toString().repeat(number.length)
    val indent = before.map(::alignmentOf).joinToString("")
    val carets = CARET.toString().repeat(maxOf(a = 1, b = marked.length))
    val pointer = SPACE.toString().repeat(lead.length) + indent + carets
    return "$number$GUTTER_SEPARATOR$lead$before$marked$after$tail\n$gutter$GUTTER_SEPARATOR$pointer"
}

private fun alignmentOf(character: Char): Char = TAB.takeIf { character == TAB } ?: SPACE
