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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Verifies that every checked file starts with the Apache License 2.0 header of the project.
 *
 * Detekt checks the header of Kotlin sources only. This task covers what Detekt never reads: build scripts, the
 * build logic, Java sources such as module descriptors, and the property and YAML files of the build.
 *
 * @since 1.0.0
 */
@CacheableTask
public abstract class LicenseHeaderCheck : DefaultTask() {

    /**
     * The files whose header is checked.
     *
     * @since 1.0.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sources: ConfigurableFileCollection

    /**
     * The report that lists every checked file, written when all of them carry the header.
     *
     * @since 1.0.0
     */
    @get:OutputFile
    public abstract val report: RegularFileProperty

    /**
     * Fails the build and names every file whose header is missing or differs.
     *
     * @since 1.0.0
     */
    @TaskAction
    public fun verify() {
        val checked = sources.files.sortedBy { file -> file.path }
        val offending = checked.filterNot(::startsWithHeader)

        if (offending.isNotEmpty()) {
            val names = offending.joinToString(separator = "\n") { file -> "  ${file.path}" }
            throw GradleException("These files do not start with the license header:\n$names")
        }

        val lines = checked.joinToString(separator = "\n", postfix = "\n") { file -> file.path }
        report.get().asFile.writeText(lines)
    }

    private fun startsWithHeader(file: File): Boolean {
        val text = file.readText().removePrefix(BYTE_ORDER_MARK)
        val header = headerFor(file)

        return text.startsWith(header)
    }

    private fun headerFor(file: File): String {
        val isHashCommented = file.extension in HASH_COMMENTED_EXTENSIONS
        if (isHashCommented) return HASH_HEADER

        return BLOCK_HEADER
    }

    private companion object {
        const val BYTE_ORDER_MARK: String = "﻿"
        val HASH_COMMENTED_EXTENSIONS: Set<String> = setOf("properties", "yaml", "yml")
        val NOTICE: List<String> = listOf(
            "Copyright 2026 Davils",
            "",
            "Licensed under the Apache License, Version 2.0 (the \"License\");",
            "you may not use this file except in compliance with the License.",
            "You may obtain a copy of the License at",
            "",
            "    http://www.apache.org/licenses/LICENSE-2.0",
            "",
            "Unless required by applicable law or agreed to in writing, software",
            "distributed under the License is distributed on an \"AS IS\" BASIS,",
            "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.",
            "See the License for the specific language governing permissions and",
            "limitations under the License.",
        )
        val BLOCK_HEADER: String = NOTICE.joinToString(separator = "\n", prefix = "/*\n", postfix = "\n */\n") { line ->
            " * $line".trimEnd()
        }
        val HASH_HEADER: String = NOTICE.joinToString(separator = "\n", prefix = "#\n", postfix = "\n#\n") { line ->
            "# $line".trimEnd()
        }
    }
}
