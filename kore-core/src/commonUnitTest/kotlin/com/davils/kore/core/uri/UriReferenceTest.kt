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

package com.davils.kore.core.uri

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import com.davils.kore.core.uri.fixture.reference
import com.davils.kore.core.uri.fixture.wellFormedReferences
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val CASES: Int = 1_000
private const val LONGEST_ARBITRARY_TEXT: Int = 40
private const val FULL: String = "http://a/b?q#f"
private const val PLAIN: String = "http://a/b"

private val examplesOfSection1: List<String> = listOf(
    "ftp://ftp.is.co.za/rfc/rfc1808.txt",
    "http://www.ietf.org/rfc/rfc2396.txt",
    "ldap://[2001:db8::7]/c=GB?objectClass?one",
    "mailto:John.Doe@example.com",
    "news:comp.infosystems.www.servers.unix",
    "tel:+1-816-555-1212",
    "telnet://192.0.2.16:80/",
    "urn:oasis:names:specification:docbook:dtd:xml:4.1.2",
)

private val wellFormedHosts: List<String> = listOf(
    "http://[::]/", "http://[::1]:80/", "http://[1:2:3:4:5:6:7:8]/", "http://[1:2:3:4:5:6:7::]/",
    "http://[::2:3:4:5:6:7:8]/", "http://[1:2:3:4:5:6:1.2.3.4]/", "http://[::ffff:192.0.2.128]/",
    "http://[1::5:6:255.255.255.255]/", "http://[v1.fe:80]/", "http://[V1A.x]/", "http://255.256.0.1/", "http://a:/",
    "http://:80/", "http://@/", "http://u:p:q@h/", "http://%41%2f/",
)

private val malformedReferences: List<String> = listOf(
    " ", "a b", "http://a b/", "ht tp:x", "1http:x", ":", ":a", "%", "%2", "%zz", "a%g0",
    "http://a:8x/", "http://a@b@c/", "http://[::1/", "http://[::1]x/", "http://[]/", "http://[1:2:3:4:5:6:7:8:9]/",
    "http://[1:2:3:4:5:6:7:8::]/", "http://[1::2::3]/", "http://[:::]/", "http://[::256.1.1.1]/",
    "http://[::01.1.1.1]/", "http://[::1.2.3]/", "http://[1.2.3.4::]/", "http://[12345::]/", "http://[:1::]/",
    "http://[v.x]/", "http://[vg.x]/", "http://[v1.]/", "http://[v1x]/", "http://a/b c", "http://a/?q#f#g",
    "http://a/\u00e4", "http://a/\n", "http://a/\u0000", "http://a/\\", "http://a/<b>", "+a:b", "a^:b", "http://a/{x}",
)

private fun textOrNull(text: String): String? = UriReference.parseOrNull(text)?.toString()

