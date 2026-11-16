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

package com.davils.kore.gradle

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

/**
 * Hands javac the classes the Kotlin compiler wrote as part of the module that `module-info.java` declares.
 *
 * The Kotlin Gradle plugin compiles the classes of a module before javac compiles its descriptor, but it never tells
 * javac that those classes belong to the module. Without this patch every `exports` of the descriptor fails, because
 * javac sees the exported packages on the class path only.
 *
 * @property moduleName the name the descriptor declares, such as `com.davils.kore.core`.
 * @property classes the directory the Kotlin compiler wrote the classes of the module to.
 * @since 1.0.0
 */
public class ModulePatch(
    @get:Input public val moduleName: String,
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE) public val classes: Provider<Directory>,
) : CommandLineArgumentProvider {
    /**
     * Returns the `--patch-module` option that adds [classes] to the module [moduleName].
     *
     * @since 1.0.0
     */
    override fun asArguments(): Iterable<String> {
        val location = classes.get().asFile.absolutePath
        return listOf("--patch-module", "$moduleName=$location")
    }
}
