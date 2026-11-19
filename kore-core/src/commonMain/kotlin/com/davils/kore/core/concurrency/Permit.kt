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

package com.davils.kore.core.concurrency

import com.davils.kore.core.annotation.DavilsExperimental

/**
 * The admission of one piece of work by an [AdmissionGate], which is given back by [close].
 *
 * Closing a permit tells the gate that the work is done. Only the first [close] counts, so a permit closed twice, by
 * a `use` block and by a cleanup, is given back once. A permit is safe to close from any thread.
 *
 * @since 2.1.0
 */
@DavilsExperimental
public class Permit internal constructor(private val gate: AdmissionGate) : AutoCloseable {
    private val released: OnceFlag = OnceFlag()

    /**
     * Gives the permit back to its gate; every later call does nothing.
     *
     * @since 2.1.0
     */
    override fun close() {
        if (released.trySet()) gate.leave()
    }
}
