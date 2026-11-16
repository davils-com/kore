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

package com.davils.kore.core.arguments

/**
 * What a [KoreParameters] schema does with an argument before `--` that names no declared parameter, or is a value that
 * belongs to none.
 *
 * @since 1.0.0
 */
public enum class KoreUnknownArgumentPolicy {
    /**
     * Refuses the load with a violation for every such argument; this is the default and fails closed.
     *
     * An unknown long argument is reported by its name without the value written into it, together with a similar
     * declared name when there is one, and a stray value is reported by its position only.
     *
     * @since 1.0.0
     */
    Reject,

    /**
     * Ignores such arguments and hands them over through [KoreParameters.unknown] and [KoreParameters.unknownNames].
     *
     * This suits a program that passes the arguments it does not know on to another program. The raw arguments in
     * [KoreParameters.unknown] are kept exactly as given, values and secrets included, so a program that logs them
     * should log [KoreParameters.unknownNames] instead.
     *
     * @since 1.0.0
     */
    Collect,
}
