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

package com.davils.kore.core.architecture.domain

import com.davils.kore.core.architecture.domain.fixture.CountServersUseCase
import com.davils.kore.core.architecture.domain.fixture.RenameServerUseCase
import com.davils.kore.core.architecture.fixture.Device
import com.davils.kore.core.architecture.fixture.ServerId
import com.davils.kore.core.architecture.fixture.ServerMock
import com.davils.kore.core.architecture.fixture.ServerRepository
import com.davils.kore.core.architecture.fixture.Switch
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

private const val ACCESS_SWITCH: String = "access-01"

class UseCaseTest : FunSpec({
    context("invocation") {
        test("is called like the function it is") {
            val repository = ServerRepository(ServerMock())
            val rename = RenameServerUseCase(repository)

            val renamed = rename(RenameServerUseCase.Input(ServerId("srv-1"), "edge-02"))

            repository.servers() shouldContain renamed
        }

        test("without input is called with empty parentheses") {
            val count: UseCase<Unit, Int> = CountServersUseCase(ServerRepository(ServerMock()))

            count() shouldBe ServerMock.templates.size
        }

        test("is a lambda where the action is trivial") {
            val nameLength = UseCase<String, Int> { it.length }

            nameLength("edge") shouldBe "edge".length
        }
    }

    context("variance") {
        test("a use case producing a subtype serves where a supertype is expected") {
            val switches = UseCase<String, Switch> { Switch(it, ports = 1) }

            val devices: UseCase<String, Device> = switches

            devices(ACCESS_SWITCH).name shouldBe ACCESS_SWITCH
        }

        test("a use case accepting a supertype serves where a subtype is passed") {
            val anyDevice = UseCase<Device, String> { it.name }

            val forSwitches: UseCase<Switch, String> = anyDevice

            forSwitches(Switch(ACCESS_SWITCH, ports = 1)) shouldBe ACCESS_SWITCH
        }
    }
})
