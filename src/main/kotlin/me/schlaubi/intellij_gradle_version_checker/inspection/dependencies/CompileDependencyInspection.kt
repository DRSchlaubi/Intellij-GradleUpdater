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

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import me.schlaubi.intellij_gradle_version_checker.GradleUpdaterBundle
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub

class CompileDependencyInspection : LocalInspectionTool() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        if (session.file !is KtFile) return PsiElementVisitor.EMPTY_VISITOR

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is KtCallExpression || !element.isDeprecatedDependencyDeclaration()) return

                holder.registerProblem(
                    element,
                    GradleUpdaterBundle.getMessage("inspection.deprecated_dependency_config.description"),
                    ProblemHighlightType.WARNING,
                    SwitchToImplementationQuickfix,
                    SwitchToImplementationAndSyncQuickfix
                )
            }
        }
    }
}

private fun KtCallExpression.isDeprecatedDependencyDeclaration(): Boolean {
    val function = ((calleeExpression as? KtReferenceExpression)?.resolve() as? KtNamedFunction) ?: return false

    return function.isDeprecated() // Only complain if a Gradle version which actually deprecates this is used
            && function.name == "compile"
}

private fun KtTypeParameterListOwnerStub<KotlinFunctionStub>.isDeprecated() =
    annotationEntries.any { it.shortName?.asString() == "Deprecated" }


