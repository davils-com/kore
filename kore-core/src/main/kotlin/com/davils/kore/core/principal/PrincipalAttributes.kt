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

package com.davils.kore.core.principal

import com.davils.kore.core.Limits
import com.davils.kore.core.text.SafeDisplay

private const val ATTRIBUTES_HASH_FACTOR: Int = 31
private const val NAME_PREFIX: String = "An attribute name "
private const val TOO_MANY_MESSAGE: String =
    "A principal carries at most ${Limits.MAX_ATTRIBUTES} attributes"

/**
 * Further facts about the caller of a [Principal], by name, such as the claims of a verified token.
 *
 * A set of attributes is immutable and safe to share between threads; [with] and [without] return a new set. Names
 * are compared exactly, case included. Every attribute is sensitive unless it is marked otherwise explicitly, and
 * [toString] masks every value, sensitive or not, so a set of attributes never prints a value. [get] returns a value
 * whatever its sensitivity: code that writes values somewhere itself, such as into an audit log or an export, asks
 * [isSensitive] first.
 *
 * Every name and value is checked when it is added, and a refused one throws an [IllegalArgumentException]:
 * - a name is not blank, that is, neither empty nor made only of whitespace and the braille pattern blank U+2800,
 *   it is at most [Limits.MAX_ATTRIBUTE_NAME_LENGTH] long, and it may start or end with whitespace;
 * - a value may be empty or blank and is at most [Limits.MAX_ATTRIBUTE_VALUE_LENGTH] long;
 * - neither contains a character that is unsafe to show: a control, format, line separator, paragraph separator,
 *   private-use, unassigned or lone surrogate character, a variation selector, the Mongolian free variation
 *   selectors included, or any other character that Unicode marks as default ignorable because it shows as nothing,
 *   such as a Hangul filler or the combining grapheme joiner;
 * - a set holds at most [Limits.MAX_ATTRIBUTES] attributes.
 *
 * Lengths are counted in UTF-16 code units. The message of the exception never contains the value; it names the
 * attribute, quoted and escaped, only when its value is refused. [PrincipalBuilder.attribute] applies the same
 * rules but reports a refused attribute as a violation instead of throwing.
 *
 * @since 1.0.0
 */
public class PrincipalAttributes private constructor(values: Map<String, String>, sensitive: Set<String>) {
    private val values: Map<String, String> = buildMap(values.size) { putAll(values) }
    private val sensitive: Set<String> = buildSet(sensitive.size) { addAll(sensitive) }

    /**
     * The names of all attributes, as an immutable set.
     *
     * @since 1.0.0
     */
    public val names: Set<String> get() = values.keys

    /**
     * Whether the set holds no attribute at all.
     *
     * @since 1.0.0
     */
    public val isEmpty: Boolean get() = values.isEmpty()

    private constructor() : this(emptyMap(), emptySet())

    /**
     * Returns the value of the attribute [name], or null when there is no such attribute.
     *
     * The value is returned whatever its sensitivity; ask [isSensitive] before writing it anywhere.
     *
     * @param name the name of the attribute, compared exactly.
     * @return the value, or null.
     * @since 1.0.0
     */
    public operator fun get(name: String): String? = values[name]

    /**
     * Returns whether the set holds an attribute [name].
     *
     * @param name the name of the attribute, compared exactly.
     * @return true if the attribute exists.
     * @since 1.0.0
     */
    public operator fun contains(name: String): Boolean = values.containsKey(name)

    /**
     * Returns whether the attribute [name] is marked as sensitive.
     *
     * [toString] masks every value anyway; the mark tells code that handles values itself which of them it must not
     * write anywhere.
     *
     * @param name the name of the attribute, compared exactly.
     * @return true if the attribute exists and is sensitive; false if it is not sensitive or does not exist.
     * @since 1.0.0
     */
    public fun isSensitive(name: String): Boolean = name in sensitive

