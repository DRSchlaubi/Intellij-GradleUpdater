/*
 * MIT License
 *
 * Copyright (c) 2020 Michael Rittmeister
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.schlaubi.intellij_gradle_version_checker.dependency_format

import me.schlaubi.intellij_gradle_version_checker.util.toPsiTemplate
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Creates a [DependencyDeclaration] from strings of [group], [name] and [version].
 *
 * @param factory the [KtPsiFactory] to create psi elements
 */
fun DependencyDeclaration(
    factory: KtPsiFactory,
    group: String,
    name: String,
    version: String? = null
) = DependencyDeclaration(group.toPsiTemplate(factory), name.toPsiTemplate(factory), version?.toPsiTemplate(factory))

/**
 * Representation of a Gradle dependency declaration.
 *
 * @property group the [KtExpression] representing the group
 * @property name the [KtExpression] representing the name
 * @property version the [KtExpression] representing the version
 */
data class DependencyDeclaration(
    val group: KtExpression,
    val name: KtExpression,
    val version: KtExpression?
)
