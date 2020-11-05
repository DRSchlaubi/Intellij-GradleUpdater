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

package me.schlaubi.intellij_gradle_version_checker.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.properties.psi.Property
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import me.schlaubi.intellij_gradle_version_checker.GradleUpdaterBundle

open class UpgradeGradleVersionQuickFix internal constructor(
    private val latestGradleVersion: String,
    private val currentGradleVersion: String
) :
    LocalQuickFix {
    override fun getFamilyName(): String = GradleUpdaterBundle.getMessage("quickfix.update_gradle.family_name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val property = descriptor.psiElement as? Property ?: error("This quick fix cannot be use outside of properties")
        val currentValue = property.value ?: error("Property does not have a value yet")

        // For some reason Property.setValue() escapes https\:// to https\\:// which causes gradle build to fail
        // So we replace the value manually to prevent escaping
        val oldNode = (property as PropertyImpl).valueNode as PropertyValueImpl
        val newNode = PropertyValueImpl(oldNode.elementType, currentValue.replace(currentGradleVersion, latestGradleVersion)).apply {
            CodeEditUtil.setNodeGenerated(this, true)
        }
        property.node.replaceChild(oldNode, newNode)
    }
 }
