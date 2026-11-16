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

package com.davils.kore.core.arguments.fixture

import com.davils.kore.core.arguments.KoreParameterLimits
import com.davils.kore.core.arguments.KoreParameters

internal const val FEWEST_ARGUMENTS: Int = 4
internal const val SHORTEST_ARGUMENT: Int = 16
internal const val FEWEST_VALUES: Int = 3

internal class Limited : KoreParameters(
    environmentPrefix = "APP",
    limits = KoreParameterLimits(
        maxArguments = FEWEST_ARGUMENTS,
        maxArgumentLength = SHORTEST_ARGUMENT,
        maxValuesPerParameter = FEWEST_VALUES,
    ),
) {
    val name: String? by optional("--name")
    val secret: String? by optional("--secret") { sensitive() }
    val tags: List<String> by multiple("--tag") { split(",") }
    val items: List<String> by multiple("--item")
}
