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

package me.schlaubi.intellij_gradle_version_checker.util

import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings

/**
 * Invokes a IntelliJ Gradle sync.
 */
@Suppress("UnstableApiUsage")
fun Project.refreshGradle() = ExternalSystemProjectTracker.getInstance(this).scheduleProjectRefresh()

fun KtCallExpression.isDependencyDeclaration(
    gradleExtensionsSettings: GradleExtensionsSettings.GradleExtensionsData? = null
): Boolean {
    val callee = calleeFunction
    if (callee == null) { // Custom declarations
        val descriptor = calleeExpression?.resolveMainReferenceToDescriptors()?.firstOrNull() as? VariableDescriptor
            ?: return false

        return descriptor.type.fqName?.asString() == "org.gradle.api.artifacts.Configuration"
    } else if (callee.greenStub?.isExtension() == true) { // built-ins
        val children = PsiTreeUtil.findChildrenOfType(callee, KtUserType::class.java).asSequence().map { it.text }
        return children.any {
            it == "org.gradle.api.artifacts.dsl.DependencyHandler"
        } && (gradleExtensionsSettings == null || callee.name in gradleExtensionsSettings.configurations.keys)
    }

    return false
}

