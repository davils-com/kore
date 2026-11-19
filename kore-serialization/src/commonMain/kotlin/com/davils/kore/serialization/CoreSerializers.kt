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

import com.davils.kore.core.identity.Uuid
import com.davils.kore.core.identity.UuidV4
import com.davils.kore.core.identity.UuidV7
import com.davils.kore.core.size.DataSize
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.version.SemanticVersion
import com.davils.kore.core.version.SemanticVersionRange
import kotlinx.serialization.modules.SerializersModule
import kotlin.time.Duration

/**
 * A module that registers every serializer of this library as the contextual serializer of its type, for a format
 * that reads `@Contextual` properties, such as `Json { serializersModule = coreSerializers }`.
 *
 * It registers [DurationSerializer] for [Duration] as well, which replaces the ISO-8601 form of the built-in
 * serializer for the contextual properties of a format that uses this module; a property that names its serializer
 * keeps it.
 *
 * @since 2.1.0
 */
public val coreSerializers: SerializersModule = SerializersModule {
    contextual(Duration::class, DurationSerializer)
    contextual(DataSize::class, DataSizeSerializer)
    contextual(SemanticVersion::class, SemanticVersionSerializer)
    contextual(SemanticVersionRange::class, SemanticVersionRangeSerializer)
    contextual(Uuid::class, UuidSerializer)
    contextual(UuidV4::class, UuidV4Serializer)
    contextual(UuidV7::class, UuidV7Serializer)
    contextual(ViolationCode::class, ViolationCodeSerializer)
}
