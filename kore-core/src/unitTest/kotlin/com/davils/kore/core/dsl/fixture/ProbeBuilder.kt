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
import kotlin.reflect.KProperty

internal class ProbeBuilder(private val onAssemble: ProbeBuilder.() -> Unit = {}) : KoreValidatingBuilder<QueueData>() {
    private val items = mutableListOf<QueueData>()

    var size: Int = QueueBuilder.DEFAULT_QUEUE_SIZE

    var assemblies: Int = 0
        private set

    fun queue(block: QueueBuilder.() -> Unit = {}): QueueData? = nested(QueueBuilder(), FileSinkData::queue, block)

    fun item(property: KProperty<*>, block: QueueBuilder.() -> Unit = {}) {
        collect(target = items, builder = QueueBuilder(), property = property, block = block)
    }

    override fun assemble(): QueueData {
        assemblies++
        onAssemble()
        return QueueData(size)
    }
}
