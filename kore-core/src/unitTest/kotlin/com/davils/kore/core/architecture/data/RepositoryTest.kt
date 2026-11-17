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

package com.davils.kore.core.architecture.data

import com.davils.kore.core.architecture.fixture.Server
import com.davils.kore.core.architecture.fixture.ServerDto
import com.davils.kore.core.architecture.fixture.ServerId
import com.davils.kore.core.architecture.fixture.ServerMapper
import com.davils.kore.core.architecture.fixture.ServerMock
import com.davils.kore.core.architecture.fixture.ServerRepository
import com.davils.kore.core.architecture.mapper.toDomain
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly

class RepositoryTest : FunSpec({
    context("reading") {
        test("hands back domain models, never the transfer objects of its source") {
            val repository = ServerRepository(ServerMock())

            repository.servers() shouldContainExactly ServerMapper.toDomain(ServerMock.templates)
        }
    }

    context("writing") {
        test("hands the source the transfer object of the domain model it was given") {
            val source = ServerMock(initial = emptyList())
            val repository = ServerRepository(source)

            repository.save(Server(id = ServerId("srv-3"), name = "lab-01", rackUnits = 1))

            source.written shouldContainExactly listOf(ServerDto(id = "srv-3", name = "lab-01", rackUnits = 1))
        }
    }
})
