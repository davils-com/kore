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

package com.davils.kore.core.arguments

import com.davils.kore.core.arguments.declaration.ParameterDraft
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A declared parameter, which becomes part of its schema when a property of that schema delegates to it with `by`.
 *
 * It is returned by the declaring functions of [ParameterDeclarations], such as `required` or `flag`, and is bound
 * to the schema that created it. The type `V` of the property it provides follows from that function: a nullable value
 * for `optional`, a value for `required` and `defaulted`, a list for `multiple` and a [Boolean] for `flag`. A parameter
 * that no property delegates to is not part of the schema and is never read. Reading the property before the schema was
 * loaded throws [com.davils.kore.core.arguments.report.ParametersNotLoadedException].
 *
 * @since 1.0.0
 */
public class Parameter<V> internal constructor(
    private val draft: ParameterDraft<*, V>,
    private val owner: ParameterDeclarations,
) : PropertyDelegateProvider<Parameters, ReadOnlyProperty<Parameters, V>> {
    /**
     * Registers this parameter with [thisRef] under the name of [property] and returns the delegate that reads its
     * value.
     *
     * Kotlin calls this function when the schema is constructed; code does not call it directly. The registration
     * checks that no other parameter of the schema uses the same name or environment variable and that the environment
     * variable is a well-formed name of ASCII letters, digits and underscores that does not start with a digit.
     *
     * @param thisRef the schema whose property delegates to this parameter; it must be the schema that declared it.
     * @param property the property that delegates to this parameter; its name identifies the parameter in groups and
     * messages.
     * @return the delegate that returns the loaded value of the parameter.
     * @throws IllegalArgumentException if [thisRef] is not the schema that declared this parameter, if a name or the
     * environment variable is already used by another parameter of the schema, or if the environment variable is not a
     * well-formed name.
     * @throws IllegalStateException if loading of the schema has already started.
     * @since 1.0.0
     */
    override fun provideDelegate(thisRef: Parameters, property: KProperty<*>): ReadOnlyProperty<Parameters, V> {
        require(thisRef === owner) { foreignSchemaMessage(thisRef, property) }
        return ParameterDelegate(thisRef.register(draft, property.name))
    }
}

private fun foreignSchemaMessage(schema: Parameters, property: KProperty<*>): String {
    val schemaName = schema::class.simpleName.orEmpty()
    return "A parameter can only be delegated to by the schema that declared it, but property " +
        "'${property.name}' of $schemaName delegates to a parameter of another schema"
}
