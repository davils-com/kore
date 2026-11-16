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

/**
 * The settings of a flag, in addition to those of [KoreParameterConfiguration].
 *
 * A flag cannot be sensitive and has no validation rules. A value given to it explicitly, such as `--name=false` or the
 * value of its environment variable or of a source, is read like
 * [com.davils.kore.core.arguments.conversion.KoreConverters.boolean].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class KoreFlagConfiguration internal constructor() : KoreParameterConfiguration() {
    private var isNegatable: Boolean = false

    /**
     * Adds a negated name `--no-name` for every long name `--name` of the flag, which sets it to `false`.
     *
     * A negated name takes no value, so `--no-name=true` is malformed. When a negated name equals a name of another
     * parameter of the schema, registering the flag throws [IllegalArgumentException].
     *
     * @since 1.0.0
     */
    public fun negatable() {
        isNegatable = true
    }

    internal fun flagTraits(): ParameterTraits = traitsWith(isSensitive = false, isNegatable = isNegatable)
}
