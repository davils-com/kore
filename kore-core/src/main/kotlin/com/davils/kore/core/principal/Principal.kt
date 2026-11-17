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
import com.davils.kore.core.text.escape.SafeDisplay
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.validateOrThrow
import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty

private const val SUBJECT: String = "principal"
private const val NO_REQUIRED_ROLE_MESSAGE: String = "At least one required role must be given"
private const val TOO_MANY_ROLES_MESSAGE: String = "must not hold more than ${Limits.MAX_ROLES} roles"
private const val SELF_DELEGATION_MESSAGE: String = "must not be the principal itself"
private const val CYCLE_MESSAGE: String = "must not lead back to a principal that is already in the chain"
private const val CROSS_TENANT_MESSAGE: String =
    "must belong to the same tenant unless acting across tenants is allowed explicitly"

/**
 * The caller on whose behalf code runs: who it is, which tenant it belongs to, which roles it holds and, when it acts
 * for somebody else, for whom.
 *
 * A principal is a claim, not a proof. Any code can create one with any id, tenant and roles, through
 * `Principal(id)`, [principal] or [onBehalfOf], and Kore never checks that the caller was authenticated. Create
 * principals only at the edge of the application that verified the caller, such as the code that checked a token,
 * and hand them on with [withPrincipal]. A principal is no element of a coroutine context itself: [withPrincipal]
 * makes it the current principal of a coroutine and guards that installation, not the creation, and a coroutine
 * context taken from inside its block while the block runs carries the principal along, as described there.
 *
 * Every principal Kore hands out is valid. Creating one that breaks a rule throws a [ValidationException] whose
 * report names every problem under the field it concerns, with the codes of [PrincipalViolationCodes]:
 *
 * - the [id] is at most [Limits.MAX_PRINCIPAL_ID_LENGTH] long, and every role, the [tenant] and the
 *   [displayName] at most [Limits.MAX_PRINCIPAL_TEXT_LENGTH], all counted in UTF-16 code units;
 * - none of them is blank, that is, empty or made only of whitespace and the braille pattern blank U+2800;
 * - none of them contains a character that is unsafe to show: a control, format, line separator, paragraph
 *   separator, private-use, unassigned or lone surrogate character, a variation selector, the Mongolian free
 *   variation selectors included, or any other character that Unicode marks as default ignorable because it shows as
 *   nothing, such as the Hangul fillers, the Khmer inherent vowels and the combining grapheme joiner. Tabs, line
 *   breaks, bidirectional overrides, zero-width joiners and soft hyphens are refused, and so is an emoji sequence
 *   built with a joiner or a variation selector. Which characters are unassigned follows the Unicode version of the
 *   running JDK;
 * - the id, every role and the tenant neither start nor end with whitespace, no-break and ideographic spaces and the
 *   braille pattern blank included, so two of them never differ only in blank space at their edges; a display name
 *   may;
 * - a principal holds at most [Limits.MAX_ROLES] roles;
 * - a delegation chain holds at most [Limits.MAX_DELEGATION_CHAIN] principals, names no caller twice and stays
 *   within one tenant unless crossing tenants was allowed explicitly, see [onBehalfOf].
 *
 * Every other printable character is allowed, such as `|`, `+`, `@` and `=`, commas, quotes and inner spaces, so the
 * subjects that common identity providers issue fit as they are. Text is compared exactly: Kore neither trims, nor
 * changes the case, nor normalises Unicode, so `Admin` and `admin` are two roles, and an `é` written as one code point
 * and one written as `e` with a combining accent make two different ids. Kore does not detect look-alike characters
 * of different scripts either: an id with a Cyrillic `а` in place of a Latin `a` is another caller, although both
 * look the same wherever they are shown.
 *
 * A principal is immutable and safe to share between threads. Its [toString] shows only the id and the kind.
 *
 * @since 1.0.0
 */
