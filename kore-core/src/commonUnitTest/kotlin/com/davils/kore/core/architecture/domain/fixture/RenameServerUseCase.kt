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

package com.davils.kore.core.architecture.domain.fixture

import com.davils.kore.core.architecture.domain.UseCase
import com.davils.kore.core.architecture.fixture.Server
import com.davils.kore.core.architecture.fixture.ServerId
import com.davils.kore.core.architecture.fixture.ServerRepository

internal class RenameServerUseCase(
    private val servers: ServerRepository,
) : UseCase<RenameServerUseCase.Input, Server> {
    data class Input(val id: ServerId, val name: String)

    override suspend fun invoke(input: Input): Server {
        val existing = servers.servers().single { it.id == input.id }
        val renamed = existing.copy(name = input.name)
        servers.save(renamed)
        return renamed
    }
}
