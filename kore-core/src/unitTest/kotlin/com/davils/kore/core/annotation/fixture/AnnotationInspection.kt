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

package com.davils.kore.core.annotation.fixture

import java.lang.classfile.AnnotationElement
import java.lang.classfile.AnnotationValue
import java.lang.classfile.Attributes
import java.lang.classfile.ClassFile
import kotlin.reflect.KClass
import java.lang.classfile.Annotation as ClassFileAnnotation

internal const val REQUIRES_OPT_IN_DESCRIPTOR: String = "Lkotlin/RequiresOptIn;"

internal fun descriptorOf(type: KClass<*>): String = "L${internalNameOf(type)};"

internal fun retentionOf(type: KClass<out Annotation>): AnnotationRetention? =
    type.java.getAnnotation(Retention::class.java)?.value

internal fun targetsOf(type: KClass<out Annotation>): Set<AnnotationTarget> {
    val target = type.java.getAnnotation(Target::class.java) ?: return emptySet()
    return target.allowedTargets.toSet()
}

internal fun isDocumented(type: KClass<out Annotation>): Boolean =
    type.java.isAnnotationPresent(MustBeDocumented::class.java)

internal fun invisibleAnnotationsOf(type: KClass<out Annotation>): List<InvisibleAnnotation> {
    val model = ClassFile.of().parse(classBytesOf(type))
    val attribute = model.findAttribute(Attributes.runtimeInvisibleAnnotations())
    val annotations = attribute.map { it.annotations() }.orElse(emptyList())
    return annotations.map(::invisibleAnnotationOf)
}

private fun invisibleAnnotationOf(annotation: ClassFileAnnotation): InvisibleAnnotation {
    val descriptor = annotation.classSymbol().descriptorString()
    val enumConstants = annotation.elements().mapNotNull(::enumConstantOf)
    return InvisibleAnnotation(descriptor, enumConstants.toMap())
}

private fun enumConstantOf(element: AnnotationElement): Pair<String, String>? {
    val value = element.value()
    if (value !is AnnotationValue.OfEnum) return null
    return element.name().stringValue() to value.constantName().stringValue()
}

private fun classBytesOf(type: KClass<*>): ByteArray {
    val resource = "${internalNameOf(type)}.class"
    val loader = type.java.classLoader
    val stream = checkNotNull(loader.getResourceAsStream(resource)) { "$resource is not on the classpath" }
    return stream.use { it.readAllBytes() }
}

private fun internalNameOf(type: KClass<*>): String {
    val binaryName = type.java.name
    return binaryName.replace(oldChar = '.', newChar = '/')
}
