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

import com.davils.kore.core.arguments.KoreParameters
import com.davils.kore.core.arguments.KoreUnknownArgumentPolicy
import com.davils.kore.core.arguments.conversion.KoreConverters

private const val HTTP: Int = 80
private const val HTTPS: Int = 443

internal class Tagged : KoreParameters(unknownArguments = KoreUnknownArgumentPolicy.Collect) {
    val tags: List<String> by multiple("--tag")
    val ports: List<Int> by multiple("--port", type = KoreConverters.int, default = listOf(HTTP, HTTPS))
    val isLegacy: Boolean by flag("--legacy") { deprecated() }
}
