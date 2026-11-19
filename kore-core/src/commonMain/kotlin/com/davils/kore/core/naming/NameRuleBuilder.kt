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
import com.davils.kore.core.annotation.DavilsDsl

private const val WORD_SEPARATORS: String = "-_.+"
private const val SEGMENT_SEPARATORS: String = "./:"

/**
 * Configures a [NameRule] inside the block of [nameRule].
 *
 * The builder starts with the rule [NameRule.KEBAB] without an example: names that start with a lower-case letter,
 * consist of lower-case letters, digits and `-`, have no parts and are at most [Limits.MAX_NAME_LENGTH] characters
 * long. A rule is part of the code that uses it, so a contradictory configuration is a programming error that
 * [nameRule] refuses with an [IllegalArgumentException]. A builder serves one block and is not thread-safe.
 *
 * @since 2.1.0
 */
@DavilsDsl
public class NameRuleBuilder internal constructor() {
    /**
     * What the first character of the name and of each of its parts may be; [NameStart.Letter] by default.
     *
     * @since 2.1.0
     */
    public var start: NameStart = NameStart.Letter

    /**
     * The characters that may join the words of a part, any of `-`, `_`, `.` and `+`; `-` by default, and empty for
     * names of a single word.
     *
     * @since 2.1.0
     */
    public var separators: String = "-"

    /**
     * The character that divides a name into parts, one of `.`, `/` and `:`, or `null` for names without parts, the
     * default.
     *
     * @since 2.1.0
     */
    public var segmentSeparator: Char? = null

    /**
     * The least number of parts a name has, 1 by default; more than 1 needs a [segmentSeparator].
     *
     * @since 2.1.0
     */
    public var minSegments: Int = 1

    /**
     * Whether two separators in a row and a part that ends with a separator are refused; `false` by default.
     *
     * @since 2.1.0
     */
    public var isStrict: Boolean = false

    /**
     * The longest name, in characters, from 1 to [Limits.MAX_NAME_LENGTH], the default.
     *
     * @since 2.1.0
     */
    public var maxLength: Int = Limits.MAX_NAME_LENGTH

    /**
     * A name that follows the rule, shown in messages, such as `load-settings`; it must be set and must follow the
     * rule.
     *
     * @since 2.1.0
     */
    public var example: String = ""

    internal fun build(): NameRule {
        require(separators.all { separator -> separator in WORD_SEPARATORS }) {
            "The word separators of a name rule are some of '$WORD_SEPARATORS', but were '$separators'"
        }
        val segment = segmentSeparator
        require(segment == null || isSegmentSeparator(segment)) {
            "The segment separator of a name rule is one of '$SEGMENT_SEPARATORS' that is no word separator"
        }
        require(minSegments >= 1 && (minSegments == 1 || segment != null)) {
            "A name rule requires at least one part, and more than one only with a segment separator"
        }
        require(maxLength in 1..Limits.MAX_NAME_LENGTH) {
            "The longest name of a name rule lies between 1 and ${Limits.MAX_NAME_LENGTH} characters, " +
                "but was $maxLength"
        }
        val grammar = NameGrammar(
            start = start,
            separators = separators,
            segmentSeparator = segment,
            minSegments = minSegments,
            isStrict = isStrict,
        )
        val rule = NameRule(grammar = grammar, maxLength = maxLength, example = example)
        require(rule.matches(example)) { "The example of a name rule must follow the rule itself" }
        return rule
    }

    private fun isSegmentSeparator(separator: Char): Boolean =
        separator in SEGMENT_SEPARATORS && separator !in separators
}
