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

import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

internal object FileSystemInspector : PathInspector {
    override fun exists(path: Path): Boolean = path.exists()

    override fun isPresent(path: Path): Boolean = path.exists(LinkOption.NOFOLLOW_LINKS)

    override fun isReadable(path: Path): Boolean = path.isReadable()

    override fun isDirectory(path: Path): Boolean = path.isDirectory()

    override fun isRegularFile(path: Path): Boolean = path.isRegularFile()

    override fun realPath(path: Path): Path? = try {
        path.toRealPath()
    } catch (expected: IOException) {
        null
    }

    override fun isLink(path: Path): Boolean = try {
        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        attributes.isSymbolicLink || attributes.isOther
    } catch (expected: NoSuchFileException) {
        false
    } catch (expectedDenial: IOException) {
        true
    }
}
