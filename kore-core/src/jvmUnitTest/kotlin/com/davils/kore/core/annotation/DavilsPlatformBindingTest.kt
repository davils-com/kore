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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.core.annotation

import com.davils.kore.core.annotation.fixture.InvisibleAnnotation
import com.davils.kore.core.annotation.fixture.descriptorOf
import com.davils.kore.core.annotation.fixture.invisibleAnnotationsOf
import com.davils.kore.core.annotation.fixture.isDocumented
import com.davils.kore.core.annotation.fixture.retentionOf
import com.davils.kore.core.annotation.fixture.targetsOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class DavilsPlatformBindingTest : FunSpec({
    context("opt-in") {
        test("is internal to kore, so only kore modules mark their platform bindings") {
            val internal = InvisibleAnnotation(descriptorOf(DavilsInternal::class))

            invisibleAnnotationsOf(DavilsPlatformBinding::class) shouldContain internal
        }
    }

    context("retention") {
        test("is kept in the binary, where the coverage engine finds it") {
            retentionOf(DavilsPlatformBinding::class) shouldBe AnnotationRetention.BINARY
        }
    }

    context("targets") {
        test("marks whole classes only") {
            targetsOf(DavilsPlatformBinding::class) shouldBe setOf(AnnotationTarget.CLASS)
        }
    }

    context("documentation") {
        test("is listed on every class it marks") {
            isDocumented(DavilsPlatformBinding::class) shouldBe true
        }
    }
})
