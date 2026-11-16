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

import com.davils.kore.core.arguments.conversion.PathInspector
import java.nio.file.Path

private const val DENIED: String = "access denied"

internal object ThrowingInspector : PathInspector {
    override fun exists(path: Path): Boolean = throw SecurityException(DENIED)

    override fun isPresent(path: Path): Boolean = throw SecurityException(DENIED)

    override fun isReadable(path: Path): Boolean = throw SecurityException(DENIED)

    override fun isDirectory(path: Path): Boolean = throw SecurityException(DENIED)

    override fun isRegularFile(path: Path): Boolean = throw SecurityException(DENIED)

    override fun realPath(path: Path): Path = throw SecurityException(DENIED)
}
