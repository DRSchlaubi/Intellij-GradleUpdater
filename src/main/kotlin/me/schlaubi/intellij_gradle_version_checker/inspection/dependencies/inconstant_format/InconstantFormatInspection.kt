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

package me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.inconstant_format

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.schlaubi.intellij_gradle_version_checker.dependency_format.dependencyFormat
import me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.DependencyDeclarationVisitor
import me.schlaubi.intellij_gradle_version_checker.util.dependencyFormat
import org.jetbrains.kotlin.psi.KtCallExpression

class InconstantFormatInspection : LocalInspectionTool() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        return object : DependencyDeclarationVisitor(false) {
            override fun visitDependencyDeclaration(element: KtCallExpression) {
                val elementFormat = element.dependencyFormat ?: return

                if (elementFormat != element.project.dependencyFormat) {
                    val convertible = with(elementFormat) {
                        element.isConvertible()
                    }
                    val pair = with(elementFormat) {
                        if (convertible) {
                            val extracted = element.extractComponents()
                            val (group, name, version) = extracted

                            "${group.text}:${name.text}:${version?.text}" to extracted
                        } else {
                            null
                        }
                    }
                    if (convertible) {
                        val (extracted, declaration) = pair!!
                        holder.registerProblem(
                            element,
                            "Extract: $extracted",
                            ReplaceBySelectedFormatQuickFix(element, declaration)
                        )
                    } else {
                        holder.registerProblem(element, "LOL")
                    }
                }
            }
        }
    }
}