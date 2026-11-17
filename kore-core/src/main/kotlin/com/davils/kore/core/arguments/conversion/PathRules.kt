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

package com.davils.kore.core.arguments.conversion

import java.nio.file.InvalidPathException
import java.nio.file.Path

private const val CONTRADICTION_MESSAGE: String = "A path cannot be required to be both a directory and a regular file"
private const val SEVERAL_BASES_MESSAGE: String = "A path can be confined to one base directory only"

private val checkedInOrder: List<KorePathRequirement> = listOf(
    KorePathRequirement.Readable,
    KorePathRequirement.Directory,
    KorePathRequirement.RegularFile,
    KorePathRequirement.NoSymlinks,
)

internal class PathRules(
    requirements: Set<KorePathRequirement>,
    private val inspector: PathInspector,
    private val flavour: PathFlavour,
) {
    private val demanded: List<KorePathRequirement> = demandedIn(requirements)

    init {
        val isContradictory = KorePathRequirement.Directory in requirements &&
            KorePathRequirement.RegularFile in requirements
        require(!isContradictory) { CONTRADICTION_MESSAGE }
        require(requirements.count { requirement -> requirement is KorePathRequirement.Within } <= 1) {
            SEVERAL_BASES_MESSAGE
        }
    }

    fun read(raw: String): Path {
        pathProblem(raw, flavour)?.let { problem -> throw KoreConversionException(problem) }
        val path = parsed(raw)
        val unmet = demanded.firstOrNull { requirement -> !requirement.isMetBy(path, inspector) }
        if (unmet != null) throw KoreConversionException(unmet.problem)
        return path
    }
}

private fun demandedIn(requirements: Set<KorePathRequirement>): List<KorePathRequirement> = buildList {
    if (requirements.any { requirement -> requirement.isExistenceImplied }) add(KorePathRequirement.Exists)
    addAll(checkedInOrder.filter { requirement -> requirement in requirements })
    addAll(requirements.filterIsInstance<KorePathRequirement.Within>())
}

private fun parsed(raw: String): Path = try {
    Path.of(raw)
} catch (expected: InvalidPathException) {
    throw KoreConversionException(INVALID_PATH_REASON)
}
