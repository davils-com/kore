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

import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty

/**
 * Checks [text] against [rule] inside a validation, reporting a violation at [property] instead of throwing.
 *
 * A text that does not follow the rule is reported as an error with the code [NameViolationCodes.INVALID_NAME] and a
 * message such as `is not a plugin id such as acme.billing: it has fewer than 2 parts`. The message names the problem
 * in fixed words and never quotes [text], so a hostile text cannot place anything in a report.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to check; it may come from an attacker.
 * @param rule the rule the text must follow.
 * @param kind what the name names, such as `plugin id`.
 * @return [text], or `null` if it does not follow [rule] and a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.name(property: KProperty<*>, text: String, rule: NameRule, kind: String): String? {
    val problem = rule.problemOf(text) ?: return text
    return reject(
        property = property,
        message = "is not a $kind such as ${rule.example}: it ${rule.reasonFor(problem)}",
        code = NameViolationCodes.INVALID_NAME,
    )
}
