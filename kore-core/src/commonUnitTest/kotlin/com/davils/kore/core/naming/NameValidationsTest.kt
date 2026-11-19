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

package com.davils.kore.core.naming

import com.davils.kore.core.validation.validate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

private const val SECRET_NAME: String = "Hunter2-Secret"
private const val VALID_NAME: String = "acme.billing"

class NameValidationsTest : FunSpec({
    context("a name inside a validation") {
        test("passes and returns a valid name") {
            val plugin = Plugin(VALID_NAME)

            plugin.validate("plugin").violations.shouldBeEmpty()
            plugin.checked shouldBe VALID_NAME
        }

        test("reports an invalid name at its property without quoting it") {
            val plugin = Plugin(SECRET_NAME)

            val violation = plugin.validate("plugin").violations.single()

            plugin.checked shouldBe null
            violation.path.toString() shouldBe "id"
            violation.code shouldBe NameViolationCodes.INVALID_NAME
            violation.message shouldBe "is not a plugin id such as acme.billing: it has a part that does not start " +
                "with a lower-case letter"
            violation.toString() shouldNotContain SECRET_NAME
        }
    }
})
