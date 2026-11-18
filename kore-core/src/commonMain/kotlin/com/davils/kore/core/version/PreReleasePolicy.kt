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

package com.davils.kore.core.version

/**
 * Decides whether a pre-release that lies in a [SemanticVersionRange] by precedence is admitted by it.
 *
 * A pre-release is unfinished software, so a range that a caller writes for releases should not pick one up only
 * because it sorts between two bounds. Releases are admitted under every policy; the policies differ only for
 * pre-releases. [SameCoreOnly] is the default of every function that takes a policy.
 *
 * @since 1.0.0
 */
public enum class PreReleasePolicy {
    /**
     * Admits a pre-release like any release, whenever its precedence lies in the range.
     *
     * Under this policy a range such as `^1.2.3` admits `1.9.0-rc.1` and any other pre-release published within its
     * bounds.
     *
     * @since 1.0.0
     */
    ByPrecedence,

    /**
     * Admits a pre-release only when an interval that holds it has a bound that is a pre-release of the same three
     * numbers, as npm does by default for each comparator set.
     *
     * `>=1.3.0-alpha <2.0.0` admits `1.3.0-beta` but neither `1.4.0-beta` nor `2.0.0-beta`, and `^1.2.3` admits no
     * pre-release at all.
     *
     * @since 1.0.0
     */
    SameCoreOnly,

    /**
     * Admits no pre-release, not even one that a bound of the range names.
     *
     * @since 1.0.0
     */
    ReleasesOnly,
}
