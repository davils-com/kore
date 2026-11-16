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

package com.davils.kore.core.arguments.fixture

private const val TERMINATOR: String = "--"

internal fun Assignment.render(): List<String> {
    val isClustered = isClustered()
    val argv = mutableListOf<String>()
    argv += renderFlags(isClustered)
    argv += named(name = form(short = "-h", long = "--host"), value = host)
    if (!isClustered) argv += named(name = form(short = "-p", long = "--port"), value = port.toString())
    tags.forEach { tag -> argv += named(name = form(short = "-t", long = "--tag"), value = tag) }
    if (trailing.isNotEmpty()) argv += listOf(TERMINATOR) + trailing
    return argv
}

private fun Assignment.isClustered(): Boolean {
    val isShortCluster = spelling.isClustered && spelling.isShort
    return isShortCluster && switches.isVerbose && switches.isArchive
}

private fun Assignment.renderFlags(isClustered: Boolean): List<String> {
    if (isClustered) return listOf("-vap", port.toString(), colourFlag())
    val verbose = form(short = "-v", long = "--verbose").takeIf { switches.isVerbose }
    val archive = form(short = "-a", long = "--archive").takeIf { switches.isArchive }
    return listOfNotNull(verbose, archive, colourFlag())
}

private fun Assignment.colourFlag(): String = "--colour".takeIf { switches.isColoured } ?: "--no-colour"

private fun Assignment.form(short: String, long: String): String = short.takeIf { spelling.isShort } ?: long

private fun Assignment.named(name: String, value: String): List<String> {
    if (spelling.isAttached) return listOf("$name=$value")
    return listOf(name, value)
}
