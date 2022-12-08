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

package me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.kotlin.unnecessary_coordinates

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.SmartPointerManager
import me.schlaubi.intellij_gradle_version_checker.GradleUpdaterBundle
import me.schlaubi.intellij_gradle_version_checker.dependency_format.dependencyFormat
import me.schlaubi.intellij_gradle_version_checker.inspection.AbstractBuildScriptInspection
import me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.DependencyDeclarationVisitor
import me.schlaubi.intellij_gradle_version_checker.util.equalsString
import me.schlaubi.intellij_gradle_version_checker.util.isSimple
import me.schlaubi.intellij_gradle_version_checker.util.simpleValue
import me.schlaubi.intellij_gradle_version_checker.util.startsWith
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.groovy.inspections.findResolvedKotlinGradleVersion
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class KotlinInspectionWithCoordinatesInspection : AbstractBuildScriptInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        onTheFly: Boolean,
        session: LocalInspectionToolSession,
        file: KtFile
    ): PsiElementVisitor {
        return object : DependencyDeclarationVisitor(false) {
            override fun visitDependencyDeclaration(element: KtCallExpression) {
                val psiModule = element.module ?: return
                val elementFormat = element.dependencyFormat ?: return
                val declaration = with(elementFormat) {
                    if (!element.isConvertible()) {
                        null
                    } else {
                        element.extractComponents()
                    }
                } ?: return

                val (group, name, version) = declaration
                val groupTemplate = group as? KtStringTemplateExpression ?: return
                val nameTemplate = name as? KtStringTemplateExpression ?: return
                if (
                    groupTemplate.equalsString("org.jetbrains.kotlin") &&
                    nameTemplate.startsWith("kotlin-")
                ) {
                    val module = name.simpleValue.substringAfter("kotlin-")
                    val versionsSimple = (version as? KtStringTemplateExpression)?.isSimple() == true

                    // I literally spent hrs searching for this function
                    val gradlePluginVersion by lazy { findResolvedKotlinGradleVersion(psiModule)?.rawVersion }
                    val versionExpression =
                        if (versionsSimple && (version as KtStringTemplateExpression).simpleValue == gradlePluginVersion) {
                            null
                        } else version

                    val callPointer = SmartPointerManager.createPointer(element)
                    val versionPointer = versionExpression?.let { SmartPointerManager.createPointer(versionExpression) }

                    holder.registerProblem(
                        element,
                        GradleUpdaterBundle.getMessage("inspection.coordinates_on_kotlin_dependency"),
                        ReplaceWithKotlinNotationQuickfix(
                            callPointer,
                            module,
                            versionPointer
                        )
                    )
                }
            }
        }
    }
}
