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

package com.davils.kore.core.arguments.resolution

import com.davils.kore.core.arguments.declaration.ParameterSpec
import com.davils.kore.core.arguments.report.ParameterViolation

internal class GroupCheck(
    private val exclusive: List<List<ParameterSpec<*, *>>>,
    private val dependencies: List<Pair<ParameterSpec<*, *>, List<ParameterSpec<*, *>>>>,
) {
    fun violations(provided: Set<ParameterSpec<*, *>>): List<ParameterViolation> =
        exclusiveViolations(provided) + dependencyViolations(provided)

    private fun exclusiveViolations(provided: Set<ParameterSpec<*, *>>): List<ParameterViolation> =
        exclusive.mapNotNull { group ->
            val given = group.filter { spec -> spec in provided }
            ParameterViolation.MutuallyExclusive(given.map { spec -> spec.info }).takeIf { given.size > 1 }
        }

    private fun dependencyViolations(provided: Set<ParameterSpec<*, *>>): List<ParameterViolation> =
        dependencies.mapNotNull { (subject, required) ->
            val missing = required.filterNot { spec -> spec in provided }
            val violation = ParameterViolation.MissingDependency(subject.info, missing.map { spec -> spec.info })
            violation.takeIf { subject in provided && missing.isNotEmpty() }
        }
}
