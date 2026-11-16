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

package com.davils.kore.core.arguments.lexer

import com.davils.kore.core.arguments.fixture.Assignment
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.Spelling
import com.davils.kore.core.arguments.fixture.Switches
import com.davils.kore.core.arguments.fixture.render
import com.davils.kore.core.arguments.lexer.fixture.Spellings
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val CASES: Int = 300
private const val LONGEST_WORD: Int = 12
private const val HIGHEST_PORT: Int = 65_535
private const val MOST_TAGS: Int = 3

private val words: Arb<String> = Arb.string(minSize = 1, maxSize = LONGEST_WORD, codepoints = Codepoint.alphanumeric())

private val assignments: Arb<Assignment> = arbitrary {
    Assignment(
        host = words.bind(),
        port = Arb.int(min = 0, max = HIGHEST_PORT).bind(),
        switches = Switches(
            isVerbose = Arb.boolean().bind(),
            isArchive = Arb.boolean().bind(),
            isColoured = Arb.boolean().bind(),
        ),
        tags = Arb.list(words, 0..MOST_TAGS).bind(),
        trailing = Arb.list(words, 0..2).bind(),
        spelling = Spelling(
            isShort = Arb.boolean().bind(),
            isAttached = Arb.boolean().bind(),
            isClustered = Arb.boolean().bind(),
        ),
    )
}

private fun describedFor(argv: List<String>): String {
    val parameters = Spellings()
    parameters.load(argv, NO_ENVIRONMENT)
    return parameters.describe()
}

class KoreParametersSyntaxPropertyTest : FunSpec({
    context("every legal spelling of an assignment") {
        test("resolves back to that assignment") {
            checkAll(CASES, assignments) { assignment ->
                val parameters = Spellings()
                val argv = assignment.render()

                parameters.load(argv, NO_ENVIRONMENT)

                withClue(argv.joinToString(" ")) {
                    parameters.host shouldBe assignment.host
                    parameters.port shouldBe assignment.port
                    parameters.isVerbose shouldBe assignment.switches.isVerbose
                    parameters.isArchive shouldBe assignment.switches.isArchive
                    parameters.isColoured shouldBe assignment.switches.isColoured
                    parameters.tags shouldBe assignment.tags
                    parameters.rest shouldBe assignment.trailing
                }
            }
        }

        test("treats an attached value and a separated value as the same argument") {
            checkAll(CASES, assignments) { assignment ->
                val attached = assignment.copy(spelling = assignment.spelling.copy(isAttached = true))
                val separated = assignment.copy(spelling = assignment.spelling.copy(isAttached = false))

                describedFor(attached.render()) shouldBe describedFor(separated.render())
            }
        }

        test("treats a long form and a short form as the same argument") {
            checkAll(CASES, assignments) { assignment ->
                val long = assignment.copy(spelling = assignment.spelling.copy(isShort = false))
                val short = assignment.copy(spelling = assignment.spelling.copy(isShort = true))

                describedFor(long.render()) shouldBe describedFor(short.render())
            }
        }
    }
})
