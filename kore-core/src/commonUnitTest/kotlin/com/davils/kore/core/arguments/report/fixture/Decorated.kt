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

package com.davils.kore.core.arguments.report.fixture

import com.davils.kore.core.arguments.Parameters
import com.davils.kore.core.arguments.conversion.Converters

internal class Decorated(text: String) : Parameters() {
    val name: String by defaulted("--name", default = text) { describedAs(text) }
    val names: List<String> by multiple("--names", default = listOf(text, text))
    val custom: String? by optional("--custom", type = Converters.custom(text) { raw -> raw }) {
        deprecated(text)
    }
}
