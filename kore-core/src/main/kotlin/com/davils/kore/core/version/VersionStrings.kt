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

package com.davils.kore.core.version

/**
 * Reads this text as a semantic version, as [SemanticVersion.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the version.
 * @throws IllegalArgumentException if this text is not a semantic version. The message never quotes the text.
 * @since 1.0.0
 */
public fun String.toSemanticVersion(): SemanticVersion = SemanticVersion.parse(this)

/**
 * Reads this text as a semantic version, as [SemanticVersion.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the version, or `null` if this text is not a semantic version.
 * @since 1.0.0
 */
public fun String.toSemanticVersionOrNull(): SemanticVersion? = SemanticVersion.parseOrNull(this)

/**
 * Reads this text as a range of semantic versions, as [SemanticVersionRange.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the range.
 * @throws IllegalArgumentException if this text is not a range. The message never quotes the text.
 * @since 1.0.0
 */
public fun String.toSemanticVersionRange(): SemanticVersionRange = SemanticVersionRange.parse(this)

/**
 * Reads this text as a range of semantic versions, as [SemanticVersionRange.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the range, or `null` if this text is not a range.
 * @since 1.0.0
 */
public fun String.toSemanticVersionRangeOrNull(): SemanticVersionRange? = SemanticVersionRange.parseOrNull(this)
