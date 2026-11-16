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
 * This number of bytes as a [KoreDataSize].
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Int.bytes: KoreDataSize get() = KoreDataSize(toLong())

/**
 * This number of bytes as a [KoreDataSize].
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Long.bytes: KoreDataSize get() = KoreDataSize(this)

/**
 * This number of kibibytes, each [KoreDataSize.KIBIBYTE] bytes, as a [KoreDataSize].
 *
 * Every non-negative [Int] fits, so the conversion never overflows.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Int.kibibytes: KoreDataSize get() = KoreDataSize(toLong() * KoreDataSize.KIBIBYTE)

/**
 * This number of mebibytes, each [KoreDataSize.MEBIBYTE] bytes, as a [KoreDataSize].
 *
 * Every non-negative [Int] fits, so the conversion never overflows.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Int.mebibytes: KoreDataSize get() = KoreDataSize(toLong() * KoreDataSize.MEBIBYTE)

/**
 * This number of gibibytes, each [KoreDataSize.GIBIBYTE] bytes, as a [KoreDataSize].
 *
 * Every non-negative [Int] fits, so the conversion never overflows.
 *
 * @throws IllegalArgumentException if the number is negative.
 * @since 1.0.0
 */
public val Int.gibibytes: KoreDataSize get() = KoreDataSize(toLong() * KoreDataSize.GIBIBYTE)
