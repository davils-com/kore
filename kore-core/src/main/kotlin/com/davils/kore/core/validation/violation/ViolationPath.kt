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

package com.davils.kore.core.validation.violation

import com.davils.kore.core.Limits
import com.davils.kore.core.immutableList
import com.davils.kore.core.text.SafeDisplay
import kotlin.reflect.KProperty

private const val SEPARATOR: Char = '.'
private val reservedInNames: Set<Char> = setOf(SEPARATOR, '[', ']', '"', '\\')

/**
 * Where in a validated value a violation was found, such as `sinks[1].queue.size` or `labels["zone"]`.
 *
 * A path is a list of [segments]: names of properties, indexes of lists and keys of maps. It starts at [ROOT] or at
 * [named] or [keyed], and every function that extends it returns a new path. A path is immutable and therefore safe to
 * share between threads; two paths are equal when their segments are equal.
 *
 * Names and keys are kept exactly as given, so they may hold text that came from the validated input, such as the key
 * of a map read from a request. The text form of a path escapes everything that could be mistaken for a part of the
 * path or that is unsafe to show: a key is always quoted in brackets with JSON escapes, and a name is quoted like a key
 * as soon as it is longer than [Limits.MAX_DISPLAY_LENGTH] or contains a separator, a bracket, a quote, a
 * backslash, whitespace other than a space or a character that the text form cannot show as it is. The escaped text
 * of a quoted name or key is shortened to [Limits.MAX_DISPLAY_LENGTH] UTF-16 code units and marked as shortened.
 * The text form is meant for people; code that needs the parts of a path reads [segments] instead of parsing
 * [toString].
 *
 * The number of segments is not limited by the path itself. A
 * [ValidationScope][com.davils.kore.core.validation.ValidationScope] stops descending at
 * [Limits.MAX_PATH_DEPTH] segments.
 *
 * @since 1.0.0
 */
public class ViolationPath private constructor(segments: List<Segment>) {
    /**
     * The parts of the path, from the outermost to the innermost.
     *
     * The list cannot be changed, not even through a cast to a mutable list.
     *
     * @since 1.0.0
     */
    public val segments: List<Segment> = segments.immutableList()

    /**
     * One part of a [ViolationPath]: a [Name], an [Index] or a [Key].
     *
     * Two segments are equal when they are of the same kind and hold the same value, so a key never equals a name of
     * the same text.
     *
     * @since 1.0.0
     */
    public sealed interface Segment {
        /**
         * The name of a property, or of anything else a validation reports on, such as an option or an environment
         * variable.
         *
         * The constructor accepts every text, the empty one included; [ViolationPath.name] refuses an empty name.
         *
         * @property value the name as declared or as given, unescaped.
         * @since 1.0.0
         */
        @JvmInline
        public value class Name(public val value: String) : Segment {
            /**
             * Returns the name as it appears in a path.
             *
             * A plain name, such as `queueSize` or `--port`, is shown as it is. A name that is longer than
             * [Limits.MAX_DISPLAY_LENGTH] or holds a character that could be mistaken for a part of the path or is
             * unsafe to show is quoted and escaped like a [Key], such as `["a.b"]`.
             *
             * @return the name as it appears in a path, safe to show.
             * @since 1.0.0
             */
            override fun toString(): String {
                if (isPlain(value)) return value
                return "[${SafeDisplay.quoted(value)}]"
            }
        }

        /**
         * The position of an element in a list.
         *
         * @property value the position, starting at zero.
         * @throws IllegalArgumentException if [value] is negative.
         * @since 1.0.0
         */
        @JvmInline
        public value class Index(public val value: Int) : Segment {
            init {
                require(value >= 0) { "An index in a violation path cannot be negative, but was $value" }
            }

            /**
             * Returns the index in brackets, such as `[1]`.
             *
             * @return the index as it appears in a path.
             * @since 1.0.0
             */
            override fun toString(): String = "[$value]"
        }

        /**
         * The key of an entry in a map, which may come from untrusted input.
         *
         * @property value the key as given, unescaped; code that shows it must escape it, as [toString] does.
         * @since 1.0.0
         */
        @JvmInline
        public value class Key(public val value: String) : Segment {
            /**
             * Returns the key in brackets, quoted with JSON escapes, such as `["zone"]`.
             *
             * Quotes and backslashes are escaped; control, format, separator, private-use and unassigned characters,
             * bidirectional overrides, variation selectors, invisible fillers and joiners such as U+3164 and lone
             * surrogates are written as escapes such as `\u{202E}`, and so is a combining mark that would otherwise
             * attach to the character before the key. The escaped text is shortened to
             * [Limits.MAX_DISPLAY_LENGTH] UTF-16 code units and marked as shortened, so a hostile key can neither
             * end the brackets early nor forge a line of a log.
             *
             * @return the key as it appears in a path, safe to show.
             * @since 1.0.0
             */
            override fun toString(): String = "[${SafeDisplay.quoted(value)}]"
        }
    }

