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

import me.schlaubi.intellij_gradle_version_checker.calleeFunction
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*

/**
 * Representation of the different Dependency formats that are available.
 */
sealed class DependencyFormat {
    /**
     * List of all Dependency formats.
     */
    val all: List<DependencyFormat> =
        listOf(NotationDependencyFormat, PositionalDependencyFormat, NamedDependencyFormat)

    /**
     * Checks whether this [KtCallExpression] adds a dependency in the specified format.
     */
    abstract fun KtCallExpression.isFromThisType(): Boolean

    /**
     * Generates a [KtCallExpression] for this format.
     */
    abstract fun KtPsiFactory.generate(group: String, artifact: String, version: String?): KtValueArgumentList

    object NotationDependencyFormat : DependencyFormat() {
        override fun KtCallExpression.isFromThisType(): Boolean {
            val function = calleeFunction ?: return false
            val parameters = function.valueParameters

            // Either the first param is a string and the second is configure action or only string
            if (parameters.size !in 1..2) return false

            val possibleNotation = parameters.first()
            if (possibleNotation.isString()) return false

            if (parameters.size == 1) return true
            return parameters[1].isConfigureAction()
        }

        override fun KtPsiFactory.generate(group: String, artifact: String, version: String?): KtValueArgumentList =
            createValueArgumentListByPattern(""""$group:$artifact${version?.let { ":$version" } ?: ""}"""")
    }

    object PositionalDependencyFormat : DependencyFormat() {
        override fun KtCallExpression.isFromThisType(): Boolean {
            TODO("Not yet implemented")
        }

        override fun KtPsiFactory.generate(group: String, artifact: String, version: String?): KtValueArgumentList {
            TODO("Not yet implemented")
        }
    }

    object NamedDependencyFormat : DependencyFormat() {
        override fun KtCallExpression.isFromThisType(): Boolean {
            TODO("Not yet implemented")
        }

        override fun KtPsiFactory.generate(group: String, artifact: String, version: String?): KtValueArgumentList {
            TODO("Not yet implemented")
        }

    }
}

private fun KtParameter.isString() =
    type()?.fqName?.asString() != "kotlin.String"

private fun KtParameter.isConfigureAction(): Boolean {
    return type()?.fqName?.asString() == "org.gradle.api.Action" // is Action
            && typeParameters.firstOrNull()?.type()?.fqName
        ?.asString() == "org.gradle.api.artifacts.ExternalModuleDependency" // is Action<ExternalModuleDependency>
}
