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

package me.schlaubi.intellij_gradle_version_checker.inspection.plugins

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import me.schlaubi.intellij_gradle_version_checker.copy.gradleOfficialPlugins
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*

abstract class GradlePluginDeclarationVisitor : PsiElementVisitor() {
    abstract fun visitPluginId(element: PsiElement, pluginId: String)

    override fun visitElement(element: PsiElement) {
        if (element !is KtCallExpression) return
        val function =
            ((element.calleeExpression as? KtReferenceExpression)?.resolve() as? KtNamedFunction) ?: return
        if (!function.isPluginSpecDeclaration()) return
        val pluginId = element.extractPluginId() ?: return

        visitPluginId(element, pluginId)
    }
}

class BuiltInPluginWithIdInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : GradlePluginDeclarationVisitor() {
            override fun visitPluginId(element: PsiElement, pluginId: String) {
                val gradleId = pluginId.substringAfter("org.gradle.")
                if (gradleId != pluginId || pluginId in gradleOfficialPlugins) {
                    holder.registerProblem(
                        element,
                        "Use built-in",
                        ProblemHighlightType.WEAK_WARNING,
                        ReplaceWithBuiltInQuickfix(gradleId)
                    )
                }
            }
        }
    }
}

class KotlinPluginWithIdInspection : LocalInspectionTool() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        if (session.file !is KtFile) return PsiElementVisitor.EMPTY_VISITOR

        return object : GradlePluginDeclarationVisitor() {
            override fun visitPluginId(element: PsiElement, pluginId: String) {
                val kotlinModule = pluginId.substringAfter("org.jetbrains.kotlin.")
                if (kotlinModule != pluginId) {
                    holder.registerProblem(
                        element,
                        "use kotlin()",
                        ProblemHighlightType.WEAK_WARNING,
                        ReplaceWithKotlinQuickfix(kotlinModule)
                    )
                }
            }
        }
    }
}

private fun KtNamedFunction.isPluginSpecDeclaration(): Boolean {
    return (parent.parent as? KtNamedDeclaration)?.fqName?.asString() == "org.gradle.kotlin.dsl.PluginDependenciesSpecScope" && name == "id"
}

private fun KtCallExpression.extractPluginId(): String? {
    return ((valueArguments.firstOrNull()?.children?.firstOrNull() as? KtStringTemplateExpression)?.entries?.firstOrNull() as? KtLiteralStringTemplateEntry)?.text
}
