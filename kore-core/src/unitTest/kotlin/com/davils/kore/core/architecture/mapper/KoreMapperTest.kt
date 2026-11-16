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

package com.davils.kore.core.architecture.mapper

import com.davils.kore.core.architecture.fixture.Device
import com.davils.kore.core.architecture.fixture.Server
import com.davils.kore.core.architecture.fixture.ServerDto
import com.davils.kore.core.architecture.fixture.ServerId
import com.davils.kore.core.architecture.fixture.ServerMapper
import com.davils.kore.core.architecture.fixture.Switch
import com.davils.kore.core.architecture.mapper.fixture.DeviceDto
import com.davils.kore.core.architecture.mapper.fixture.SwitchDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val ACCESS_SWITCH: String = "access-01"

class KoreMapperTest : FunSpec({
    val edge = Server(id = ServerId("srv-1"), name = "edge-01", rackUnits = 1)
    val edgeDto = ServerDto(id = "srv-1", name = "edge-01", rackUnits = 1)
    val core = Server(id = ServerId("srv-2"), name = "core-01", rackUnits = 2)
    val coreDto = ServerDto(id = "srv-2", name = "core-01", rackUnits = 2)

    context("reading") {
        test("turns a transfer object into the domain model it stands for") {
            ServerMapper.toDomain(edgeDto) shouldBe edge
        }

        test("turns a whole answer into domain models in the order it arrived") {
            ServerMapper.toDomain(listOf(edgeDto, coreDto)) shouldContainExactly listOf(edge, core)
        }
    }

    context("writing") {
        test("turns a domain model into the transfer object that carries it") {
            ServerMapper.toDto(edge) shouldBe edgeDto
        }

        test("turns several domain models into transfer objects in the order they were given") {
            ServerMapper.toDto(listOf(core, edge)) shouldContainExactly listOf(coreDto, edgeDto)
        }
    }

    context("round trip") {
        test("reading what was written gives back every transfer object the pairing carries") {
            checkAll(Arb.string(), Arb.string(), Arb.int()) { id, name, rackUnits ->
                val dto = ServerDto(id = id, name = name, rackUnits = rackUnits)

                ServerMapper.toDto(ServerMapper.toDomain(dto)) shouldBe dto
            }
        }
    }

    context("single direction") {
        test("a pairing that is only read is a lambda") {
            val reader = KoreDomainMapper<SwitchDto, Switch> { Switch(it.name, it.ports) }

            reader.toDomain(SwitchDto(ACCESS_SWITCH, ports = 2)).ports shouldBe 2
        }

        test("a pairing that is only written is a lambda") {
            val writer = KoreDtoMapper<Switch, SwitchDto> { SwitchDto(it.name, it.ports) }

            writer.toDto(listOf(Switch(ACCESS_SWITCH, ports = 1))).single().ports shouldBe 1
        }
    }

    context("variance") {
        test("a mapper producing a subtype serves where a supertype is read") {
            val switches = KoreDomainMapper<SwitchDto, Switch> { Switch(it.name, it.ports) }

            val devices: KoreDomainMapper<SwitchDto, Device> = switches

            devices.toDomain(SwitchDto(ACCESS_SWITCH, ports = 1)).name shouldBe ACCESS_SWITCH
        }

        test("a mapper accepting a supertype serves where a subtype is written") {
            val devices = KoreDtoMapper<Device, DeviceDto> { DeviceDto(it.name) }

            val switches: KoreDtoMapper<Switch, DeviceDto> = devices

            switches.toDto(Switch(ACCESS_SWITCH, ports = 1)).name shouldBe ACCESS_SWITCH
        }
    }
})
