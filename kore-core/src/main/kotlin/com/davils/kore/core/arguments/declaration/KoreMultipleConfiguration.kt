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

package com.davils.kore.core.arguments.declaration

import com.davils.kore.core.annotation.DavilsDsl

private const val EMPTY_DELIMITER_MESSAGE: String = "A split delimiter must not be empty; " +
    "leave split() out to take one value per occurrence"

/**
 * The settings of a parameter that takes several values, in addition to those of [KoreValueConfiguration].
 *
 * @param T the type of the values of the parameter.
 * @since 1.0.0
 */
@DavilsDsl
public class KoreMultipleConfiguration<T : Any> internal constructor() : KoreValueConfiguration<T>() {
    internal var splitDelimiter: String? = null
        private set

    /**
     * Splits every given value at each occurrence of [delimiter], so that `--tag=a,b` yields two values.
     *
     * The delimiter is matched literally, not as a pattern, and empty pieces are dropped, so `a,,b,` yields `a` and
     * `b`. It applies to values from the command line, the environment and sources, but not to the declared default.
     * The pieces of all occurrences together are bounded by
     * [com.davils.kore.core.arguments.KoreParameterLimits.maxValuesPerParameter]: beyond it, splitting stops and the
     * parameter is refused without any of its values.
     *
     * @param delimiter the text between two values, such as a comma.
     * @throws IllegalArgumentException if [delimiter] is empty.
     * @since 1.0.0
     */
    public fun split(delimiter: String) {
        require(delimiter.isNotEmpty()) { EMPTY_DELIMITER_MESSAGE }
        splitDelimiter = delimiter
    }
}
