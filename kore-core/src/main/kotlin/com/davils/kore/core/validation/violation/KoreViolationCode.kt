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

import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.text.escape.SafeDisplay

private val codePattern = Regex("[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+")

/**
 * A stable, machine readable name for a kind of violation, such as `kore.validation.required`.
 *
 * A code consists of at least two dot-separated segments of ASCII letters in lower case, digits and hyphens, each
 * segment starting with a letter, and is at most [KoreLimits.MAX_DISPLAY_LENGTH] characters long. Because of that
 * alphabet a code is always safe to show and to log as it is. Codes are a wire contract: once released, a code keeps
 * its meaning, so a client can react to it without parsing the message of a violation. Two codes are equal when their
 * text is equal. The codes of Kore start with `kore.`; codes of your own belong under a first segment of your own, so
 * they cannot collide with them.
 *
 * @property value the code as text.
 * @throws IllegalArgumentException if [value] is longer than [KoreLimits.MAX_DISPLAY_LENGTH] or is no dotted name in
 * lower case; the message shows the refused text escaped and shortened.
 * @since 1.0.0
 */
@JvmInline
public value class KoreViolationCode(public val value: String) {
    init {
        require(value.length <= KoreLimits.MAX_DISPLAY_LENGTH && codePattern.matches(value)) {
            "A violation code is a dotted name in lower case such as kore.validation.invalid, " +
                "but was ${SafeDisplay.quoted(value)}"
        }
    }

    /**
     * Returns the code as text, which is always safe to show and to log.
     *
     * @return the code as text, the same as [value].
     * @since 1.0.0
     */
    override fun toString(): String = value

    /**
     * The general codes that every validation may report; the areas of Kore define further codes of their own.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * A value breaks a rule that no more specific code describes.
         *
         * @since 1.0.0
         */
        public val INVALID: KoreViolationCode = KoreViolationCode("kore.validation.invalid")

        /**
         * A value that must be present is missing.
         *
         * @since 1.0.0
         */
        public val REQUIRED: KoreViolationCode = KoreViolationCode("kore.validation.required")

        /**
         * A value is still accepted but will stop being accepted.
         *
         * @since 1.0.0
         */
        public val DEPRECATED: KoreViolationCode = KoreViolationCode("kore.validation.deprecated")

        /**
         * A value appears more than once where it must be unique.
         *
         * @since 1.0.0
         */
        public val DUPLICATE: KoreViolationCode = KoreViolationCode("kore.validation.duplicate")

        /**
         * A validation would have descended deeper than [KoreLimits.MAX_PATH_DEPTH] and stopped there.
         *
         * The error is reported at the deepest path that was still validated; nothing below it was checked.
         *
         * @since 1.0.0
         */
        public val TOO_DEEP: KoreViolationCode = KoreViolationCode("kore.validation.too-deep")

        /**
         * A validation found more than [KoreLimits.MAX_VIOLATIONS] violations and only counted the rest.
         *
         * The error is reported once, after the collected violations, at the root path of the validation that left
         * them out, and its message names how many it left out.
         *
         * @since 1.0.0
         */
        public val TOO_MANY: KoreViolationCode = KoreViolationCode("kore.validation.too-many")
    }
}