    /**
     * Returns a copy of this set in which the attribute [name] has [value].
     *
     * An attribute of the same name is replaced together with its sensitivity, so setting it again without passing
     * [sensitive] marks it as sensitive again. Replacing an attribute is allowed even when the set is full.
     *
     * @param name the name of the attribute.
     * @param value the value of the attribute.
     * @param sensitive whether the value is sensitive; leave it true unless the value is known to be safe to show,
     * such as the issuer of a token.
     * @return the new set.
     * @throws IllegalArgumentException if [name] or [value] breaks a rule, or if the set already holds
     * [Limits.MAX_ATTRIBUTES] attributes and [name] is none of them.
     * @since 1.0.0
     */
    public fun with(name: String, value: String, sensitive: Boolean = true): PrincipalAttributes {
        requireValid(name, value)
        require(name in values || values.size < Limits.MAX_ATTRIBUTES) { TOO_MANY_MESSAGE }
        return PrincipalAttributes(values + (name to value), sensitivityOf(name, sensitive))
    }

    /**
     * Returns a copy of this set without the attribute [name] and its sensitivity.
     *
     * @param name the name of the attribute, compared exactly.
     * @return the new set, or this set itself when it holds no attribute [name].
     * @since 1.0.0
     */
    public fun without(name: String): PrincipalAttributes {
        if (name !in values) return this
        return PrincipalAttributes(values - name, sensitive - name)
    }

    /**
     * Compares two sets of attributes by their names, their values and which of them are sensitive.
     *
     * @param other the object to compare with.
     * @return true if [other] is a set of attributes with the same names, values and sensitivity.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is PrincipalAttributes && values == other.values && sensitive == other.sensitive

    /**
     * Returns a hash code derived from the names, the values and which of them are sensitive.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode() * ATTRIBUTES_HASH_FACTOR + sensitive.hashCode()

    /**
     * Returns the names with every value masked, such as `{"email"=****, "issuer"=****}`, which is safe to log.
     *
     * Every value is replaced by the same mask, sensitive or not, so the rendering reveals neither a value nor its
     * length. The names are sorted, quoted, escaped and shortened to [Limits.MAX_DISPLAY_LENGTH] when needed, so a
     * name cannot break the format.
     *
     * @return the rendering of this set.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shown = values.keys.sorted()
        return shown.joinToString(prefix = "{", postfix = "}") { name ->
            "${SafeDisplay.quoted(name)}=${SafeDisplay.REDACTED}"
        }
    }

    private fun sensitivityOf(name: String, isMarked: Boolean): Set<String> {
        if (isMarked) return sensitive + name
        return sensitive - name
    }

    /**
     * Creates sets of attributes.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The set without any attribute.
         *
         * @since 1.0.0
         */
        public val EMPTY: PrincipalAttributes = PrincipalAttributes()

        /**
         * Creates a set of the given attributes and marks every one of them as sensitive.
         *
         * Use [with] with `sensitive = false` to add an attribute whose value is safe to show. When a name occurs more
         * than once, its last value is kept. More than [Limits.MAX_ATTRIBUTES] entries are refused before any of
         * them is read, even when some of their names repeat.
         *
         * @param entries the attributes, each a pair of name and value.
         * @return the new set.
         * @throws IllegalArgumentException if more than [Limits.MAX_ATTRIBUTES] entries are given, or if a name
         * or a value breaks a rule.
         * @since 1.0.0
         */
        public operator fun invoke(vararg entries: Pair<String, String>): PrincipalAttributes {
            require(entries.size <= Limits.MAX_ATTRIBUTES) { TOO_MANY_MESSAGE }
            return entries.fold(EMPTY) { attributes, (name, value) -> attributes.with(name = name, value = value) }
        }
    }
}

private fun requireValid(name: String, value: String) {
    val problem = problemOf(name, value) ?: return
    throw IllegalArgumentException(problem)
}

private fun problemOf(name: String, value: String): String? {
    val nameProblem = PrincipalTextRule.AttributeName.problemOf(name)
    if (nameProblem != null) return NAME_PREFIX + nameProblem.requirement(PrincipalTextRule.AttributeName)
    return valueProblemOf(name, value)
}

private fun valueProblemOf(name: String, value: String): String? {
    val valueProblem = PrincipalTextRule.AttributeValue.problemOf(value) ?: return null
    val requirement = valueProblem.requirement(PrincipalTextRule.AttributeValue)
    return "The value of the attribute ${SafeDisplay.quoted(name)} $requirement"
}
