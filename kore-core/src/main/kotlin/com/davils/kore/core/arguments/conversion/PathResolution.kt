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

import java.nio.file.Path

private val navigationNames: Set<String> = setOf(".", "..")

internal fun isWithin(path: Path, base: Path, inspector: PathInspector): Boolean {
    val realBase = inspector.realPath(base.toAbsolutePath()) ?: return false
    val target = resolvedTarget(path.toAbsolutePath(), inspector)
    return target != null && target.startsWith(realBase)
}

internal fun hasNoSymlinks(path: Path, inspector: PathInspector): Boolean {
    val absolute = path.toAbsolutePath()
    val real = inspector.realPath(absolute) ?: return isCreatableWithoutSymlinks(absolute, inspector)
    return real == absolute.normalize()
}

private fun isCreatableWithoutSymlinks(absolute: Path, inspector: PathInspector): Boolean {
    val parent = creatableParent(absolute, inspector) ?: return false
    return inspector.realPath(parent) == parent.normalize()
}

private fun resolvedTarget(absolute: Path, inspector: PathInspector): Path? =
    inspector.realPath(absolute) ?: creatableTarget(absolute, inspector)

private fun creatableTarget(absolute: Path, inspector: PathInspector): Path? {
    val parent = creatableParent(absolute, inspector) ?: return null
    return inspector.realPath(parent)?.resolve(absolute.fileName)
}

private fun creatableParent(absolute: Path, inspector: PathInspector): Path? {
    if (inspector.isPresent(absolute) || !isCreatable(absolute)) return null
    return absolute.parent
}

private fun isCreatable(absolute: Path): Boolean {
    val name = absolute.fileName ?: return false
    return name.toString() !in navigationNames
}
