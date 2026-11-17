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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kore.core.limit.Limits
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.Violation
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.validation.violation.ViolationPath

private const val BUILDER_SUBJECT: String = "principal"
private const val NAME_PREFIX: String = "an attribute name "
private const val TOO_MANY_ATTRIBUTES_MESSAGE: String =
    "must not hold more than ${Limits.MAX_ATTRIBUTES} attributes"

private val attributesPath: ViolationPath = ViolationPath.named(Principal::attributes)

/**
 * Configures a [Principal] inside the block of [principal].
 *
 * The builder starts with a [PrincipalKind.User] that has no tenant, display name, roles, attributes or
 * delegation. Nothing in it throws for invalid input: every problem is collected, and once the block has returned
 * [principal] throws one [ValidationException] whose report names each problem under the field it concerns, with
 * the rules described at [Principal] and the codes of [PrincipalViolationCodes].
 *
 * A builder serves the one block it is created for and is not thread-safe. Its first evaluation, by [validate] inside
 * the block or by [principal] once the block has returned, closes it: from then on every change, of a property or
 * through a function, throws an [IllegalStateException] instead of being lost, and the principal stays as it was
 * evaluated.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class PrincipalBuilder internal constructor(
    private val id: String,
) : ValidatingBuilder<Principal>() {
    private val heldRoles: MutableSet<String> = mutableSetOf()
    private var hasTooManyRoles: Boolean = false
    private var attributes: PrincipalAttributes = PrincipalAttributes.EMPTY
    private var delegate: Principal? = null
    private var isActingAcrossTenants: Boolean = false
    private var isOverfull: Boolean = false

    override val subject: String get() = BUILDER_SUBJECT

    /**
     * What kind of caller the principal is; [PrincipalKind.User] unless it is set.
     *
     * Setting it once the builder was evaluated throws an [IllegalStateException].
     *
     * @since 1.0.0
     */
    public var kind: PrincipalKind = PrincipalKind.User
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The tenant the caller belongs to, or null, the default, when it belongs to none.
     *
     * When set, it must not be blank, contain a character that is unsafe to show, start or end with whitespace or be
     * longer than [Limits.MAX_PRINCIPAL_TEXT_LENGTH]. Setting it once the builder was evaluated throws an
     * [IllegalStateException].
     *
     * @since 1.0.0
     */
    public var tenant: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * A name of the caller for people to read, or null, the default.
     *
     * When set, it must not be blank, contain a character that is unsafe to show or be longer than
     * [Limits.MAX_PRINCIPAL_TEXT_LENGTH]; unlike the id, it may start or end with whitespace. Setting it once the
     * builder was evaluated throws an [IllegalStateException].
     *
     * @since 1.0.0
     */
    public var displayName: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Adds roles that the principal holds.
     *
     * Every call adds to the roles of the calls before it, and a role given twice is held once. The roles are checked
     * once the block has returned: a refused role is reported under `roles["<role>"]`, escaped wherever it is shown.
     * More than [Limits.MAX_ROLES] roles in all, or more than [Limits.MAX_ROLES] names in one call, even when
     * some of them repeat, are reported once under `roles` with [PrincipalViolationCodes.TOO_MANY_ROLES]; the
     * names of a call that is too large are not read, and once there are too many roles, no further role is kept or
     * checked.
     *
     * @param names the roles to add.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun roles(vararg names: String) {
        checkOpen()
        hasTooManyRoles = hasTooManyRoles || names.size > Limits.MAX_ROLES
        if (hasTooManyRoles) return
        heldRoles += names
        hasTooManyRoles = heldRoles.size > Limits.MAX_ROLES
    }

    /**
     * Sets the attribute [name] of the principal to [value], with the rules of [PrincipalAttributes].
     *
     * An attribute of the same name is replaced together with its sensitivity, so setting it again without passing
     * [sensitive] marks it as sensitive again. A refused attribute is not stored but reported, and the report never
     * contains its value: a refused name is reported under `attributes` without the name, a refused value under
     * `attributes["<name>"]`. A name that is blank is reported with [PrincipalViolationCodes.INVALID_ATTRIBUTE],
     * a name or value that is too long or holds a character that is unsafe to show with
     * [PrincipalViolationCodes.TOO_LONG] or [PrincipalViolationCodes.INVALID_CHARACTER]. Attributes beyond
     * [Limits.MAX_ATTRIBUTES] are reported once under `attributes` with
     * [PrincipalViolationCodes.TOO_MANY_ATTRIBUTES]; an attribute that is already set may still be replaced.
     *
     * @param name the name of the attribute, compared exactly.
     * @param value the value of the attribute.
     * @param sensitive whether the value is sensitive; leave it true unless the value is known to be safe to show,
     * such as the issuer of a token.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun attribute(name: String, value: String, sensitive: Boolean = true) {
        checkOpen()
        val isAccepted = acceptsName(name) && acceptsValue(name, value) && hasRoomFor(name)
        if (!isAccepted) return
        attributes = attributes.with(name = name, value = value, sensitive = sensitive)
    }

    /**
     * Lets the principal act on behalf of [principal], with the rules of [Principal.onBehalfOf].
     *
     * A later call replaces an earlier one. A delegation that breaks a rule is reported under `actingFor` once the
     * block has returned.
     *
     * @param principal the principal to act for.
     * @param acrossTenants whether [principal] may belong to another tenant than the principal being built. Pass true
     * only for an operation across tenants that was authorised on purpose, such as support access.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun actingFor(principal: Principal, acrossTenants: Boolean = false) {
        checkOpen()
        delegate = principal
        isActingAcrossTenants = acrossTenants
    }

    override fun assemble(): Principal = Principal.validated(
        identity = CallerIdentity(id = id, kind = kind, tenant = tenant),
        roles = heldRoles,
        displayName = displayName,
        attributes = attributes,
        actingFor = delegate,
        acrossTenants = isActingAcrossTenants,
        hasTooManyRoles = hasTooManyRoles,
    )

    private fun acceptsName(name: String): Boolean {
        val problem = PrincipalTextRule.AttributeName.problemOf(name) ?: return true
        val message = NAME_PREFIX + problem.requirement(PrincipalTextRule.AttributeName)
        report(attributesPath, message, problem.code(PrincipalViolationCodes.INVALID_ATTRIBUTE))
        return false
    }

    private fun acceptsValue(name: String, value: String): Boolean {
        val problem = PrincipalTextRule.AttributeValue.problemOf(value) ?: return true
        val message = problem.requirement(PrincipalTextRule.AttributeValue)
        report(attributesPath.key(name), message, problem.code(PrincipalViolationCodes.INVALID_ATTRIBUTE))
        return false
    }

    private fun hasRoomFor(name: String): Boolean {
        val isFull = name !in attributes && attributes.names.size >= Limits.MAX_ATTRIBUTES
        if (isFull) reportOverfull()
        return !isFull
    }

    private fun reportOverfull() {
        if (isOverfull) return
        isOverfull = true
        report(attributesPath, TOO_MANY_ATTRIBUTES_MESSAGE, PrincipalViolationCodes.TOO_MANY_ATTRIBUTES)
    }

    private fun report(path: ViolationPath, message: String, code: ViolationCode) {
        val violation = Violation(
            path = ViolationPath.ROOT,
            code = code,
            severity = Severity.Error,
            message = message,
        )
        adopt(listOf(violation), path)
    }
}
