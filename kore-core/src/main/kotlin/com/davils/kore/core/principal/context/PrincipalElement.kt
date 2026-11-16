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

package com.davils.kore.core.principal.context

import com.davils.kore.core.principal.KorePrincipal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

private const val REPLACING_MESSAGE: String = "A different principal is already established in this coroutine " +
    "context. Pass replacing = true to replace it for the block on purpose."

internal class PrincipalElement private constructor(
    private val principal: KorePrincipal,
) : AbstractCoroutineContextElement(PrincipalElement) {
    private val isOpen: AtomicBoolean = AtomicBoolean(true)

    private val openPrincipal: KorePrincipal?
        get() {
            val isStillOpen = isOpen.get()
            return principal.takeIf { isStillOpen }
        }

    override fun toString(): String = principal.toString()

    private fun close() {
        isOpen.set(false)
    }

    companion object Key : CoroutineContext.Key<PrincipalElement> {
        suspend fun <T> establish(
            principal: KorePrincipal,
            replacing: Boolean,
            block: suspend CoroutineScope.() -> T,
        ): T {
            val established = principalIn(currentCoroutineContext())
            check(replacing || established == null || established == principal) { REPLACING_MESSAGE }
            val element = PrincipalElement(principal)
            return withContext(element) {
                try {
                    coroutineScope(block)
                } finally {
                    element.close()
                }
            }
        }

        fun principalIn(context: CoroutineContext): KorePrincipal? = context[Key]?.openPrincipal
    }
}
