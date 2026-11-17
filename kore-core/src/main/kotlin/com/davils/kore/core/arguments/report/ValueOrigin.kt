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

package com.davils.kore.core.arguments.report

import com.davils.kore.core.arguments.ParameterInfo
import com.davils.kore.core.text.escape.SafeDisplay

/**
 * Where the value of a parameter came from: the command line, an environment variable, a value source or the default.
 *
 * Origins are immutable and compared by what they name.
 *
 * @since 1.0.0
 */
public sealed interface ValueOrigin {
    /**
     * Returns how a violation names a value of [parameter] from this origin, which becomes the path of the violation.
     *
     * The label is raw: the name of a variable or a source is not escaped here, and the path escapes it when it is
     * printed.
     *
     * @param parameter the parameter whose value came from this origin.
     * @return the label, such as `--port`, `APP_PORT`, `--port (from vault)` or `--port (default)`.
     * @since 1.0.0
     */
    public fun label(parameter: ParameterInfo): String

    /**
     * The value was given on the command line; it is labelled with the primary name of the parameter.
     *
     * @since 1.0.0
     */
    public data object CommandLine : ValueOrigin {
        override fun label(parameter: ParameterInfo): String = parameter.primaryName
    }

    /**
     * The value was read from the environment variable [variable]; it is labelled with the name of that variable.
     *
     * @property variable the name of the environment variable.
     * @since 1.0.0
     */
    public class Environment(public val variable: String) : ValueOrigin {
        override fun label(parameter: ParameterInfo): String = variable

        /**
         * Compares two origins by the name of their variable.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is an [Environment] with the same [variable].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Environment && variable == other.variable

        /**
         * Returns a hash code derived from the name of the variable.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = variable.hashCode()

        /**
         * Returns the origin with the name of its variable, escaped, such as `Environment(variable=APP_PORT)`.
         *
         * @return the origin as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Environment(variable=${SafeDisplay.text(variable)})"
    }

    /**
     * The value was returned by the [com.davils.kore.core.arguments.ValueSource] named [sourceName]; it is labelled
     * with the primary name of the parameter followed by `(from` and that name.
     *
     * @property sourceName the name of the value source.
     * @since 1.0.0
     */
    public class External(public val sourceName: String) : ValueOrigin {
        override fun label(parameter: ParameterInfo): String = "${parameter.primaryName} (from $sourceName)"

        /**
         * Compares two origins by the name of their source.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is an [External] with the same [sourceName].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is External && sourceName == other.sourceName

        /**
         * Returns a hash code derived from the name of the source.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = sourceName.hashCode()

        /**
         * Returns the origin with the name of its source, escaped, such as `External(sourceName=vault)`.
         *
         * @return the origin as text.
         * @since 1.0.0
         */
        override fun toString(): String = "External(sourceName=${SafeDisplay.text(sourceName)})"
    }

    /**
     * The value is the declared default of the parameter; it is labelled with the primary name followed by `(default)`.
     *
     * @since 1.0.0
     */
    public data object Default : ValueOrigin {
        override fun label(parameter: ParameterInfo): String = "${parameter.primaryName} (default)"
    }
}
