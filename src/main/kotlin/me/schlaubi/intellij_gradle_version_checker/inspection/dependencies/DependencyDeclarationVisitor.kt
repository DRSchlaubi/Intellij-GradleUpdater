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

package me.schlaubi.intellij_gradle_version_checker.inspection.dependencies

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import me.schlaubi.intellij_gradle_version_checker.calleeFunction
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings

abstract class DependencyDeclarationVisitor(private val ignoreConfigurations: Boolean) : PsiElementVisitor() {

    private lateinit var gradleExtensionsSettings: GradleExtensionsSettings.GradleExtensionsData

    override fun visitElement(element: PsiElement) {
        val module = element.module ?: return
        if (!element.containingFile.name.endsWith(".kts") || element !is KtCallExpression) return

        if (!ignoreConfigurations && !::gradleExtensionsSettings.isInitialized) {
            val extensions = GradleExtensionsSettings.getInstance(element.project).getExtensionsFor(module) ?: return

            gradleExtensionsSettings = extensions
        }

        if (element.isDependencyDeclaration()) {
            println("Is declar: ${element.text}")
            visitDependencyDeclaration(element)
        }
    }

    abstract fun visitDependencyDeclaration(element: KtCallExpression)

    private fun KtCallExpression.isDependencyDeclaration(): Boolean {
        val callee = calleeFunction
        if (callee == null) { // Custom declarations
            val descriptor = calleeExpression?.resolveMainReferenceToDescriptors()?.firstOrNull() as? PropertyDescriptor
                ?: return false

            return descriptor.type.fqName?.asString() == "org.gradle.api.artifacts.Configuration"
        } else if (callee.greenStub?.isExtension() == true) { // built-ins
            val children = PsiTreeUtil.findChildrenOfType(callee, KtUserType::class.java).asSequence().map { it.text }
            return children.any {
                it == "org.gradle.api.artifacts.dsl.DependencyHandler"
            }
                    && (ignoreConfigurations || callee.name in gradleExtensionsSettings.configurations.keys)
        }

        return false
    }
}
