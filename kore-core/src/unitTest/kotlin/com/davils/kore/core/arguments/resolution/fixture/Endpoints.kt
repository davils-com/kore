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

package com.davils.kore.core.arguments.resolution.fixture

import com.davils.kore.core.arguments.Parameters
import com.davils.kore.core.arguments.conversion.Converters
import java.net.URI

internal class Endpoints : Parameters() {
    val endpoint: URI? by optional("--endpoint", type = Converters.uri(allowUserInfo = true))
    val mirrors: List<URI> by multiple("--mirror", type = Converters.uri(allowUserInfo = true))
    val database: URI by defaulted(
        "--database",
        type = Converters.uri(schemes = setOf("postgres"), allowUserInfo = true),
        default = URI("postgres://app:default-password@db/app"),
    )
    val dsn: String? by optional("--dsn")
    val strict: URI? by optional("--strict", type = Converters.uri())
}
