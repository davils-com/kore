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

import com.davils.kore.core.arguments.Parameters
import com.davils.kore.core.arguments.conversion.Converter
import com.davils.kore.core.arguments.conversion.Converters

internal class Defaults : Parameters() {
    val names: List<String> by multiple("--name", default = listOf("alpha", "beta"))
    val secrets: List<String> by multiple("--secret", default = listOf("hidden")) { sensitive() }
    val isLoud: Boolean by flag("--loud", default = true)
    val described: String by defaulted("--described", default = "plain") { describedAs("Says what it is") }
    val raw: String? by optional("--raw", type = Converter { raw -> raw.reversed() })
    val numbers: List<Int> by multiple("--number", type = Converters.int)
}
