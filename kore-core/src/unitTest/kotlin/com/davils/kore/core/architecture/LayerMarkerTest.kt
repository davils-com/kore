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

package com.davils.kore.core.architecture

import com.davils.kore.core.architecture.data.KoreDataSource
import com.davils.kore.core.architecture.data.KoreDataTransferObject
import com.davils.kore.core.architecture.data.KoreRepository
import com.davils.kore.core.architecture.domain.KoreDomainModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private val markers = listOf(
    KoreDomainModel::class.java,
    KoreDataTransferObject::class.java,
    KoreDataSource::class.java,
    KoreRepository::class.java,
)

class LayerMarkerTest : FunSpec({
    context("every layer marker") {
        markers.forEach { marker ->
            test("${marker.simpleName} is an interface, so a type carries its layer beside its own supertype") {
                marker.isInterface shouldBe true
            }

            test("${marker.simpleName} declares no member") {
                marker.declaredMethods.toList().shouldBeEmpty()
            }

            test("${marker.simpleName} inherits nothing, so no framework arrives with the layer") {
                marker.interfaces.toList().shouldBeEmpty()
            }
        }
    }

    context("repository") {
        test("names exactly one subject") {
            KoreRepository::class.java.typeParameters.size shouldBe 1
        }

        test("accepts only a domain model as its subject") {
            val subject = KoreRepository::class.java.typeParameters.single()

            subject.bounds.single() shouldBe KoreDomainModel::class.java
        }
    }
})
