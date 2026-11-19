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
import com.davils.kore.core.identity.UuidV4

/**
 * Serializes a [UuidV4] as its canonical text and refuses a UUID of another version when it reads.
 *
 * @since 2.1.0
 */
public object UuidV4Serializer : TextSerializer<UuidV4>(
    serialName = "com.davils.kore.UuidV4",
    kind = "version 4 UUID",
    maxLength = Limits.MAX_UUID_TEXT_LENGTH,
) {
    override fun read(text: String): UuidV4? = UuidV4.parseOrNull(text)

    override fun write(value: UuidV4): String = value.toString()
}
