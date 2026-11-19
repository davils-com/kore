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

package com.davils.kore.core.naming

import com.davils.kore.core.Limits
import com.davils.kore.core.text.internal.SafeDisplay

private const val LOWER_LETTER: String = "a lower-case letter"
private const val LOWER_LETTER_OR_DIGIT: String = "a lower-case letter or a digit"

/**
 * A rule for names and identifiers, such as the names of modules, the ids of plugins or the keys of settings, that
 * checks a text in one pass, without a regular expression.
 *
 * A name is lower-case ASCII. It starts with a letter, or with a letter or a digit as [NameStart] decides, and goes on
 * with letters, digits and the word separators of the rule, such as `-`. A rule with a segment separator, such as
 * `.`, divides a name into parts, each of which follows these rules on its own, and may require a least number of
 * parts. A strict rule refuses two word separators in a row and a part that ends with one. Upper-case letters,
 * white space and every character outside ASCII are refused by every rule.
 *
 * The four common rules are [KEBAB], [DOTTED], [QUALIFIED] and [STRICT_KEBAB]; [nameRule] builds any other one, such
 * as a rule for URI schemes or for the ids of keys. A rule refuses a text longer than its [maxLength] before it looks
 * at a single character, and checks every other text in time that grows linearly with its length, so it is safe to
 * apply to input of any origin. A valid name contains nothing but letters, digits and separators of ASCII and is at
 * most [Limits.MAX_NAME_LENGTH] characters long, so it may be shown and logged as it is. A rule is immutable and safe
 * to share between threads.
 *
 * @property maxLength the longest name, in characters, that the rule accepts.
 * @property example a name that follows the rule, shown in messages, such as `warm-cache`.
 * @since 2.1.0
 */
public class NameRule internal constructor(
    private val grammar: NameGrammar,
    public val maxLength: Int,
    public val example: String,
) {
    /**
     * Tells whether [text] follows this rule.
     *
     * @param text the text to check, which may contain anything.
     * @return `true` if [text] is a valid name.
     * @since 2.1.0
     */
    public fun matches(text: CharSequence): Boolean = grammar.problemOf(text, maxLength) == null

    /**
     * Returns [text] if it follows this rule, and `null` otherwise.
     *
     * @param text the text to check, which may contain anything.
     * @return [text], or `null` if it is not a valid name.
     * @since 2.1.0
     */
    public fun validOrNull(text: String): String? = text.takeIf { candidate -> matches(candidate) }

    /**
     * Returns [text] if it follows this rule, and throws an exception that says why otherwise.
     *
     * It is meant for the `init` block of an identifier type, such as `PluginId(value)`. The message names the text,
     * escaped and shortened to [Limits.MAX_DISPLAY_LENGTH] characters, the [kind] of name, an [example] and the reason,
     * such as `"Core" is not a plugin id such as acme.billing: it contains a character other than lower-case
     * letters, digits, '-' and '.'`. Use it only for names that are no secret.
     *
     * @param text the text to check, which may contain anything.
     * @param kind what the name names, such as `plugin id`; escaped in the message.
     * @param example a valid name shown in the message; this rule's [example] by default; escaped in the message.
     * @return [text], unchanged.
     * @throws IllegalArgumentException if [text] is not a valid name.
     * @since 2.1.0
     */
    public fun requireValid(text: String, kind: String, example: String = this.example): String {
        val problem = grammar.problemOf(text, maxLength) ?: return text
        val shown = SafeDisplay.quoted(text)
        val described = "${SafeDisplay.text(kind)} such as ${SafeDisplay.text(example)}"
        throw IllegalArgumentException("$shown is not a $described: it ${reasonFor(problem)}")
    }

    internal fun problemOf(text: CharSequence): NameProblem? = grammar.problemOf(text, maxLength)

    internal fun reasonFor(problem: NameProblem): String = when (problem) {
        NameProblem.Empty -> "is empty"
        NameProblem.TooLong -> "is longer than $maxLength characters"
        NameProblem.InvalidStart -> "${partPhrase()}does not start with ${startPhrase()}"
        NameProblem.InvalidCharacter -> "contains a character other than lower-case letters, digits${allowedPhrase()}"
        NameProblem.DoubledSeparator -> "has two separators in a row"
        NameProblem.TrailingSeparator -> "${partPhrase()}ends with a separator"
        NameProblem.EmptySegment -> "has an empty part"
        NameProblem.TooFewSegments -> "has fewer than ${grammar.minSegments} parts"
    }

    private fun partPhrase(): String {
        if (grammar.segmentSeparator == null) return ""
        return "has a part that "
    }

    private fun startPhrase(): String {
        if (grammar.isDigitStartAllowed) return LOWER_LETTER_OR_DIGIT
        return LOWER_LETTER
    }

    private fun allowedPhrase(): String {
        val allowed = grammar.separators + grammar.segmentSeparator?.toString().orEmpty()
        return allowed.map { separator -> ", '$separator'" }.joinToString(separator = "")
    }

    /**
     * The common rules for names.
     *
     * @since 2.1.0
     */
    public companion object {
        /**
         * A name of lower-case words joined by dashes, such as `warm-cache`: `[a-z][a-z0-9-]*`.
         *
         * Two dashes in a row and a dash at the end are accepted, as by the regular expression.
         *
         * @since 2.1.0
         */
        public val KEBAB: NameRule = nameRule {
            example = "warm-cache"
        }

        /**
         * A name of one or more [KEBAB] parts joined by dots, such as `inventory.data-store`:
         * `[a-z][a-z0-9-]*(\.[a-z][a-z0-9-]*)*`.
         *
         * @since 2.1.0
         */
        public val DOTTED: NameRule = nameRule {
            segmentSeparator = '.'
            example = "inventory.data-store"
        }

        /**
         * A [DOTTED] name of at least two parts, such as `acme.billing`, the form of a violation code.
         *
         * @since 2.1.0
         */
        public val QUALIFIED: NameRule = nameRule {
            segmentSeparator = '.'
            minSegments = 2
            example = "acme.billing"
        }

        /**
         * A name of lower-case words joined by single dashes or dots, such as `load-settings`:
         * `[a-z][a-z0-9]*([.-][a-z0-9]+)*`.
         *
         * Unlike [KEBAB] it refuses two separators in a row and a separator at the end.
         *
         * @since 2.1.0
         */
        public val STRICT_KEBAB: NameRule = nameRule {
            separators = "-."
            isStrict = true
            example = "load-settings"
        }
    }
}
