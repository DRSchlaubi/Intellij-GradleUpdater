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

package me.schlaubi.intellij_gradle_version_checker.inspection.wrapper

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.properties.psi.Property
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import me.schlaubi.intellij_gradle_version_checker.GradleUpdaterBundle
import me.schlaubi.intellij_gradle_version_checker.GradleVersion
import me.schlaubi.intellij_gradle_version_checker.latestGradleVersion
import me.schlaubi.intellij_gradle_version_checker.util.extractVersionFromDistributionUrlProperty

/**
 * Inspection inspecting gradle-wrapper.properties for outdated Gradle version.
 * @see LocalInspectionTool
 */
class GradleWrapperVersionInspection : LocalInspectionTool() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        val file = session.file.virtualFile

        if (!file.path.contains("gradle/wrapper") || file.name != "gradle-wrapper.properties" || file.extension != "properties") return PsiElementVisitor.EMPTY_VISITOR

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (
                    // Has to be a property
                    element !is Property ||
                    // has to be the "distributionUrl" property
                    element.name != WRAPPER_VERSION_PROPERTY
                ) return
                val value = element.value ?: return
                val gradleVersion = extractVersionFromDistributionUrlProperty(value) ?: return
                val comparison = latestGradleVersion.compareTo(gradleVersion)
                if (comparison != 0) {
                    val currentGradleVersion = gradleVersion.toString()
                    holder.registerProblem(
                        element,
                        GradleUpdaterBundle.getMessage(
                            "inspection.outdated_version.description",
                            latestGradleVersion
                        ),
                        when (comparison) {
                            GradleVersion.MAJOR -> ProblemHighlightType.LIKE_DEPRECATED
                            GradleVersion.MINOR -> ProblemHighlightType.WARNING
                            GradleVersion.REVISION -> ProblemHighlightType.WEAK_WARNING
                            GradleVersion.TOO_NEW -> ProblemHighlightType.ERROR
                            else -> error("Invalid severity: $comparison")
                        },
                        UpgradeGradleVersionQuickFix(
                            latestGradleVersion.toString(),
                            currentGradleVersion
                        ),
                        UpgradeGradleVersionAndSyncQuickFix(
                            latestGradleVersion.toString(),
                            currentGradleVersion
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val WRAPPER_VERSION_PROPERTY = "distributionUrl"
    }
}
