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

package com.davils.kore.core.version.grammar

import com.davils.kore.core.version.KoreSemanticVersion
import com.davils.kore.core.version.interval.floorAbove
import com.davils.kore.core.version.interval.releaseFloor
import com.davils.kore.core.version.interval.successor

internal class Partial(internal val numbers: List<Int>, internal val version: KoreSemanticVersion?) {
    internal val isAny: Boolean get() = numbers.isEmpty()

    internal fun lowest(): KoreSemanticVersion = version ?: releaseFloor(numbers)

    internal fun firstAbove(): KoreSemanticVersion? {
        val complete = version ?: return floorAbove(numbers)
        return successor(complete)
    }
}
