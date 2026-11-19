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

package com.davils.kore.core.size

/**
 * This number of bytes as a [DataSize].
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Int.bytes: DataSize get() = DataSize(toLong())

/**
 * This number of bytes as a [DataSize].
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Long.bytes: DataSize get() = DataSize(this)

/**
 * This number of kibibytes, each [DataSize.KIBIBYTE] bytes, as a [DataSize].
 *
 * Every non-negative [Int] fits, so the conversion never overflows.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Int.kibibytes: DataSize get() = DataSize(toLong() * DataSize.KIBIBYTE)

/**
 * This number of mebibytes, each [DataSize.MEBIBYTE] bytes, as a [DataSize].
 *
 * Every non-negative [Int] fits, so the conversion never overflows.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Int.mebibytes: DataSize get() = DataSize(toLong() * DataSize.MEBIBYTE)

/**
 * This number of gibibytes, each [DataSize.GIBIBYTE] bytes, as a [DataSize].
 *
 * Every non-negative [Int] fits, so the conversion never overflows.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Int.gibibytes: DataSize get() = DataSize(toLong() * DataSize.GIBIBYTE)

/**
 * This number of tebibytes, each [DataSize.TEBIBYTE] bytes, as a [DataSize].
 *
 * @throws IllegalArgumentException if the number is negative.
 * @throws ArithmeticException if the size exceeds [Long.MAX_VALUE] bytes.
 * @since 2.1.0
 */
public val Int.tebibytes: DataSize get() = toLong().tebibytes

/**
 * This number of kibibytes, each [DataSize.KIBIBYTE] bytes, as a [DataSize], exactly as [DataSize.times] multiplies.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @throws ArithmeticException if the size exceeds [Long.MAX_VALUE] bytes.
 * @since 2.1.0
 */
public val Long.kibibytes: DataSize get() = DataSize(this) * DataSize.KIBIBYTE

/**
 * This number of mebibytes, each [DataSize.MEBIBYTE] bytes, as a [DataSize], exactly as [DataSize.times] multiplies.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @throws ArithmeticException if the size exceeds [Long.MAX_VALUE] bytes.
 * @since 2.1.0
 */
public val Long.mebibytes: DataSize get() = DataSize(this) * DataSize.MEBIBYTE

/**
 * This number of gibibytes, each [DataSize.GIBIBYTE] bytes, as a [DataSize], exactly as [DataSize.times] multiplies.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @throws ArithmeticException if the size exceeds [Long.MAX_VALUE] bytes.
 * @since 2.1.0
 */
public val Long.gibibytes: DataSize get() = DataSize(this) * DataSize.GIBIBYTE

/**
 * This number of tebibytes, each [DataSize.TEBIBYTE] bytes, as a [DataSize], exactly as [DataSize.times] multiplies.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @throws ArithmeticException if the size exceeds [Long.MAX_VALUE] bytes.
 * @since 2.1.0
 */
public val Long.tebibytes: DataSize get() = DataSize(this) * DataSize.TEBIBYTE
