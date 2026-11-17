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

import com.davils.kore.core.arguments.Parameters
import com.davils.kore.core.arguments.conversion.ConversionException
import com.davils.kore.core.arguments.conversion.Converters

private const val PREFIX: String = "id-"

internal class Custom : Parameters() {
    val identifier: String? by optional(
        "--id",
        type = Converters.custom("identifier") { raw ->
            if (!raw.startsWith(PREFIX)) throw ConversionException("must start with '$PREFIX'")
            raw.removePrefix(PREFIX)
        },
    )
}
