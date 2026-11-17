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
import com.davils.kore.core.principal.context.PrincipalElement
import com.davils.kore.core.validation.report.ValidationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext

/**
 * Creates a [PrincipalKind.User] principal with the given roles, without a tenant, a display name, attributes or
 * a delegation.
 *
 * A role given twice is held once. More than [Limits.MAX_ROLES] roles, duplicates included, are refused with
 * [PrincipalViolationCodes.TOO_MANY_ROLES] before any of them is read. The principal is validated before it is
 * returned, with the rules described at [Principal]. Kore does not check that the caller was authenticated;
 * create principals only where it was.
 *
 * @param id the identifier of the caller.
 * @param roles the roles the caller holds.
 * @return the validated principal.
 * @throws ValidationException if the id or a role breaks a rule, or if more than [Limits.MAX_ROLES] roles are
 * given; its report names every problem.
 * @since 1.0.0
 */
public fun principal(id: String, vararg roles: String): Principal {
    val identity = CallerIdentity(id = id, kind = PrincipalKind.User, tenant = null)
    return Principal.validated(identity = identity, roles = roles.asList())
}

/**
 * Creates a principal that [block] configures, such as one with a tenant, a display name, attributes or a delegation.
 *
 * The block runs once, on the calling thread. Nothing in the [PrincipalBuilder] throws for invalid input: every
 * problem, a refused attribute included, is collected and reported together once the block has returned, with the
 * rules described at [Principal]. An exception that [block] throws itself propagates unchanged, and so does the
 * [IllegalStateException] of a change that [block] makes after it has evaluated the builder with
 * [PrincipalBuilder.validate]. Kore does not check that the caller was authenticated; create principals only
 * where it was.
 *
 * @param id the identifier of the caller.
 * @param block sets the other fields of the principal.
 * @return the validated principal.
 * @throws ValidationException if a field breaks a rule; its report names every problem under the field it
 * concerns.
 * @since 1.0.0
 */
public fun principal(id: String, block: PrincipalBuilder.() -> Unit): Principal {
    val builder = PrincipalBuilder(id)
    builder.block()
    return builder.build()
}

/**
 * Runs [block] with [principal] as the current principal of the coroutine and returns what [block] returns.
 *
 * Within [block], [currentPrincipal] and [requirePrincipal] return [principal], and so they do in every coroutine
 * started in the scope of [block], such as with `async` or `launch`, and in every flow collected there. This function
 * returns only once [block] and every coroutine it started in its scope have completed. From then on, the principal
 * that was current before is current again, and [principal] is current nowhere any more: a coroutine context that was
 * saved inside [block] and is used again later, such as with `withContext(saved)` or `launch(saved)` in a callback,
 * carries no principal, so [currentPrincipal] returns null and [requirePrincipal] throws there, even where another
 * principal was current before. Wherever the coroutine context is rendered, such as in a debugger or a log line, the
 * principal appears only as [Principal.toString] shows it, with its id and kind.
 *
 * An established principal is not replaced by accident: when another principal is current and it is not equal to
 * [principal], the call throws unless [replacing] is true. Establishing an equal principal again is allowed. With
 * [replacing], [principal] applies to [block] only.
 *
 * This check guards this function, not the coroutine context: while [block] runs, a coroutine context taken from
 * inside it carries [principal] wherever it is used, such as with `withContext(saved)` or `async(saved)` in a
 * coroutine of another scope or of another request, and there it replaces the principal that is current without
 * this check. Never hand a coroutine context, or a scope built from one, from the work of one caller to that of
 * another. The check does not guard the origin of a principal either: any code can create a principal with any roles
 * and establish it where none is current, or pass [replacing]. Call this function at the edge that verified the
 * caller, and treat every use of [replacing] as a decision about security that deserves a review.
 *
 * @param T the type of the result of [block].
 * @param principal the principal to establish.
 * @param replacing whether [principal] may replace a different principal that is already established, for the
 * duration of [block].
 * @param block the code to run with [principal] as the current principal.
 * @return the result of [block].
 * @throws IllegalStateException if a different principal is already established and [replacing] is false; its message
 * contains nothing of either principal.
 * @since 1.0.0
 */
public suspend fun <T> withPrincipal(
    principal: Principal,
    replacing: Boolean = false,
    block: suspend CoroutineScope.() -> T,
): T = PrincipalElement.establish(principal = principal, replacing = replacing, block = block)

/**
 * Returns the principal that the innermost enclosing [withPrincipal] established, or null when there is none.
 *
 * The answer is null as well in a coroutine context that was saved inside the block of a [withPrincipal] that has
 * returned since, even when another principal was current where that context is used again. Code that must not run
 * without a principal calls [requirePrincipal] instead, so that a missing principal fails closed.
 *
 * @return the current principal, or null.
 * @since 1.0.0
 */
public suspend fun currentPrincipal(): Principal? = PrincipalElement.principalIn(currentCoroutineContext())

/**
 * Returns the principal that the innermost enclosing [withPrincipal] established, and fails when there is none.
 *
 * It fails as well where [currentPrincipal] returns null for a coroutine context saved inside the block of a
 * [withPrincipal] that has returned since.
 *
 * @return the current principal.
 * @throws PrincipalMissingException if no principal is established in the coroutine context.
 * @since 1.0.0
 */
public suspend fun requirePrincipal(): Principal = currentPrincipal() ?: throw PrincipalMissingException()
