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
import com.davils.kore.core.uri.fixture.EXAMPLE_BASE
import com.davils.kore.core.uri.fixture.abnormalExamples
import com.davils.kore.core.uri.fixture.normalExamples
import com.davils.kore.core.uri.fixture.reference
import com.davils.kore.core.uri.fixture.resolutionCases
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

private const val CASES: Int = 1_000
private const val DOTTED_BASE: String = "urn:a/.."

private fun resolved(base: String, relative: String): String? {
    val target = reference(base).resolve(reference(relative))
    return target?.toString()
}

private fun resolvedFromNormal(base: String, relative: String): UriReference {
    val normalized = reference(base).normalize()
    return normalized.resolve(reference(relative)).shouldNotBeNull()
}

private fun targetOrNull(base: String, relative: String): UriReference? = reference(base).resolve(reference(relative))

private fun shouldResolveAll(examples: List<Pair<String, String>>) {
    examples.forEach { (relative, target) ->
        withClue(relative) { resolved(base = EXAMPLE_BASE, relative = relative) shouldBe target }
    }
}

private fun hasDotSegment(path: String): Boolean {
    val unguarded = path.removePrefix("/.").takeIf { path.startsWith("/.//") } ?: path
    return unguarded.split('/').any { segment -> segment == "." || segment == ".." }
}

class UriResolutionTest : FunSpec({
    context("the examples of RFC 3986") {
        test("resolve every normal example of section 5.4.1") {
            shouldResolveAll(normalExamples)
        }

        test("resolve every abnormal example of section 5.4.2 by the strict algorithm") {
            shouldResolveAll(abnormalExamples)
        }

        test("remove the dot segments of section 5.2.4") {
            removeDotSegments("/a/b/c/./../../g") shouldBe "/a/g"
            removeDotSegments("mid/content=5/../6") shouldBe "mid/6"
        }
    }

    context("resolve") {
        test("merges a relative path with the empty path of a base with an authority") {
            resolved(base = "http://a", relative = "g") shouldBe "http://a/g"
            resolved(base = "http://a?q#f", relative = "") shouldBe "http://a?q"
        }

        test("merges a relative path with a base without an authority") {
            resolved(base = "urn:a:b", relative = "c") shouldBe "urn:c"
            resolved(base = "file:/etc/xml/catalog", relative = "../dtd/x.dtd") shouldBe "file:/etc/dtd/x.dtd"
            resolved(base = "file:///etc/xml/catalog", relative = "docbook.xml") shouldBe "file:///etc/xml/docbook.xml"
        }

        test("keeps a target without an authority from reading back with one") {
            val target = targetOrNull(base = "urn:a", relative = "/..//b").shouldNotBeNull()

            target.path shouldBe "/.//b"
            target.toString() shouldBe "urn:/.//b"
            reference(target.toString()) shouldBe target
        }

        test("ignores the fragment of the base") {
            resolved(base = "http://a/b#f", relative = "c") shouldBe "http://a/c"
        }

        test("keeps the dot segments of the base for the empty reference, as section 5.2.2 does") {
            resolved(base = DOTTED_BASE, relative = "") shouldBe DOTTED_BASE
            resolved(base = DOTTED_BASE, relative = "#f") shouldBe "$DOTTED_BASE#f"
        }

        test("refuses a base without a scheme") {
            targetOrNull(base = "//a/b", relative = "c").shouldBeNull()
            targetOrNull(base = "b/c", relative = "g:h").shouldBeNull()
        }

        test("refuses a target longer than the longest URI reference") {
            val base = "http://a/" + "b/".repeat((Limits.MAX_URI_REFERENCE_LENGTH - 9) / 2)
            val longest = reference(base)

            longest.resolve(reference("")).shouldNotBeNull()
            longest.resolve(reference("gg")).shouldBeNull()
        }

        test("gives an absolute target without dot segments that reads back to itself") {
            checkAll(CASES, resolutionCases) { (base, relative) ->
                val target = resolvedFromNormal(base = base, relative = relative)

                target.isAbsolute shouldBe true
                hasDotSegment(target.path) shouldBe false
                reference(target.toString()) shouldBe target
            }
        }

        test("gives the same target for a reference and its own target") {
            checkAll(CASES, resolutionCases) { (base, relative) ->
                val origin = reference(base).normalize()
                val target = resolvedFromNormal(base = base, relative = relative)

                origin.resolve(target) shouldBe target
            }
        }
    }
})
