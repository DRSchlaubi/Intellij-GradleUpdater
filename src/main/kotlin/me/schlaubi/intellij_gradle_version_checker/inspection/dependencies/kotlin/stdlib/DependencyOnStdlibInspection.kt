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

package me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.kotlin.stdlib

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.schlaubi.intellij_gradle_version_checker.GradleUpdaterBundle
import me.schlaubi.intellij_gradle_version_checker.dependency_format.dependencyFormat
import me.schlaubi.intellij_gradle_version_checker.inspection.AbstractBuildScriptInspection
import me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.DependencyDeclarationVisitor
import me.schlaubi.intellij_gradle_version_checker.util.calleeFunction
import me.schlaubi.intellij_gradle_version_checker.util.isSimple
import me.schlaubi.intellij_gradle_version_checker.util.simpleValue
import org.jetbrains.kotlin.idea.inspections.gradle.getResolvedKotlinGradleVersion
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class DependencyOnStdlibInspection : AbstractBuildScriptInspection() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        val version = getResolvedKotlinGradleVersion(session.file) ?: return PsiElementVisitor.EMPTY_VISITOR
        val (major, minor) = version.split(".")
        return if (major.toInt() > 1 || minor.toInt() >= 4) {
            super.buildVisitor(holder, isOnTheFly, session)
        } else {
            PsiElementVisitor.EMPTY_VISITOR
        }
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        onTheFly: Boolean,
        session: LocalInspectionToolSession,
        file: KtFile
    ): PsiElementVisitor {
        return object : DependencyDeclarationVisitor(true) {
            override fun visitDependencyDeclaration(element: KtCallExpression) {
                val format = element.dependencyFormat
                val name = if (format != null) {
                    with(format) {
                        if (element.isConvertible()) {
                            val components = element.extractComponents()
                            val expression = components.name as? KtStringTemplateExpression ?: return
                            if (expression.isSimple()) {
                                expression.simpleValue
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                } else {
                    val call = element.valueArguments.first().firstChild as? KtCallExpression ?: return
                    val callee = call.calleeFunction ?: return
                    if (callee.fqName?.asString() != "org.gradle.kotlin.dsl.kotlin") return
                    val nameExpression = call.valueArguments[0].firstChild as? KtStringTemplateExpression ?: return
                    if (nameExpression.isSimple()) "kotlin-" + nameExpression.simpleValue else null
                } ?: return

                if (name.startsWith("kotlin-stdlib")) {
                    holder.registerProblem(
                        element,
                        GradleUpdaterBundle.getMessage("inspection.stdlib_dependency.description"),
                        ProblemHighlightType.WARNING,
                        RemoveDependencyQuickfix(element)
                    )
                }
            }
        }
    }
}