    /**
     * Whether the path has no segments and therefore points at the validated value itself.
     *
     * @since 1.0.0
     */
    public val isRoot: Boolean get() = segments.isEmpty()

    /**
     * The number of segments of the path.
     *
     * @since 1.0.0
     */
    public val depth: Int get() = segments.size

    /**
     * Returns this path extended by a name.
     *
     * @param value the name, which must not be empty; it is kept as given and escaped wherever the path is shown.
     * @return the extended path.
     * @throws IllegalArgumentException if [value] is empty.
     * @since 1.0.0
     */
    public fun name(value: String): ViolationPath {
        require(value.isNotEmpty()) { "A name in a violation path cannot be empty" }
        return ViolationPath(segments + Segment.Name(value))
    }

    /**
     * Returns this path extended by the name of [property].
     *
     * Only the name of the property is used; the property is neither read nor kept.
     *
     * @param property the property whose name is appended.
     * @return the extended path.
     * @since 1.0.0
     */
    public fun name(property: KProperty<*>): ViolationPath = name(property.name)

    /**
     * Returns this path extended by the position of an element in a list.
     *
     * @param value the position, starting at zero.
     * @return the extended path.
     * @throws IllegalArgumentException if [value] is negative.
     * @since 1.0.0
     */
    public fun index(value: Int): ViolationPath = ViolationPath(segments + Segment.Index(value))

    /**
     * Returns this path extended by the key of an entry in a map.
     *
     * @param value the key, which may come from untrusted input; it is kept as given and escaped wherever the path is
     * shown.
     * @return the extended path.
     * @since 1.0.0
     */
    public fun key(value: String): ViolationPath = ViolationPath(segments + Segment.Key(value))

    /**
     * Returns this path followed by the segments of [other].
     *
     * @param other the path to append.
     * @return the combined path; [ROOT] on either side leaves the other path unchanged.
     * @since 1.0.0
     */
    public operator fun plus(other: ViolationPath): ViolationPath = ViolationPath(segments + other.segments)

    /**
     * Returns the path as text, such as `sinks[1].queue.size`, with every untrusted part escaped.
     *
     * Plain names are joined with dots, indexes and keys follow in brackets, and [ROOT] is the empty text. Each
     * segment is written as its own `toString` describes, so the text is safe to show on a terminal and in a log.
     *
     * @return the path as text.
     * @since 1.0.0
     */
    override fun toString(): String {
        val rendered = StringBuilder()
        segments.forEach { segment ->
            val isSeparated = segment is Segment.Name && isPlain(segment.value) && rendered.isNotEmpty()
            if (isSeparated) rendered.append(SEPARATOR)
            rendered.append(segment)
        }
        return rendered.toString()
    }

    /**
     * Compares two paths by their segments.
     *
     * @param other the object to compare with.
     * @return whether [other] is a path with equal segments in the same order.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is ViolationPath && segments == other.segments

    /**
     * Returns a hash code derived from the segments, consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = segments.hashCode()

    /**
     * The empty path and the shortcuts that start a path.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The path without segments, which points at the validated value itself.
         *
         * @since 1.0.0
         */
        public val ROOT: ViolationPath = ViolationPath(emptyList())

        /**
         * Returns a path that consists of one name.
         *
         * @param value the name, which must not be empty; it is kept as given and escaped wherever the path is shown.
         * @return the path.
         * @throws IllegalArgumentException if [value] is empty.
         * @since 1.0.0
         */
        public fun named(value: String): ViolationPath = ROOT.name(value)

        /**
         * Returns a path that consists of the name of [property].
         *
         * @param property the property whose name starts the path; only its name is used.
         * @return the path.
         * @since 1.0.0
         */
        public fun named(property: KProperty<*>): ViolationPath = ROOT.name(property)

        /**
         * Returns a path that consists of one key, for a value that comes from untrusted input.
         *
         * @param value the key; it is kept as given and escaped wherever the path is shown.
         * @return the path.
         * @since 1.0.0
         */
        public fun keyed(value: String): ViolationPath = ROOT.key(value)
    }
}

private fun isPlain(name: String): Boolean {
    if (name.length > Limits.MAX_DISPLAY_LENGTH) return false
    return name.all(::isPlainCharacter)
}

private fun isPlainCharacter(character: Char): Boolean = character == ' ' || isPlainNonSpace(character)

private fun isPlainNonSpace(character: Char): Boolean {
    val isReserved = character in reservedInNames || character.isWhitespace()
    return !isReserved && SafeDisplay.isSafe(character.code)
}
