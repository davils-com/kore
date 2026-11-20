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

package com.davils.kore.core.uri.fixture

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull

private const val MOST_SEGMENTS: Int = 8
private const val MOST_PIECES: Int = 3

private val schemes: List<String> = listOf("http", "HTTP", "urn", "g", "a+b.c-d", "File")
private val authorities: List<String> = listOf(
    "", "a", "Example.COM", "User@Example.COM:8080", "u:p@h", "[::1]", "[2001:DB8::7]", "[v7.X:y]", "127.0.0.1",
    "%41b", "%7e%2f", "h:",
)
private val segmentPieces: List<String> = listOf(
    "a", "B", ".", "..", "%41", "%7e", "%2F", "%c3%a4", "~", ";p", "=", "@", "x.y", "-", "_",
)
private val queryPieces: List<String> = listOf("", "a", "b=c", "/./", "?", "%7E", "%2f", "../x", "&")

private val segmentPieceLists: Arb<List<String>> = Arb.list(Arb.element(segmentPieces), 0..MOST_PIECES)

private val segments: Arb<String> = segmentPieceLists.map { pieces -> pieces.joinToString(separator = "") }

private val queryPieceLists: Arb<List<String>> = Arb.list(Arb.element(queryPieces), 0..MOST_PIECES)

private val presentQueries: Arb<String> = queryPieceLists.map { pieces -> pieces.joinToString(separator = "") }

private val queries: Arb<String?> = presentQueries.orNull()

private val optionalSchemes: Arb<String?> = Arb.element(schemes).orNull()

private val optionalAuthorities: Arb<String?> = Arb.element(authorities).orNull()

private val hierarchies: Arb<String> = Arb.bind(
    optionalAuthorities,
    Arb.boolean(),
    Arb.list(segments, 0..MOST_SEGMENTS),
) { authority, isRooted, pathSegments ->
    val path = pathSegments.joinToString(separator = "/")
    val authorityPart = authority?.let { part -> "//$part" }.orEmpty()
    val isPathRooted = isRooted || authority != null
    authorityPart + ("/$path".takeIf { isPathRooted } ?: path)
}

private val suffixes: Arb<String> = Arb.bind(genA = queries, genB = queries) { query, fragment ->
    query?.let { part -> "?$part" }.orEmpty() + fragment?.let { part -> "#$part" }.orEmpty()
}

private val composedReferences: Arb<String> = Arb.bind(
    genA = optionalSchemes,
    genB = hierarchies,
    genC = suffixes,
) { scheme, hierarchy, suffix ->
    scheme?.let { name -> "$name:" }.orEmpty() + hierarchy + suffix
}

internal val wellFormedReferences: Arb<String> =
    composedReferences.filter { text -> UriReference.parseOrNull(text) != null }

internal val wellFormedUris: Arb<String> =
    wellFormedReferences.filter { text -> UriReference.parseOrNull(text)?.isAbsolute == true }

internal val resolutionCases: Arb<Pair<String, String>> =
    Arb.bind(genA = wellFormedUris, genB = wellFormedReferences) { base, relative -> base to relative }

internal fun reference(text: String): UriReference = UriReference.parseOrNull(text).shouldNotBeNull()
