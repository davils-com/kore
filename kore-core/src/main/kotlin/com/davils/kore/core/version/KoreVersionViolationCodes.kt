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

import com.davils.kore.core.validation.violation.KoreViolationCode

/**
 * The codes of the violations that the validations of versions and ranges report.
 *
 * The values of the codes are stable and may be stored, compared and matched by consumers.
 *
 * @since 1.0.0
 */
public object KoreVersionViolationCodes {
    /**
     * The code of a text that is not a semantic version, reported by [semanticVersion]; its value is
     * `kore.version.invalid-version`.
     *
     * @since 1.0.0
     */
    public val INVALID_VERSION: KoreViolationCode = KoreViolationCode("kore.version.invalid-version")

    /**
     * The code of a text that is not a range of semantic versions, reported by [semanticVersionRange]; its value is
     * `kore.version.invalid-range`.
     *
     * @since 1.0.0
     */
    public val INVALID_RANGE: KoreViolationCode = KoreViolationCode("kore.version.invalid-range")
}
