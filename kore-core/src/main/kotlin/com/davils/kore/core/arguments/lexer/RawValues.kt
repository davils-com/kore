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

package com.davils.kore.core.arguments.lexer

import com.davils.kore.core.arguments.report.ValueOrigin
import com.davils.kore.core.text.escape.SafeDisplay

internal data class RawValues(
    val raws: List<String>,
    val origin: ValueOrigin,
    val occurrences: Int = raws.size,
) {
    init {
        require(occurrences >= raws.size) { "Values cannot have occurred less often than they were kept" }
    }

    override fun toString(): String =
        "RawValues(raws=${SafeDisplay.REDACTED}, origin=$origin, occurrences=$occurrences)"
}
