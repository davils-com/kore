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

package com.davils.kore.core.arguments.declaration.fixture

import com.davils.kore.core.arguments.KoreParameters
import com.davils.kore.core.arguments.conversion.KoreConverters

internal class ServerParameters : KoreParameters(environmentPrefix = "KORE") {
    val port: Int by required("--port", "-p", type = KoreConverters.int) {
        describedAs("Port the control plane listens on")
    }
    val host: String by defaulted("--host", default = "0.0.0.0")
    val token: String? by optional("--token") { sensitive() }
    val tags: List<String> by multiple("--tag") { split(",") }
    val isDryRun: Boolean by flag("--dry-run") { negatable() }

    init {
        exclusive(::token, ::tags)
    }
}
