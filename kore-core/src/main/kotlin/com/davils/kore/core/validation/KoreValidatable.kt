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

package com.davils.kore.core.validation

/**
 * A value that states its own rules and reports every problem it finds, instead of stopping at the first one.
 *
 * Implement [validate] to check the value; check an instance with the extension functions
 * [validate][com.davils.kore.core.validation.validate], which returns a
 * [KoreValidationReport][com.davils.kore.core.validation.report.KoreValidationReport], and [validateOrThrow], which
 * throws when the value has errors. Kore runs the rules once per call, on the calling thread, so the value must not
 * change while it is validated.
 *
 * @since 1.0.0
 */
public interface KoreValidatable {
    /**
     * Checks this value and reports every problem to the receiving [KoreValidationScope].
     *
     * Report problems with [KoreValidationScope.fail] and [KoreValidationScope.warn] and descend into nested values
     * with [KoreValidationScope.within] and [KoreValidationScope.each]. A nested value that is itself a
     * [KoreValidatable] is validated in place with `with(nested) { validate() }` inside such a block. Code that only
     * checks a value uses the extension functions named in the description of [KoreValidatable] instead, which create
     * the scope. The scope is valid only while this function runs and must not be kept or handed to another thread.
     *
     * An exception thrown here is not caught: it ends the validation and reaches the caller of the extension function
     * unchanged. A message must not quote a secret, because messages are escaped but never masked wherever they are
     * shown.
     *
     * @since 1.0.0
     */
    public fun KoreValidationScope.validate()
}
