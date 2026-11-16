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

package com.davils.kore.core.dsl.fixture

import com.davils.kore.core.validation.KoreValidatable
import com.davils.kore.core.validation.KoreValidationScope

internal data class LoggingData(
    val level: String,
    val sinks: List<SinkData>,
    val loggers: Map<String, LoggerData>,
) : KoreValidatable {
    override fun KoreValidationScope.validate() {
        if (sinks.isEmpty()) fail(::sinks, "at least one sink must be configured")
        val names = sinks.map { it.name }
        if (names.size != names.toSet().size) fail(::sinks, "the sink names must be unique")
        if (level == TRACE) warn(::level, "trace on the root logger is expensive in production")
    }

    companion object {
        const val TRACE: String = "TRACE"
    }
}