public class Principal private constructor(
    internal val identity: CallerIdentity,
    roles: Collection<String>,
    /**
     * A name of the caller for people to read, or null.
     *
     * It is neither unique nor verified, so it never decides access, and [toString] never shows it.
     *
     * @since 1.0.0
     */
    public val displayName: String?,
    /**
     * Further facts about the caller, such as the claims of a verified token.
     *
     * See [PrincipalAttributes] for how their values are masked.
     *
     * @since 1.0.0
     */
    public val attributes: PrincipalAttributes,
    /**
     * The principal this one acts on behalf of, or null when it acts for nobody but itself.
     *
     * @since 1.0.0
     */
    public val actingFor: Principal?,
    acrossTenants: Boolean,
    private val hasTooManyRoles: Boolean,
) : Validatable {
    /**
     * The roles this principal holds itself, compared exactly; an immutable copy of the roles it was created with.
     *
     * Roles never travel along a delegation chain: a principal that acts for another holds only its own.
     *
     * @since 1.0.0
     */
    public val roles: Set<String> = copyOf(roles, hasTooManyRoles)

    private val isActingAcrossTenants: Boolean =
        actingFor != null && acrossTenants && actingFor.identity.tenant != identity.tenant

    /**
     * The identifier of the caller, such as the subject of a verified token or the name of a service account.
     *
     * Kore treats the id, the [kind] and the [tenant] together as the identity of a caller: the same id with another
     * kind or in another tenant is another caller, for example when Kore looks for a cycle in a delegation chain.
     * [toString] shows the id, so where ids are personal data, such as e-mail addresses, the rendering is personal
     * data too.
     *
     * @since 1.0.0
     */
    public val id: String get() = identity.id

    /**
     * What kind of caller this is; part of the identity of the caller, see [id].
     *
     * @since 1.0.0
     */
    public val kind: PrincipalKind get() = identity.kind

    /**
     * The tenant the caller belongs to, or null when it belongs to none; part of the identity of the caller.
     *
     * A missing tenant fails closed in [sameTenantAs]: two principals without a tenant are never in the same tenant
     * there. A delegation between two principals without a tenant, on the other hand, does not cross tenants.
     *
     * @since 1.0.0
     */
    public val tenant: String? get() = identity.tenant

    /**
     * This principal, followed by the principal it acts for, the one that one acts for and so on, up to the
     * [originator].
     *
     * The list is immutable, holds at most [Limits.MAX_DELEGATION_CHAIN] principals and is built anew on every
     * read.
     *
     * @since 1.0.0
     */
    public val chain: List<Principal>
        get() = buildList { addAll(generateSequence(this@Principal, Principal::actingFor)) }

    /**
     * The principal at the end of the [chain], on whose behalf the work is ultimately done; this principal itself when
     * it acts for nobody.
     *
     * Roles do not travel along the chain, so a decision about what the originator may do asks the originator.
     *
     * @since 1.0.0
     */
    public val originator: Principal get() = chain.last()

    /**
     * Whether this principal acts on behalf of another one, that is, whether [actingFor] is set.
     *
     * @since 1.0.0
     */
    public val isDelegated: Boolean get() = actingFor != null

    private val parts: List<Any?>
        get() = listOf(identity, roles, displayName, attributes, actingFor, isActingAcrossTenants)

    init {
        validateOrThrow(SUBJECT)
    }

    /**
     * Returns whether this principal holds [role] itself.
     *
     * The role is compared exactly, case included. Roles of the principals this one acts for do not count; ask the
     * [originator] or another member of the [chain] when a decision depends on them.
     *
     * @param role the role to look for.
     * @return true if this principal holds [role].
     * @since 1.0.0
     */
    public fun hasRole(role: String): Boolean = role in roles

    /**
     * Returns whether this principal holds at least one of the given roles.
     *
     * The signature asks for at least one role, so a requirement cannot be empty by accident. Roles are compared as
     * by [hasRole].
     *
     * @param first a role that is enough on its own.
     * @param more further roles, each of which is enough on its own.
     * @return true if this principal holds [first] or any of [more].
     * @since 1.0.0
     */
    public fun hasAnyRole(first: String, vararg more: String): Boolean = hasRole(first) || more.any(::hasRole)

    /**
     * Returns whether this principal holds at least one role of [required].
     *
     * An empty collection fails closed: out of no roles none is held, so the answer is false. Roles are compared as
     * by [hasRole].
     *
     * @param required the roles, each of which is enough on its own.
     * @return true if this principal holds a role of [required]; false if [required] is empty.
     * @since 1.0.0
     */
    public fun hasAnyRole(required: Collection<String>): Boolean = required.any(::hasRole)

    /**
     * Returns whether this principal holds every one of the given roles.
     *
     * The signature asks for at least one role, so a requirement cannot be empty by accident. Roles are compared as
     * by [hasRole].
     *
     * @param first a role that is needed.
     * @param more further roles that are needed as well.
     * @return true if this principal holds [first] and every role of [more].
     * @since 1.0.0
     */
    public fun hasAllRoles(first: String, vararg more: String): Boolean = hasRole(first) && more.all(::hasRole)

    /**
     * Returns whether this principal holds every role of [required].
     *
     * An empty collection is refused instead of answered: every principal holds all of no roles, so an empty
     * requirement, such as one read from a configuration that lacks an entry, would let everybody in. Roles are
     * compared as by [hasRole].
     *
     * @param required the roles that are all needed; at least one.
     * @return true if this principal holds every role of [required].
     * @throws IllegalArgumentException if [required] is empty.
     * @since 1.0.0
     */
    public fun hasAllRoles(required: Collection<String>): Boolean {
        require(required.isNotEmpty()) { NO_REQUIRED_ROLE_MESSAGE }
        return required.all(::hasRole)
    }

    /**
     * Returns whether this principal and [other] belong to the same tenant.
     *
     * A missing tenant fails closed: the answer is false whenever either principal has no tenant, even when both have
     * none. Tenants are compared exactly. Only the tenants of the two principals count, not those of the principals
     * they act for, which differ after a delegation that was allowed to cross tenants.
     *
     * @param other the principal to compare with.
     * @return true if both principals have a tenant and it is the same.
     * @since 1.0.0
     */
    public fun sameTenantAs(other: Principal): Boolean = sameTenantAs(other.tenant)

    /**
     * Returns whether this principal belongs to [other], the tenant of a resource it wants to use.
     *
     * A missing tenant fails closed: the answer is false when this principal has no tenant or [other] is null.
     * Tenants are compared exactly, so the tenant of a resource is stored in the same form in which tenants of
     * principals are issued. Only the tenant of this principal counts, not that of the principal it acts for.
     *
     * @param other the tenant of the resource, or null when the resource belongs to no tenant.
     * @return true if this principal has a tenant and it equals [other].
     * @since 1.0.0
     */
    public fun sameTenantAs(other: String?): Boolean {
        val own = tenant ?: return false
        return own == other
    }

    /**
     * Returns a copy of this principal that acts on behalf of [principal], such as a service that handles the request
     * of a user.
     *
     * The copy keeps the id, kind, tenant, roles, display name and attributes of this principal and takes over no
     * role of [principal]. A delegation this principal already has is replaced, not extended: to extend a chain, call
     * this function on the principal that is to act for the whole chain. This principal stays as it is.
     *
     * The delegation is refused when
     * - [principal] is the same caller as this one, with the same id, kind and tenant
     *   ([PrincipalViolationCodes.SELF_DELEGATION]),
     * - the chain that results names one caller twice, such as A acting for B acting for A
     *   ([PrincipalViolationCodes.DELEGATION_CYCLE]),
     * - the two principals belong to different tenants and [acrossTenants] is false, where a principal without a
     *   tenant and one with a tenant count as different tenants
     *   ([PrincipalViolationCodes.CROSS_TENANT_DELEGATION]),
     * - the chain that results holds more than [Limits.MAX_DELEGATION_CHAIN] principals
     *   ([PrincipalViolationCodes.DELEGATION_TOO_DEEP]).
     *
     * The allowance to cross tenants is kept only when the tenants really differ, and it takes part in [equals].
     *
     * @param principal the principal to act for.
     * @param acrossTenants whether [principal] may belong to another tenant than this principal. Pass true only for an
     * operation across tenants that was authorised on purpose, such as support access.
     * @return the principal that acts on behalf of [principal].
     * @throws ValidationException if the delegation breaks one of these rules; its report names every problem
     * under `actingFor`.
     * @since 1.0.0
     */
    public fun onBehalfOf(principal: Principal, acrossTenants: Boolean = false): Principal = validated(
        identity = identity,
        roles = roles,
        displayName = displayName,
        attributes = attributes,
        actingFor = principal,
        acrossTenants = acrossTenants,
    )

    /**
     * Reports every rule of [Principal] that this principal breaks, each under the field it concerns.
     *
     * Every principal Kore hands out has passed this validation already; it is part of the API so that a principal
     * can be validated as part of a larger value. A message never contains the text it refuses. A refused role is
     * named only in the path of its violation, such as `roles["<role>"]`, which is escaped wherever it is shown.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        checkText(
            property = ::id,
            value = id,
            rule = PrincipalTextRule.Id,
            blankCode = PrincipalViolationCodes.BLANK_ID,
        )
        validateRoles()
        validateOptionalTexts()
        validateDelegation()
    }

    /**
     * Compares two principals by their id, kind, tenant, roles, display name, attributes with their sensitivity, the
     * principal they act for and whether that delegation was allowed to cross tenants.
     *
     * [withPrincipal] relies on this equality to tell whether a principal established again is the same one.
     *
     * @param other the object to compare with.
     * @return true if [other] is a principal that is equal in all of these.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is Principal && parts == other.parts

    /**
     * Returns a hash code derived from everything [equals] compares.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = parts.hashCode()

    /**
     * Returns `Principal(id="<id>", kind=<kind>)`, which is safe to log.
     *
     * The id is quoted, escaped and shortened to [Limits.MAX_DISPLAY_LENGTH] when needed, so an id such as
     * `a", kind=System` cannot pose as another field. The tenant, the display name, the roles, the attributes and the
     * delegation chain are never shown. The id itself is shown, so where ids are personal data, so is the rendering.
     *
     * @return the rendering of this principal.
     * @since 1.0.0
     */
    override fun toString(): String = "Principal(id=${SafeDisplay.quoted(id)}, kind=$kind)"

    private fun ValidationScope.checkText(
        property: KProperty<*>,
        value: String,
        rule: PrincipalTextRule,
        blankCode: ViolationCode,
    ) {
        val problem = rule.problemOf(value) ?: return
        fail(property, problem.requirement(rule), problem.code(blankCode))
    }

    private fun ValidationScope.validateOptionalTexts() {
        val setTenant = tenant
        if (setTenant != null) {
            checkText(
                property = ::tenant,
                value = setTenant,
                rule = PrincipalTextRule.Tenant,
                blankCode = PrincipalViolationCodes.BLANK_TENANT,
            )
        }
        val setDisplayName = displayName ?: return
        checkText(
            property = ::displayName,
            value = setDisplayName,
            rule = PrincipalTextRule.DisplayName,
            blankCode = PrincipalViolationCodes.BLANK_DISPLAY_NAME,
        )
    }

    private fun ValidationScope.validateRoles() {
        if (hasTooManyRoles || roles.size > Limits.MAX_ROLES) {
            fail(::roles, TOO_MANY_ROLES_MESSAGE, PrincipalViolationCodes.TOO_MANY_ROLES)
            return
        }
        val problems = buildMap {
            roles.forEach { role -> PrincipalTextRule.Role.problemOf(role)?.let { problem -> put(role, problem) } }
        }
        each(::roles, problems) { problem ->
            fail(problem.requirement(PrincipalTextRule.Role), problem.code(PrincipalViolationCodes.BLANK_ROLE))
        }
    }

    private fun ValidationScope.validateDelegation() {
        val delegate = actingFor ?: return
        val isSelf = identity == delegate.identity
        if (isSelf) fail(::actingFor, SELF_DELEGATION_MESSAGE, PrincipalViolationCodes.SELF_DELEGATION)
        if (!isSelf && hasRepeatedCaller()) {
            fail(::actingFor, CYCLE_MESSAGE, PrincipalViolationCodes.DELEGATION_CYCLE)
        }
        if (!isActingAcrossTenants && tenant != delegate.tenant) {
            fail(::actingFor, CROSS_TENANT_MESSAGE, PrincipalViolationCodes.CROSS_TENANT_DELEGATION)
        }
        val length = chain.size
        if (length > Limits.MAX_DELEGATION_CHAIN) {
            fail(
                property = ::actingFor,
                message = "makes a chain of $length principals, and at most ${Limits.MAX_DELEGATION_CHAIN} " +
                    "are allowed",
                code = PrincipalViolationCodes.DELEGATION_TOO_DEEP,
            )
        }
    }

    private fun hasRepeatedCaller(): Boolean {
        val callers = chain.map(Principal::identity)
        return callers.toSet().size != callers.size
    }

    /**
     * Creates principals in a single call.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Creates a principal that belongs to no tenant, has neither a display name nor attributes and acts for
         * nobody.
         *
         * The principal is validated before it is returned, with the rules described at [Principal]. The [roles]
         * are copied, so a later change to the given set does not reach the principal; a set of more than
         * [Limits.MAX_ROLES] roles is refused with [PrincipalViolationCodes.TOO_MANY_ROLES] before any of its
         * roles is read. Use [principal] with a block to set the other fields. Kore does not check that the caller
         * was authenticated; create principals only where it was.
         *
         * @param id the identifier of the caller.
         * @param kind what kind of caller it is.
         * @param roles the roles the caller holds.
         * @return the validated principal.
         * @throws ValidationException if the id or a role breaks a rule, or if there are more than
         * [Limits.MAX_ROLES] roles; its report names every problem.
         * @since 1.0.0
         */
        public operator fun invoke(
            id: String,
            kind: PrincipalKind = PrincipalKind.User,
            roles: Set<String> = emptySet(),
        ): Principal = validated(identity = CallerIdentity(id = id, kind = kind, tenant = null), roles = roles)

        internal fun validated(
            identity: CallerIdentity,
            roles: Collection<String>,
            displayName: String? = null,
            attributes: PrincipalAttributes = PrincipalAttributes.EMPTY,
            actingFor: Principal? = null,
            acrossTenants: Boolean = false,
            hasTooManyRoles: Boolean = roles.size > Limits.MAX_ROLES,
        ): Principal = Principal(
            identity = identity,
            roles = roles,
            displayName = displayName,
            attributes = attributes,
            actingFor = actingFor,
            acrossTenants = acrossTenants,
            hasTooManyRoles = hasTooManyRoles,
        )
    }
}

private fun copyOf(roles: Collection<String>, hasTooManyRoles: Boolean): Set<String> {
    if (hasTooManyRoles) return emptySet()
    return buildSet(roles.size) { addAll(roles) }
}
