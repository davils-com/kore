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

package com.davils.kore.core.arguments

import com.davils.kore.core.arguments.fixture.Donor
import com.davils.kore.core.arguments.fixture.LateGroups
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.Recipient
import com.davils.kore.core.arguments.report.KoreParameterException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val LATE: String = "was declared after load() had already started"

class KoreParametersOwnershipTest : FunSpec({
    context("a parameter declared by one schema") {
        test("cannot be delegated to by another schema") {
            val failure = shouldThrow<IllegalArgumentException> { Recipient(Donor().donated()) }

            failure.message shouldContain "property 'taken' of Recipient delegates to a parameter of another schema"
        }
    }

    context("a parameter group declared after loading started") {
        test("is refused when it is an exclusive group") {
            val parameters = LateGroups()
            parameters.load(listOf("--quiet", "--loud"), NO_ENVIRONMENT)

            shouldThrow<IllegalStateException> { parameters.groupNow() }.message shouldBe "An exclusive group $LATE"
        }

        test("is refused when it is a dependency") {
            val parameters = LateGroups()
            parameters.load(emptyList(), NO_ENVIRONMENT)

            shouldThrow<IllegalStateException> { parameters.dependNow() }.message shouldContain LATE
        }

        test("declared before loading is still enforced") {
            val parameters = LateGroups()
            parameters.groupNow()

            shouldThrow<KoreParameterException> { parameters.load(listOf("--quiet", "--loud"), NO_ENVIRONMENT) }
        }
    }
})
