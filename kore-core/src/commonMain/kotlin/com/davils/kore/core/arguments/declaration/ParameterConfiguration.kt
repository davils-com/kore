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
 * The settings every kind of parameter shares, configured in the block passed to a declaring function of
 * [com.davils.kore.core.arguments.ParameterDeclarations].
 *
 * The block runs once, while the parameter is declared, on the thread that constructs the schema; a configuration must
 * not be kept or used after it. When a setting is made more than once, the last call wins.
 *
 * @since 1.0.0
 */
@DavilsDsl
public sealed class ParameterConfiguration {
    private var description: String? = null
    private var deprecation: String? = null
    private var environment: EnvironmentSetting = EnvironmentSetting.Derived

    /**
     * Sets the description that the usage lists next to the parameter.
     *
     * The description is escaped and shortened to [com.davils.kore.core.Limits.MAX_MESSAGE_LENGTH] when it is
     * listed.
     *
     * @param description what the parameter is for, in a short phrase.
     * @since 1.0.0
     */
    public fun describedAs(description: String) {
        this.description = description
    }

    /**
     * Reads the parameter from the environment variable [variable] instead of the one derived from the environment
     * prefix of the schema.
     *
     * The name is checked when the parameter is registered with its schema: it must be ASCII letters, digits and
     * underscores that do not start with a digit, and no other parameter of the schema may read the same variable;
     * otherwise the registration throws [IllegalArgumentException]. A variable set to the empty string counts as not
     * set.
     *
     * @param variable the name of the environment variable, such as `APP_TOKEN`.
     * @since 1.0.0
     */
    public fun env(variable: String) {
        environment = EnvironmentSetting.Explicit(variable)
    }

    /**
     * Reads the parameter from no environment variable, even when the schema has an environment prefix.
     *
     * @since 1.0.0
     */
    public fun noEnv() {
        environment = EnvironmentSetting.Disabled
    }

    /**
     * Marks the parameter as deprecated, so that giving it, from any place, is reported as a warning and the usage
     * marks it.
     *
     * The parameter keeps working; the warning is a
     * [com.davils.kore.core.arguments.report.ParameterViolation.Deprecated] in
     * [com.davils.kore.core.arguments.Parameters.warnings].
     *
     * @param advice what to use instead, which the warning appends to its message; empty for no advice.
     * @since 1.0.0
     */
    public fun deprecated(advice: String = "") {
        deprecation = advice
    }

    internal fun traitsWith(isSensitive: Boolean, isNegatable: Boolean): ParameterTraits = ParameterTraits(
        description = description,
        deprecation = deprecation,
        environment = environment,
        isSensitive = isSensitive,
        isNegatable = isNegatable,
    )
}
