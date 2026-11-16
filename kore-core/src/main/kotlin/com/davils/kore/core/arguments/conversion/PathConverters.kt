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

import java.io.File
import java.nio.file.Path

internal fun pathConverter(
    requirements: Set<KorePathRequirement>,
    inspector: PathInspector,
    flavour: PathFlavour = PathFlavour.current,
): KoreConverter<Path> {
    val rules = PathRules(requirements, inspector, flavour)
    return NamedConverter(typeName = "path", isCheckingDefaults = true, reading = rules::read)
}

internal fun fileConverter(
    requirements: Set<KorePathRequirement>,
    inspector: PathInspector,
    flavour: PathFlavour = PathFlavour.current,
): KoreConverter<File> {
    val rules = PathRules(requirements, inspector, flavour)
    return NamedConverter(typeName = "file", isCheckingDefaults = true) { raw -> rules.read(raw).toFile() }
}
