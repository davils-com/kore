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

package com.davils.kore.core.budget

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.internal.SafeDisplay

/**
 * Thrown when a piece of work needs more steps than its [WorkBudget] allows.
 *
 * The work stops at the first excess. The message names the limit, escaped and shortened, and its maximum, and
 * never anything of the input the work read.
 *
 * @property limit the name of the limit that was exceeded, as the [WorkBudget] was given it.
 * @property maximum the most steps the limit allows.
 * @since 2.2.0
 */
@DavilsInternal
public class WorkLimitException internal constructor(public val limit: String, public val maximum: Long) :
    IllegalStateException(limitMessage(limit, maximum))

private fun limitMessage(limit: String, maximum: Long): String =
    "The work needs more than the $maximum steps that the limit ${SafeDisplay.text(limit)} allows"
