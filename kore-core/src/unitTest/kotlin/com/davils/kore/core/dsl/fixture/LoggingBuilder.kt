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

import com.davils.kore.core.dsl.KoreValidatingBuilder

internal class LoggingBuilder : KoreValidatingBuilder<LoggingData>() {
    var level: String = "INFO"
    private val sinks = mutableListOf<SinkData>()
    private val loggers = mutableMapOf<String, LoggerData>()

    fun console(block: ConsoleSinkBuilder.() -> Unit = {}) {
        collect(target = sinks, builder = ConsoleSinkBuilder(), property = LoggingData::sinks, block = block)
    }

    fun file(block: FileSinkBuilder.() -> Unit = {}) {
        collect(target = sinks, builder = FileSinkBuilder(), property = LoggingData::sinks, block = block)
    }

    fun logger(name: String, block: LoggerBuilder.() -> Unit = {}) {
        collect(
            target = loggers,
            key = name,
            builder = LoggerBuilder(),
            property = LoggingData::loggers,
            block = block,
        )
    }

    override fun assemble(): LoggingData = LoggingData(level, sinks.toList(), loggers.toMap())
}

internal fun logging(block: LoggingBuilder.() -> Unit): LoggingData {
    val builder = LoggingBuilder()
    builder.block()
    return builder.build()
}
