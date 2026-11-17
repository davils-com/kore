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

package com.davils.kore.core.arguments.conversion.fixture

import com.davils.kore.core.arguments.conversion.PathInspector
import java.nio.file.Path

private val ROOT: Path = Path.of("").toAbsolutePath().root

internal class ComponentInspector(
    present: Set<String>,
    links: Set<String> = emptySet(),
) : PathInspector {
    private val presentPaths: Set<Path> = present.mapTo(mutableSetOf(ROOT), ::pathOf)
    private val linkPaths: Set<Path> = links.map(::pathOf).toSet()

    fun pathOf(relative: String): Path = ROOT.resolve(relative)

    override fun exists(path: Path): Boolean = path in presentPaths

    override fun isPresent(path: Path): Boolean = path in presentPaths

    override fun isReadable(path: Path): Boolean = path in presentPaths

    override fun isDirectory(path: Path): Boolean = path in presentPaths

    override fun isRegularFile(path: Path): Boolean = path in presentPaths

    override fun realPath(path: Path): Path? = path.takeIf { candidate -> candidate in presentPaths }?.let(::longName)

    override fun isLink(path: Path): Boolean = path in linkPaths

    private fun longName(path: Path): Path {
        val shortName = path.toString()
        return Path.of(shortName.replace(oldValue = "PROGRA~1", newValue = "Program Files"))
    }
}
