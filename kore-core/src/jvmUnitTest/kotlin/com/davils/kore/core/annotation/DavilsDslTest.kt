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

class DavilsDslTest : FunSpec({
    context("dsl marker") {
        test("keeps the receivers of nested kore builders apart") {
            invisibleAnnotationsOf(DavilsDsl::class) shouldContain InvisibleAnnotation(descriptorOf(DslMarker::class))
        }
    }

    context("retention") {
        test("is kept in the binary for the compiler but not loaded at runtime") {
            retentionOf(DavilsDsl::class) shouldBe AnnotationRetention.BINARY
        }
    }

    context("targets") {
        test("marks builder classes and the receiver types of builder lambdas") {
            targetsOf(DavilsDsl::class) shouldBe setOf(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
        }
    }

    context("documentation") {
        test("is listed on every declaration it marks") {
            isDocumented(DavilsDsl::class) shouldBe true
        }
    }
})
