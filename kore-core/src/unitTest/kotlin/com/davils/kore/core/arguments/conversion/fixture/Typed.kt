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

package com.davils.kore.core.arguments.conversion.fixture

import com.davils.kore.core.arguments.KoreParameters
import com.davils.kore.core.arguments.conversion.KoreConverters
import java.net.URI
import kotlin.time.Duration

internal class Typed : KoreParameters() {
    val count: Int? by optional("--count", type = KoreConverters.int)
    val size: Long? by optional("--size", type = KoreConverters.long)
    val ratio: Double? by optional("--ratio", type = KoreConverters.double)
    val isEnabled: Boolean? by optional("--enabled", type = KoreConverters.boolean)
    val colour: Colour? by optional("--colour", type = KoreConverters.enum<Colour>())
    val timeout: Duration? by optional("--timeout", type = KoreConverters.duration())
    val endpoint: URI? by optional("--endpoint", type = KoreConverters.uri())
    val level: String? by optional("--level", type = KoreConverters.choice("low", "high"))
    val tags: List<String> by multiple("--tags") { split(",") }
}
