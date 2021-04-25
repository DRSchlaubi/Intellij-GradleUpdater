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

package me.schlaubi.intellij_gradle_version_checker.inspection.dependencies.kotlin.unnecessary_coordinates

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import me.schlaubi.intellij_gradle_version_checker.util.toPsiTemplate
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.buildValueArgumentList

class ReplaceWithKotlinNotationQuickfix(
    private val call: SmartPsiElementPointer<KtCallExpression>,
    private val module: String,
    private val version: SmartPsiElementPointer<KtExpression>?
) : LocalQuickFix {
    override fun getFamilyName(): String = "Replace"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val factory = KtPsiFactory(project)
        val callArguments = factory.buildValueArgumentList {
            appendFixedText("(")
            appendExpression(module.toPsiTemplate(factory))
            if (version != null) {
                appendFixedText(", ")
                appendExpression(version.element)
            }
            appendFixedText(")")
        }
        val callExpression = factory.createExpression("kotlin${callArguments.text}")
        val dependencyArguments = factory.buildValueArgumentList {
            appendFixedText("(")
            appendExpression(callExpression)
            appendFixedText(")")
        }

        call.element?.valueArgumentList?.replace(dependencyArguments)
    }
}
