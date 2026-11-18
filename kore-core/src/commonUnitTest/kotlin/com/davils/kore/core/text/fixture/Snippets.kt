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

package com.davils.kore.core.text.fixture

import com.davils.kore.core.text.TextPosition
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.text.TextSnippet

internal const val SAMPLE_LINE: String = "port: eighty"
internal const val SAMPLE_VALUE_START: Int = 6
internal const val SAMPLE_VALUE_END: Int = 12

private val sampleHighlight: TextRange = TextRange(start = SAMPLE_VALUE_START, end = SAMPLE_VALUE_END)
private val samplePosition: TextPosition = TextPosition(line = 0, column = SAMPLE_VALUE_START)

internal fun snippetOf(
    line: String = SAMPLE_LINE,
    highlight: TextRange = sampleHighlight,
    position: TextPosition = samplePosition,
    isCutBefore: Boolean = false,
    isCutAfter: Boolean = false,
): TextSnippet = TextSnippet(
    position = position,
    line = line,
    highlight = highlight,
    isCutBefore = isCutBefore,
    isCutAfter = isCutAfter,
)
