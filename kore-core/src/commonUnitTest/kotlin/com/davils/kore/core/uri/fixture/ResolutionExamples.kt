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

package com.davils.kore.core.uri.fixture

private const val QUOTE: Char = '"'
private const val QUOTES_OF_AN_EXAMPLE: Int = 4

internal const val EXAMPLE_BASE: String = "http://a/b/c/d;p?q"

internal val normalExamples: List<Pair<String, String>> = examplesOf(
    """
        |"g:h"           =  "g:h"
        |"g"             =  "http://a/b/c/g"
        |"./g"           =  "http://a/b/c/g"
        |"g/"            =  "http://a/b/c/g/"
        |"/g"            =  "http://a/g"
        |"//g"           =  "http://g"
        |"?y"            =  "http://a/b/c/d;p?y"
        |"g?y"           =  "http://a/b/c/g?y"
        |"#s"            =  "http://a/b/c/d;p?q#s"
        |"g#s"           =  "http://a/b/c/g#s"
        |"g?y#s"         =  "http://a/b/c/g?y#s"
        |";x"            =  "http://a/b/c/;x"
        |"g;x"           =  "http://a/b/c/g;x"
        |"g;x?y#s"       =  "http://a/b/c/g;x?y#s"
        |""              =  "http://a/b/c/d;p?q"
        |"."             =  "http://a/b/c/"
        |"./"            =  "http://a/b/c/"
        |".."            =  "http://a/b/"
        |"../"           =  "http://a/b/"
        |"../g"          =  "http://a/b/g"
        |"../.."         =  "http://a/"
        |"../../"        =  "http://a/"
        |"../../g"       =  "http://a/g"
    """.trimMargin(),
)

internal val abnormalExamples: List<Pair<String, String>> = examplesOf(
    """
        |"../../../g"    =  "http://a/g"
        |"../../../../g" =  "http://a/g"
        |
        |"/./g"          =  "http://a/g"
        |"/../g"         =  "http://a/g"
        |"g."            =  "http://a/b/c/g."
        |".g"            =  "http://a/b/c/.g"
        |"g.."           =  "http://a/b/c/g.."
        |"..g"           =  "http://a/b/c/..g"
        |
        |"./../g"        =  "http://a/b/g"
        |"./g/."         =  "http://a/b/c/g/"
        |"g/./h"         =  "http://a/b/c/g/h"
        |"g/../h"        =  "http://a/b/c/h"
        |"g;x=1/./y"     =  "http://a/b/c/g;x=1/y"
        |"g;x=1/../y"    =  "http://a/b/c/y"
        |
        |"g?y/./x"       =  "http://a/b/c/g?y/./x"
        |"g?y/../x"      =  "http://a/b/c/g?y/../x"
        |"g#s/./x"       =  "http://a/b/c/g#s/./x"
        |"g#s/../x"      =  "http://a/b/c/g#s/../x"
        |
        |"http:g"        =  "http:g"         ; for strict parsers
        |                /  "http://a/b/c/g" ; for backward compatibility
    """.trimMargin(),
)

private fun examplesOf(table: String): List<Pair<String, String>> = table.lines().mapNotNull(::exampleOf)

private fun exampleOf(line: String): Pair<String, String>? {
    val quotes = line.indices.filter { at -> line[at] == QUOTE }
    if (quotes.size < QUOTES_OF_AN_EXAMPLE) return null
    val reference = line.substring(startIndex = quotes[0] + 1, endIndex = quotes[1])
    return reference to line.substring(startIndex = quotes[2] + 1, endIndex = quotes[3])
}
