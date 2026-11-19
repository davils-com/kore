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
import kotlinx.serialization.SerializationException
import kotlin.time.Duration

private const val FIRST_PRINTABLE: Char = ' '
private const val LAST_PRINTABLE: Char = '~'

/**
 * Serializes a [Duration] as the text Kotlin writes for it, such as `45s` or `1h 30m`.
 *
 * The built-in serializer of kotlinx-serialization writes ISO-8601, such as `PT1H30M`, which few people read at a
 * glance in a configuration file. This one writes what `Duration.toString` writes and reads what `Duration.parse`
 * reads, the ISO form included, the same text that the duration converter of Kore's command line reads. A text must
 * be printable ASCII and at most [Limits.MAX_NUMERIC_TEXT_LENGTH] characters long. An infinite duration is refused
 * both ways, because a configuration that waits forever is a mistake more often than an intention; a negative one is
 * read as it is, and whether it is valid is for a validation to decide.
 *
 * @since 2.1.0
 */
public object DurationSerializer : TextSerializer<Duration>(
    serialName = "com.davils.kore.Duration",
    kind = "finite duration such as 45s",
    maxLength = Limits.MAX_NUMERIC_TEXT_LENGTH,
) {
    override fun read(text: String): Duration? {
        val isPrintable = text.all { character -> character in FIRST_PRINTABLE..LAST_PRINTABLE }
        if (!isPrintable) return null
        return Duration.parseOrNull(text)?.takeIf { duration -> duration.isFinite() }
    }

    override fun write(value: Duration): String {
        if (!value.isFinite()) throw SerializationException("An infinite duration cannot be written")
        return value.toString()
    }
}
