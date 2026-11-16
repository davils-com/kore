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

package com.davils.kore.core.arguments.lexer.fixture

import com.davils.kore.core.arguments.KoreParameters
import com.davils.kore.core.arguments.conversion.KoreConverters

internal class Spellings : KoreParameters(environmentPrefix = "APP") {
    val host: String by required("--host", "-h")
    val port: Int by required("--port", "-p", type = KoreConverters.int)
    val isVerbose: Boolean by flag("--verbose", "-v")
    val isArchive: Boolean by flag("--archive", "-a")
    val isColoured: Boolean by flag("--colour") { negatable() }
    val tags: List<String> by multiple("--tag", "-t")
}
