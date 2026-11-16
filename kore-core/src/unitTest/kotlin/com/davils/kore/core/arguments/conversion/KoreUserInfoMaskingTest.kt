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

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.net.URI

private const val CASES: Int = 300
private const val SHORTEST_SECRET: Int = 12
private const val LONGEST_SECRET: Int = 32
private const val MOST_SEPARATORS: Int = 4
private const val MOST_LEADING: Int = 2
private const val SECRET: String = "hunter2-placeholder"

private val secrets: Arb<String> = Arb.string(
    minSize = SHORTEST_SECRET,
    maxSize = LONGEST_SECRET,
    codepoints = Codepoint.alphanumeric(),
)
private val schemes: Arb<String> = Arb.element("https", "HTTP", "ws", "wss", "ftp", "postgres", "x-custom+v1.2")
private val separators: Arb<String> = joined(Arb.element("/", "\\", "\t", "\n", "\r"), MOST_SEPARATORS)
private val leading: Arb<String> = joined(Arb.element(" ", "\t", "\u0000", "\u001f"), MOST_LEADING)

private val prefixes: Arb<String> = Arb.bind(
    genA = leading,
    genB = schemes,
    genC = separators,
) { space, scheme, separator ->
    "$space$scheme:$separator"
}

private fun joined(pieces: Arb<String>, most: Int): Arb<String> {
    val lists = Arb.list(pieces, 0..most)
    return lists.map { chosen -> chosen.joinToString(separator = "") }
}

class KoreUserInfoMaskingTest : FunSpec({
    context("user information in a text") {
        test("is masked whatever number of slashes or backslashes follows the scheme") {
            checkAll(iterations = CASES, genA = prefixes, genB = secrets) { prefix, secret ->
                val text = "${prefix}user:$secret@evil.example/x"

                val masked = maskedUserInfo(text)

                withClue(text) {
                    masked shouldNotContain secret
                    masked shouldBe text.substringBefore("user:") + "****@evil.example/x"
                }
            }
        }

        test("is masked after two leading slashes or backslashes") {
            listOf("//", "\\\\", "/\\", "\\/", " //", "/\t/").forEach { prefix ->
                maskedUserInfo("${prefix}user:$SECRET@evil.example/x") shouldBe "$prefix****@evil.example/x"
            }
        }

        test("is masked after a scheme separator anywhere in the text") {
            maskedUserInfo("see https://user:$SECRET@evil.example/x") shouldBe "see https://****@evil.example/x"
        }

        test("is left alone where no scheme or authority precedes it") {
            val plain = listOf("alice@example.com", "/user@host", "1:user@host", "C:\\temp", "https://example.com/")

            plain.forEach { text -> maskedUserInfo(text) shouldBe text }
        }
    }

    context("a URI with an authority") {
        test("is masked before the last at sign of its authority only") {
            maskedUserInfo(URI("https://user:$SECRET@host/a@b?q=1#f")) shouldBe "https://****@host/a@b?q=1#f"
            maskedUserInfo(URI("//user:$SECRET@host/x")) shouldBe "//****@host/x"
        }

        test("without user information is shown as it is") {
            maskedUserInfo(URI("https://host/x?q=1")) shouldBe "https://host/x?q=1"
        }
    }

    context("a URI without an authority") {
        test("is masked like a text") {
            maskedUserInfo(URI("https:/user:$SECRET@evil.example/x")) shouldBe "https:/****@evil.example/x"
        }
    }

    context("an opaque URI") {
        test("is masked like a text") {
            maskedUserInfo(URI("mailto:team@example.com")) shouldBe "mailto:****@example.com"
        }

        test("carries user information only after a scheme separator") {
            hasUserInfo(URI("mailto:team@example.com")) shouldBe false
            hasUserInfo(URI("jar:https://user:$SECRET@example.com/a.jar!/b")) shouldBe true
            hasUserInfo(URI("urn:example:a@b://c")) shouldBe false
        }
    }
})
