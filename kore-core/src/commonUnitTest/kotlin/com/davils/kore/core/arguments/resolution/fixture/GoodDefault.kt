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
import com.davils.kore.core.arguments.fixture.GOOD_DEFAULT_PORT
import com.davils.kore.core.arguments.fixture.HIGHEST_PORT
import com.davils.kore.core.arguments.fixture.LOWEST_PORT
import com.davils.kore.core.arguments.fixture.PORT_MESSAGE

internal class GoodDefault : Parameters() {
    val port: Int by defaulted("--port", type = Converters.int, default = GOOD_DEFAULT_PORT) {
        validate { value -> if (value !in LOWEST_PORT..HIGHEST_PORT) fail(PORT_MESSAGE) }
    }
}
