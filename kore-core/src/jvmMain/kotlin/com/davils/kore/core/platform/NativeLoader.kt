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

package com.davils.kore.core.platform

import com.davils.kore.core.annotation.DavilsInternal

/**
 * Loads a native library and returns the binding through which Kotlin code calls it.
 *
 * A [NativeLibrary] calls the loader at most once, on the first thread that asks for the library, and keeps the
 * outcome. The loader decides which file is loaded from where; Kore does not verify the library it loads.
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary: the package is exported, so any code on the class path can call it.
 *
 * @param Api the type of the binding.
 * @since 1.0.0
 */
@DavilsInternal
public fun interface NativeLoader<out Api : Any> {
    /**
     * Loads the native library and returns its binding.
     *
     * A [LinkageError], a [RuntimeException] or an [java.io.IOException] thrown here tells the [NativeLibrary]
     * that the library is not available; every other throwable is passed on to whoever asked for the library.
     *
     * @return the binding of the loaded library.
     * @throws UnsatisfiedLinkError if the native library cannot be found or linked.
     * @throws java.io.IOException if the library cannot be extracted to the file system.
     * @since 1.0.0
     */
    public fun load(): Api
}
