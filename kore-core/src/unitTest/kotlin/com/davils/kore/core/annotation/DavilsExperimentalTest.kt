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
import com.davils.kore.core.annotation.fixture.REQUIRES_OPT_IN_DESCRIPTOR
import com.davils.kore.core.annotation.fixture.invisibleAnnotationsOf
import com.davils.kore.core.annotation.fixture.isDocumented
import com.davils.kore.core.annotation.fixture.retentionOf
import com.davils.kore.core.annotation.fixture.targetsOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class DavilsExperimentalTest : FunSpec({
    context("opt-in") {
        test("warns every caller that does not opt in without failing the build") {
            val requiresOptIn = InvisibleAnnotation(REQUIRES_OPT_IN_DESCRIPTOR, mapOf("level" to "WARNING"))

            invisibleAnnotationsOf(DavilsExperimental::class) shouldContain requiresOptIn
        }
    }

    context("retention") {
        test("is kept in the binary for the compiler but not loaded at runtime") {
            retentionOf(DavilsExperimental::class) shouldBe AnnotationRetention.BINARY
        }
    }

    context("targets") {
        test("marks every kind of declaration that can be part of a public API") {
            targetsOf(DavilsExperimental::class) shouldBe setOf(
                AnnotationTarget.CLASS,
                AnnotationTarget.ANNOTATION_CLASS,
                AnnotationTarget.CONSTRUCTOR,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.PROPERTY,
                AnnotationTarget.PROPERTY_GETTER,
                AnnotationTarget.PROPERTY_SETTER,
                AnnotationTarget.TYPEALIAS,
            )
        }
    }

    context("documentation") {
        test("is listed on every declaration it marks") {
            isDocumented(DavilsExperimental::class) shouldBe true
        }
    }
})
