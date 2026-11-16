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

package com.davils.kore.core.arguments.declaration

import com.davils.kore.core.arguments.declaration.fixture.Named
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.text.fixture.ESCAPE
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll

private const val CASES: Int = 200

private fun refusedName(name: String): String =
    withClue(name) { shouldThrow<IllegalArgumentException> { Named(name) }.message.orEmpty() }

class KoreParameterNameGrammarTest : FunSpec({
    context("a long name") {
        test("that could never be matched on the command line is refused when declared") {
            val unmatchable = listOf("--a=b", "--with space", "--tab\there", "--$ESCAPE[2J", "--über", "---x", "--_x")

            unmatchable.forEach { name -> refusedName(name) shouldContain "Long parameter name" }
        }

        test("of ASCII letters, digits, dashes and underscores is accepted") {
            listOf("--data_dir", "--Port2", "--x", "--9lives").forEach { name ->
                val parameters = Named(name)

                parameters.load(listOf("$name=value"), NO_ENVIRONMENT)

                parameters.value shouldBe "value"
            }
        }
    }

    context("a short name") {
        test("that is not an ASCII letter or digit is refused when declared") {
            listOf("-=", "- ", "-é", "-$ESCAPE", "-١").forEach { name ->
                refusedName(name) shouldContain "Short parameter name"
            }
        }
    }

    context("a refused name") {
        test("is shown safely in the message that refuses it") {
            checkAll(CASES, hostileText) { hostile ->
                refusedName("--a$hostile\u0000").shouldBeSafeToShow()
                refusedName("-$hostile\u0000").shouldBeSafeToShow()
            }
        }
    }
})
