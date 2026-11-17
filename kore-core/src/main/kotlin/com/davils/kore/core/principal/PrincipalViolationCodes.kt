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

package com.davils.kore.core.principal

import com.davils.kore.core.limit.Limits
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes that the validation of a [Principal] and of a [PrincipalBuilder] reports.
 *
 * Every code is a stable name in the `kore.principal` namespace, so a client can react to it without parsing the
 * message of a violation. The message of such a violation never contains the text it refuses; a refused role or the
 * name of an attribute with a refused value appears only in the path of the violation, escaped wherever it is shown.
 *
 * @since 1.0.0
 */
public object PrincipalViolationCodes {
    /**
     * The id is empty or consists only of whitespace and the braille pattern blank U+2800; reported under `id`.
     *
     * @since 1.0.0
     */
    public val BLANK_ID: ViolationCode = ViolationCode("kore.principal.blank-id")

    /**
     * A role is empty or consists only of whitespace and the braille pattern blank U+2800; reported under
     * `roles["<role>"]`.
     *
     * @since 1.0.0
     */
    public val BLANK_ROLE: ViolationCode = ViolationCode("kore.principal.blank-role")

    /**
     * The tenant is set but empty or consists only of whitespace and the braille pattern blank U+2800; reported
     * under `tenant`.
     *
     * @since 1.0.0
     */
    public val BLANK_TENANT: ViolationCode = ViolationCode("kore.principal.blank-tenant")

    /**
     * The display name is set but empty or consists only of whitespace and the braille pattern blank U+2800;
     * reported under `displayName`.
     *
     * @since 1.0.0
     */
    public val BLANK_DISPLAY_NAME: ViolationCode = ViolationCode("kore.principal.blank-display-name")

    /**
     * The name of an attribute given to [PrincipalBuilder.attribute] is empty or consists only of whitespace and
     * the braille pattern blank U+2800; reported under `attributes`.
     *
     * @since 1.0.0
     */
    public val INVALID_ATTRIBUTE: ViolationCode = ViolationCode("kore.principal.invalid-attribute")

    /**
     * A text is longer than its limit, counted in UTF-16 code units; reported under the field of the text.
     *
     * The limits are [Limits.MAX_PRINCIPAL_ID_LENGTH] for the id, [Limits.MAX_PRINCIPAL_TEXT_LENGTH] for a
     * role, the tenant and the display name, [Limits.MAX_ATTRIBUTE_NAME_LENGTH] for the name of an attribute and
     * [Limits.MAX_ATTRIBUTE_VALUE_LENGTH] for its value.
     *
     * @since 1.0.0
     */
    public val TOO_LONG: ViolationCode = ViolationCode("kore.principal.too-long")

    /**
     * A text contains a character that is unsafe to show; reported under the field of the text.
     *
     * Unsafe are control, format, line separator, paragraph separator, private-use, unassigned and lone surrogate
     * characters, variation selectors, the Mongolian free variation selectors included, and every other character
     * that Unicode marks as default ignorable because it shows as nothing, such as a tab, a line break, a
     * bidirectional override, a zero-width joiner, a Hangul filler or the combining grapheme joiner.
     *
     * @since 1.0.0
     */
    public val INVALID_CHARACTER: ViolationCode = ViolationCode("kore.principal.invalid-character")

    /**
     * The id, a role or the tenant starts or ends with whitespace, no-break and ideographic spaces and the braille
     * pattern blank U+2800 included; reported under the field of the text.
     *
     * @since 1.0.0
     */
    public val SURROUNDING_WHITESPACE: ViolationCode = ViolationCode("kore.principal.surrounding-whitespace")

    /**
     * A principal was given more than [Limits.MAX_ROLES] roles; reported once under `roles`, and the roles
     * themselves are then not checked.
     *
     * Where roles are given as names, to [principal] or to one call of [PrincipalBuilder.roles], more than
     * [Limits.MAX_ROLES] names are refused before any of them is read, even when some of them repeat.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_ROLES: ViolationCode = ViolationCode("kore.principal.too-many-roles")

    /**
     * A [PrincipalBuilder] was given more than [Limits.MAX_ATTRIBUTES] attributes; reported once under
     * `attributes`.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_ATTRIBUTES: ViolationCode = ViolationCode("kore.principal.too-many-attributes")

    /**
     * A principal acts on behalf of itself, that is, of a principal with the same id, kind and tenant; reported under
     * `actingFor`.
     *
     * @since 1.0.0
     */
    public val SELF_DELEGATION: ViolationCode = ViolationCode("kore.principal.self-delegation")

    /**
     * A delegation chain leads back to a caller that is already in it, such as A acting for B acting for A; reported
     * under `actingFor`.
     *
     * A principal that acts directly for itself is reported as [SELF_DELEGATION] instead.
     *
     * @since 1.0.0
     */
    public val DELEGATION_CYCLE: ViolationCode = ViolationCode("kore.principal.delegation-cycle")

    /**
     * A principal acts on behalf of a principal of another tenant, and acting across tenants was not allowed
     * explicitly; reported under `actingFor`.
     *
     * A principal without a tenant and one with a tenant belong to different tenants.
     *
     * @since 1.0.0
     */
    public val CROSS_TENANT_DELEGATION: ViolationCode = ViolationCode("kore.principal.cross-tenant-delegation")

    /**
     * A delegation chain holds more than [Limits.MAX_DELEGATION_CHAIN] principals; reported under `actingFor`.
     *
     * @since 1.0.0
     */
    public val DELEGATION_TOO_DEEP: ViolationCode = ViolationCode("kore.principal.delegation-too-deep")
}
