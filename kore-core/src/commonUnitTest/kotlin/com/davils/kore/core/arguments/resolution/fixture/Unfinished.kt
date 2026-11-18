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

internal class Unfinished : Parameters() {
    val token: String? by optional("--token") {
        sensitive()
        validate { value -> if (value.isNotEmpty()) TODO("cannot check $value yet") }
    }
    val pin: String? by optional(
        "--pin",
        type = Converters.custom("pin") { raw -> throw AssertionError("unexpected pin $raw") },
    ) { sensitive() }
}
