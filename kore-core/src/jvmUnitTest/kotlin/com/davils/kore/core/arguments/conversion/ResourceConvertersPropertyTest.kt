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

package com.davils.kore.core.arguments.conversion

import com.davils.kore.core.arguments.conversion.fixture.Located
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.net.URI
import java.nio.file.Path

private const val CASES: Int = 300
private const val LONGEST_NAME: Int = 12

private val names: Arb<String> = Arb.string(minSize = 1, maxSize = LONGEST_NAME, codepoints = Codepoint.az())

private fun loaded(argument: String): Located {
    val parameters = Located()
    parameters.load(listOf(argument), NO_ENVIRONMENT)
    return parameters
}

class ResourceConvertersPropertyTest : FunSpec({
    context("a value survives being written down and converted back") {
        test("as a path") {
            checkAll(CASES, names) { name -> loaded("--directory=/tmp/$name").directory shouldBe Path.of("/tmp/$name") }
        }

        test("as a URI") {
            checkAll(CASES, names) { host ->
                loaded(
                    "--endpoint=https://$host.example.com/x",
                ).endpoint shouldBe URI.create("https://$host.example.com/x")
            }
        }
    }
})
