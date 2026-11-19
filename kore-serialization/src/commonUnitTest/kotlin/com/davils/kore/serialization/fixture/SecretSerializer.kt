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

package com.davils.kore.serialization.fixture

import com.davils.kore.serialization.TextSerializer

internal object SecretSerializer : TextSerializer<String>(
    serialName = "com.davils.kore.test.Secret",
    kind = "secret",
    maxLength = 8,
    isSensitive = true,
) {
    override fun read(text: String): String? = text.takeIf { candidate -> candidate.all(Char::isLetter) }

    override fun write(value: String): String = value
}
