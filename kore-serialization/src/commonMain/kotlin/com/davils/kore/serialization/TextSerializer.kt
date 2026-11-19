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

package com.davils.kore.serialization

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializer that writes a value as one string and reads it back strictly, for values that have a text form, such
 * as identifiers, versions and sizes.
 *
 * A subclass names how a value is read with [read] and written with [write]. Reading refuses a text longer than
 * [maxLength] UTF-16 code units before [read] sees it, and a text that [read] does not accept, with a
 * [SerializationException] whose message is constant text plus the text that was read, escaped and shortened, so a
 * hostile document cannot forge lines of a log. When [isSensitive] is `true`, the message shows `****` instead of the
 * text. The limit is checked once the format has decoded the string, so it bounds the work of [read], not the memory
 * the format spent; a format such as `Json` may also put parts of its raw input into its own exceptions, which no
 * serializer controls.
 *
 * Its descriptor is a primitive string with the [serialName] the subclass gives, which must not be the name of a
 * built-in serializer, such as `kotlin.time.Duration`.
 *
 * @param T the type of the values.
 * @param serialName the name of the descriptor, such as `com.acme.PluginId`.
 * @param kind what a valid text is, shown in messages, such as `plugin id such as acme.billing`.
 * @param maxLength the longest text, in UTF-16 code units, that is read.
 * @param isSensitive whether the text is a secret that no message may show.
 * @since 2.1.0
 */
public abstract class TextSerializer<T : Any>(
    serialName: String,
    private val kind: String,
    private val maxLength: Int,
    private val isSensitive: Boolean = false,
) : KSerializer<T> {
    /**
     * A primitive string descriptor with the serial name this serializer was created with.
     *
     * @since 2.1.0
     */
    final override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    /**
     * Reads a value from [text], which is at most `maxLength` UTF-16 code units long.
     *
     * @param text the text to read; it may come from an attacker.
     * @return the value, or `null` if [text] is no valid text of a value.
     * @since 2.1.0
     */
    protected abstract fun read(text: String): T?

    /**
     * Writes [value] as text that [read] reads back to an equal value.
     *
     * @param value the value to write.
     * @return the text of [value].
     * @throws SerializationException if [value] has no text form.
     * @since 2.1.0
     */
    protected abstract fun write(value: T): String

    /**
     * Encodes [value] as the string [write] gives.
     *
     * @param encoder the encoder of the format.
     * @param value the value to encode.
     * @since 2.1.0
     */
    final override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(write(value))
    }

    /**
     * Decodes a string and reads it with [read].
     *
     * @param decoder the decoder of the format.
     * @return the value read.
     * @throws SerializationException if the string is longer than the limit or [read] does not accept it.
     * @since 2.1.0
     */
    final override fun deserialize(decoder: Decoder): T {
        val text = decoder.decodeString()
        val value = text.takeIf { candidate -> candidate.length <= maxLength }?.let(::read)
        return value ?: throw SerializationException(refusalOf(text))
    }

    private fun refusalOf(text: String): String {
        val shown = SafeText.REDACTED.takeIf { isSensitive } ?: SafeText.quoted(text)
        return "$shown is not a ${SafeText.text(kind)}"
    }
}
