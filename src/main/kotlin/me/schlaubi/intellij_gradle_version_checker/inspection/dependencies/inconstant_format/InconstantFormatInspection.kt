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

package me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.inconstant_format

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.schlaubi.intellij_gradle_version_checker.GradleUpdaterBundle
import me.schlaubi.intellij_gradle_version_checker.dependency_format.dependencyFormat
import me.schlaubi.intellij_gradle_version_checker.inspection.AbstractBuildScriptInspection
import me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.DependencyDeclarationVisitor
import me.schlaubi.intellij_gradle_version_checker.settings.ProjectPersistentGradleVersionSettings
import me.schlaubi.intellij_gradle_version_checker.util.dependencyFormat
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile

class InconstantFormatInspection : AbstractBuildScriptInspection() {

    @OptIn(ExperimentalStdlibApi::class)
    override fun buildVisitor(
        holder: ProblemsHolder,
        onTheFly: Boolean,
        session: LocalInspectionToolSession,
        file: KtFile
    ): PsiElementVisitor {
        return object : DependencyDeclarationVisitor(false) {
            override fun visitDependencyDeclaration(element: KtCallExpression) {
                val elementFormat = element.dependencyFormat ?: return
                if (elementFormat != element.project.dependencyFormat) {
                    val convertible = with(elementFormat) {
                        element.isConvertible()
                    }
                    val declaration = with(elementFormat) {
                        if (convertible) {
                            val extracted = element.extractComponents() ?: return@with null

                            extracted
                        } else {
                            null
                        }
                    }

                    val quickFixes = buildList(2) {
                        if (declaration != null) {
                            if (convertible) {
                                add(ReplaceBySelectedFormatQuickFix(element, declaration))
                            }
                            add(
                                ProjectUpdateSettingsQuickfix(
                                    elementFormat,
                                    ProjectPersistentGradleVersionSettings.getInstance(element.project)
                                )
                            )
                            add(ApplicationUpdateSettingsQuickfix(elementFormat))
                        }
                    }

                    holder.registerProblem(
                        element,
                        GradleUpdaterBundle.getMessage("inspection.inconsistent_format.description"),
                        *quickFixes.toTypedArray()
                    )
                }
            }
        }
    }
}
