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

package com.davils.kore.core.naming

/**
 * What the first character of a name, and of every part of a name with parts, may be.
 *
 * @since 2.1.0
 */
public enum class NameStart {
    /**
     * A lower-case ASCII letter, `a` to `z`.
     *
     * @since 2.1.0
     */
    Letter,

    /**
     * A lower-case ASCII letter or an ASCII digit, `a` to `z` or `0` to `9`.
     *
     * @since 2.1.0
     */
    LetterOrDigit,
}
