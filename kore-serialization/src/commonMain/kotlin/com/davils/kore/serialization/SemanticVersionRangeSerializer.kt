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

package com.davils.kore.serialization

import com.davils.kore.core.Limits
import com.davils.kore.core.version.SemanticVersionRange

/**
 * Serializes a [SemanticVersionRange] as its canonical text and reads every form `SemanticVersionRange.parse` reads.
 *
 * @since 2.1.0
 */
public object SemanticVersionRangeSerializer : TextSerializer<SemanticVersionRange>(
    serialName = "com.davils.kore.SemanticVersionRange",
    kind = "version range such as >=2.0.0 <3.0.0",
    maxLength = Limits.MAX_RANGE_LENGTH,
) {
    override fun read(text: String): SemanticVersionRange? = SemanticVersionRange.parseOrNull(text)

    override fun write(value: SemanticVersionRange): String = value.toString()
}
