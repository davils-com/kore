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

/**
 * Counts the steps of one piece of work against a [maximum], so that work whose size an attacker controls, such as a
 * query, a diff, a merge or the matching of a pattern, stops at a limit instead of running for as long as the input
 * makes it.
 *
 * The work charges the budget for each step it takes, with [charge] where the excess should end the work with a
 * [WorkLimitException], or with [tryCharge] where the caller ends it on its own. A budget fails closed: once a charge
 * would exceed [maximum], the budget is exhausted, that charge and every later one fail, and [spent] keeps the steps
 * charged before. The sum of the steps never overflows, whatever the charges are.
 *
 * The exception names [limit], the name of the limit, and never anything of the input. A library that throws its own
 * exception for an excess catches [WorkLimitException] at its boundary and reads [WorkLimitException.limit].
 *
 * A budget is mutable and not safe to use from several threads at once without synchronization of the caller; one
 * budget belongs to one piece of work.
 *
 * @property maximum the most steps the work may take.
 * @property limit the name of the limit [maximum] comes from, such as `maximumQuerySteps`; it is shown escaped and
 * shortened in the message of a [WorkLimitException].
 * @throws IllegalArgumentException if [maximum] is negative.
 * @since 2.2.0
 */
@DavilsInternal
public class WorkBudget(public val maximum: Long, public val limit: String) {
    init {
        require(maximum >= 0L) { "The maximum of a work budget must not be negative, but was $maximum" }
    }

    /**
     * The steps charged so far, never more than [maximum].
     *
     * @since 2.2.0
     */
    public var spent: Long = 0L
        private set

    /**
     * Whether a charge failed, so that every further charge fails as well.
     *
     * @since 2.2.0
     */
    public var isExhausted: Boolean = false
        private set

    /**
     * The steps that may still be charged, `0` once the budget [isExhausted].
     *
     * @since 2.2.0
     */
    public val remaining: Long
        get() = (maximum - spent).takeUnless { isExhausted } ?: 0L

    /**
     * Charges [steps] steps, or throws if they exceed what remains.
     *
     * @param steps the steps to charge, `1` by default.
     * @throws WorkLimitException if [steps] exceed [remaining] or the budget [isExhausted]; the budget is exhausted
     * afterwards.
     * @throws IllegalArgumentException if [steps] is negative.
     * @since 2.2.0
     */
    public fun charge(steps: Long = 1L) {
        if (!tryCharge(steps)) throw WorkLimitException(limit, maximum)
    }

    /**
     * Charges [steps] steps if they do not exceed what remains.
     *
     * @param steps the steps to charge, `1` by default.
     * @return `true` if the steps were charged, `false` if they exceed [remaining] or the budget [isExhausted]; the
     * budget is exhausted afterwards.
     * @throws IllegalArgumentException if [steps] is negative.
     * @since 2.2.0
     */
    public fun tryCharge(steps: Long = 1L): Boolean {
        require(steps >= 0L) { "A work budget is charged a number of steps that is not negative, but was $steps" }
        val isWithin = !isExhausted && steps <= maximum - spent
        if (!isWithin) {
            isExhausted = true
            return false
        }
        spent += steps
        return true
    }
}
