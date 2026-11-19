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
import com.davils.kore.core.version.SemanticVersion

/**
 * Serializes a [SemanticVersion] as its text, such as `2.1.0-rc.1`, and reads it as `SemanticVersion.parse` does.
 *
 * @since 2.1.0
 */
public object SemanticVersionSerializer : TextSerializer<SemanticVersion>(
    serialName = "com.davils.kore.SemanticVersion",
    kind = "semantic version such as 2.1.0",
    maxLength = Limits.MAX_VERSION_LENGTH,
) {
    override fun read(text: String): SemanticVersion? = SemanticVersion.parseOrNull(text)

    override fun write(value: SemanticVersion): String = value.toString()
}
