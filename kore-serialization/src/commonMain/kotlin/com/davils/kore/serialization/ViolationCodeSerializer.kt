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
import com.davils.kore.core.naming.NameRule
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * Serializes a [ViolationCode] as its text, such as `kore.validation.invalid`, and refuses a text that is no
 * dotted name of at least two parts.
 *
 * @since 2.1.0
 */
public object ViolationCodeSerializer : TextSerializer<ViolationCode>(
    serialName = "com.davils.kore.ViolationCode",
    kind = "violation code such as kore.validation.invalid",
    maxLength = Limits.MAX_NAME_LENGTH,
) {
    override fun read(text: String): ViolationCode? = NameRule.QUALIFIED.validOrNull(text)?.let(::ViolationCode)

    override fun write(value: ViolationCode): String = value.value
}
