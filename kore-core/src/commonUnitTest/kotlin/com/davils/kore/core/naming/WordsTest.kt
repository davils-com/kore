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

@file:OptIn(DavilsExperimental::class)

package com.davils.kore.core.naming

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsExperimental
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.pattern
import io.kotest.property.checkAll

private const val MOST_WORDS: Int = 5
private const val RETRY: String = "maxRetryCount"

private val lowerWords: Arb<List<String>> = Arb.list(Arb.pattern("[a-z]{2,7}"), 1..MOST_WORDS)

class WordsTest : FunSpec({
    context("words") {
        test("splits at separators, case changes and the end of acronyms") {
            words(RETRY) shouldContainExactly listOf("max", "retry", "count")
            words("HTTPServerError2") shouldContainExactly listOf("http", "server", "error2")
            words("parse_HTTP-response.v2 now") shouldContainExactly listOf("parse", "http", "response", "v2", "now")
            words("version2Beta") shouldContainExactly listOf("version2", "beta")
            words("ABC") shouldContainExactly listOf("abc")
        }

        test("drops empty words and keeps characters outside ASCII inside their word") {
            words("--a__b--") shouldContainExactly listOf("a", "b")
            words("größeWert") shouldContainExactly listOf("größe", "wert")
            words("").shouldBeEmpty()
        }

        test("refuses a text beyond the longest splittable") {
            shouldThrow<IllegalArgumentException> { words("a".repeat(Limits.MAX_WORDS_TEXT_LENGTH + 1)) }
        }
    }

    context("toCase") {
        test("writes a name in every style") {
            RETRY.toCase(CaseStyle.Camel) shouldBe RETRY
            RETRY.toCase(CaseStyle.Pascal) shouldBe "MaxRetryCount"
            RETRY.toCase(CaseStyle.Kebab) shouldBe "max-retry-count"
            RETRY.toCase(CaseStyle.Snake) shouldBe "max_retry_count"
            RETRY.toCase(CaseStyle.ScreamingSnake) shouldBe "MAX_RETRY_COUNT"
            RETRY.toCase(CaseStyle.Words) shouldBe "max retry count"
        }

        test("reads any style back into any other") {
            checkAll(lowerWords) { parts ->
                val kebab = parts.joinToString(separator = "-")

                kebab.toCase(CaseStyle.Camel).toCase(CaseStyle.Kebab) shouldBe kebab
                kebab.toCase(CaseStyle.ScreamingSnake).toCase(CaseStyle.Kebab) shouldBe kebab
            }
        }

        test("writes nothing for a text without words") {
            "--".toCase(CaseStyle.Camel) shouldBe ""
        }
    }
})