class UriReferenceTest : FunSpec({
    context("parseOrNull") {
        test("splits a reference into its components as Appendix B of RFC 3986 does") {
            val related = reference("http://www.ics.uci.edu/pub/ietf/uri/#Related")

            related.scheme shouldBe "http"
            related.authority shouldBe "www.ics.uci.edu"
            related.path shouldBe "/pub/ietf/uri/"
            related.query.shouldBeNull()
            related.fragment shouldBe "Related"
        }

        test("reads every example of section 1.1.2 of RFC 3986") {
            examplesOfSection1.forEach { text ->
                withClue(text) { reference(text).toString() shouldBe text }
            }
        }

        test("tells an empty component from a missing one") {
            val empty = reference("file://?#")
            val missing = reference("file:")

            empty.authority shouldBe ""
            empty.query shouldBe ""
            empty.fragment shouldBe ""
            missing.authority.shouldBeNull()
            missing.query.shouldBeNull()
            missing.fragment.shouldBeNull()
            missing.path shouldBe ""
        }

        test("reads the empty text as the empty relative reference") {
            val empty = reference("")

            empty.isAbsolute shouldBe false
            empty.path shouldBe ""
            empty.toString() shouldBe ""
        }

        test("reads relative references") {
            reference("//host/a").authority shouldBe "host"
            reference("../c.dtd").path shouldBe "../c.dtd"
            reference("./a:b").path shouldBe "./a:b"
            reference("?q").query shouldBe "q"
            reference("#f").fragment shouldBe "f"
        }

        test("reads every kind of host and port") {
            wellFormedHosts.forEach { text ->
                withClue(text) { reference(text).toString() shouldBe text }
            }
        }

        test("refuses text outside the grammar of RFC 3986") {
            malformedReferences.forEach { text ->
                withClue(text) { UriReference.parseOrNull(text).shouldBeNull() }
            }
        }

        test("reads text up to the longest URI reference and refuses longer text") {
            val longest = "a:" + "b".repeat(Limits.MAX_URI_REFERENCE_LENGTH - 2)

            reference(longest).toString() shouldBe longest
            UriReference.parseOrNull(longest + "b").shouldBeNull()
        }

        test("returns null or a reference that reads back to the same text for any text") {
            checkAll(CASES, Arb.string(0..LONGEST_ARBITRARY_TEXT)) { text ->
                textOrNull(text)?.shouldBe(text)
            }
            checkAll(CASES, wellFormedReferences) { text ->
                reference(text).toString() shouldBe text
            }
        }

        test("never gives text that is unsafe to show from hostile text") {
            checkAll(CASES, hostileText) { text ->
                textOrNull(text)?.shouldBeSafeToShow()
            }
        }
    }

    context("isAbsolute") {
        test("tells a URI from a relative reference") {
            reference("g:h").isAbsolute shouldBe true
            reference("http://a/b#f").isAbsolute shouldBe true
            reference("//a/b").isAbsolute shouldBe false
            reference("b").isAbsolute shouldBe false
        }
    }

    context("equals") {
        test("compares the components as text") {
            reference(FULL) shouldBe reference(FULL)
            reference(FULL).hashCode() shouldBe reference(FULL).hashCode()
            reference(PLAIN) shouldNotBe reference("HTTP://a/b")
            reference("$PLAIN?") shouldNotBe reference(PLAIN)
            reference("$PLAIN#") shouldNotBe reference(PLAIN)
            reference("http://a/%7e") shouldNotBe reference("http://a/~")
            reference("a").equals("a") shouldBe false
        }
    }

    context("normalize") {
        test("makes the example of section 6.2.2 of RFC 3986 equal") {
            val written = reference("example://a/b/c/%7Bfoo%7D")
            val varied = reference("eXAMPLE://a/./b/../b/%63/%7bfoo%7d")

            varied.normalize() shouldBe written.normalize()
            varied.normalize().toString() shouldBe "example://a/b/c/%7Bfoo%7D"
        }

        test("lowercases the scheme and the host but keeps the case of the rest") {
            val normalized = reference("HTTP://User@Example.COM:80/A?B#C").normalize()

            normalized.toString() shouldBe "http://User@example.com:80/A?B#C"
            reference("http://[2001:DB8::A]/").normalize().authority shouldBe "[2001:db8::a]"
            reference("http://[V1.AB]/").normalize().authority shouldBe "[v1.ab]"
        }

        test("decodes unreserved characters and uppercases the remaining percent-encodings") {
            val normalized = reference("http://%41%2f@%41%c3%a4/%7e%2d%2E%5f%2f?%61%3d#%7A%3f").normalize()

            normalized.toString() shouldBe "http://A%2F@a%C3%A4/~-._%2F?a%3D#z%3F"
        }

        test("removes dot segments where resolve would remove them") {
            reference("http://a/b/../c/./d").normalize().path shouldBe "/c/d"
            reference("/a/../../b").normalize().path shouldBe "/b"
            reference("urn:a/../b").normalize().path shouldBe "/b"
            reference("//a/./b").normalize().path shouldBe "/b"
            reference("a/%2E%2E/b").normalize().path shouldBe "a/../b"
            reference("../a").normalize().path shouldBe "../a"
        }

        test("keeps a path without an authority from reading back with one") {
            val normalized = reference("urn:a/..//b").normalize()

            normalized.toString() shouldBe "urn:/.//b"
            reference(normalized.toString()) shouldBe normalized
        }

        test("gives a reference that is no longer, reads back to itself and stays the same when normalized again") {
            checkAll(CASES, wellFormedReferences) { text ->
                val normalized = reference(text).normalize()

                (normalized.toString().length <= text.length) shouldBe true
                reference(normalized.toString()) shouldBe normalized
                normalized.normalize() shouldBe normalized
            }
        }
    }
})
