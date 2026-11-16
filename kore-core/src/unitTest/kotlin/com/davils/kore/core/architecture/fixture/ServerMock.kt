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

package com.davils.kore.core.architecture.fixture

internal class ServerMock(initial: List<ServerDto> = templates) : ServerSource {
    private val stored = initial.toMutableList()

    val written: List<ServerDto> get() = stored.toList()

    override suspend fun servers(): List<ServerDto> = stored.toList()

    override suspend fun save(server: ServerDto) {
        stored.removeAll { it.id == server.id }
        stored += server
    }

    companion object {
        val templates: List<ServerDto> = listOf(
            ServerDto(id = "srv-1", name = "edge-01", rackUnits = 1),
            ServerDto(id = "srv-2", name = "core-01", rackUnits = 2),
        )
    }
}
