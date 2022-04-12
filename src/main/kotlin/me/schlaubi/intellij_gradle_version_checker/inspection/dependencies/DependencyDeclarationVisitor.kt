/*
 * MIT License
 *
 * Copyright (c) 2020-2022 Michael Rittmeister
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

package me.schlaubi.intellij_gradle_version_checker.inspection.dependencies

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import me.schlaubi.intellij_gradle_version_checker.util.isDependencyDeclaration
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings

abstract class DependencyDeclarationVisitor(private val ignoreConfigurations: Boolean) : PsiElementVisitor() {

    private var gradleExtensionsSettings: GradleExtensionsSettings.GradleExtensionsData? = null

    override fun visitElement(element: PsiElement) {
        val module = element.module ?: return
        if (!element.containingFile.name.endsWith(".kts") || element !is KtCallExpression) return

        if (!ignoreConfigurations && gradleExtensionsSettings == null) {
            val extensions = GradleExtensionsSettings.getInstance(element.project)
                .getExtensionsFor(module) ?: return

            gradleExtensionsSettings = extensions
        }

        if (element.isDependencyDeclaration(gradleExtensionsSettings)) {
            visitDependencyDeclaration(element)
        }
    }

    abstract fun visitDependencyDeclaration(element: KtCallExpression)
}
