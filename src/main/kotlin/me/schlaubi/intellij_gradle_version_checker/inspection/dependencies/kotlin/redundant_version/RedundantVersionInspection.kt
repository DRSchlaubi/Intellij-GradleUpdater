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

package me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.kotlin.redundant_version

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.SmartPointerManager
import me.schlaubi.intellij_gradle_version_checker.inspection.AbstractBuildScriptInspection
import me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.DependencyDeclarationVisitor
import me.schlaubi.intellij_gradle_version_checker.util.calleeFunction
import me.schlaubi.intellij_gradle_version_checker.util.isSimple
import me.schlaubi.intellij_gradle_version_checker.util.simpleValue
import org.jetbrains.kotlin.idea.inspections.gradle.getResolvedKotlinGradleVersion
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class RedundantVersionInspection : AbstractBuildScriptInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        onTheFly: Boolean,
        session: LocalInspectionToolSession,
        file: KtFile
    ): PsiElementVisitor {
        return object : DependencyDeclarationVisitor(true) {
            override fun visitDependencyDeclaration(element: KtCallExpression) {
                val call = element.valueArguments.first().firstChild as? KtCallExpression ?: return
                val callee = call.calleeFunction ?: return
                if (callee.fqName?.asString() != "org.gradle.kotlin.dsl.kotlin") return

                val version = call.valueArguments.getOrNull(1) ?: return
                val string = version.firstChild as? KtStringTemplateExpression ?: return
                if (!string.isSimple()) return

                val module = element.module ?: return
                if (string.simpleValue == getResolvedKotlinGradleVersion(module)) {
                    val argumentsPointer = SmartPointerManager.createPointer(call.valueArgumentList!!)
                    val argumentPointer = SmartPointerManager.createPointer(version)
                    holder.registerProblem(
                        string,
                        "Unnötig",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        RemoveRedundantVersionQuickfix(
                            argumentsPointer, argumentPointer
                        )
                    )
                }
            }
        }
    }
}
