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
import com.davils.kore.core.arguments.conversion.KoreConverters
import com.davils.kore.core.arguments.fixture.HIGHEST_PORT
import com.davils.kore.core.arguments.fixture.LOWEST_PORT

private const val LONGEST_NAME: Int = 8

internal class Validated : KoreParameters() {
    val port: Int? by optional("--port", type = KoreConverters.int) {
        validate { value -> if (value !in LOWEST_PORT..HIGHEST_PORT) fail("must be a valid TCP port") }
    }
    val name: String? by optional("--name") {
        validate { value -> if (value.isBlank()) fail("must not be blank") }
        validate { value -> if (value.length > LONGEST_NAME) fail("must be at most 8 characters") }
    }
    val weights: List<Int> by multiple("--weight", type = KoreConverters.int) {
        validate { value -> if (value < 0) fail("must not be negative") }
    }
}
