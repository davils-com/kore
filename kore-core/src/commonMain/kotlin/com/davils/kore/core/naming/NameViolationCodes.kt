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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The violation codes of names checked inside a validation.
 *
 * @since 2.1.0
 */
public object NameViolationCodes {
    /**
     * A text does not follow the [NameRule] it was checked against.
     *
     * @since 2.1.0
     */
    public val INVALID_NAME: ViolationCode = ViolationCode("kore.naming.invalid-name")
}
