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

package me.schlaubi.intellij_gradle_version_checker.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * [Configurable] for [ApplicationGradleVersionSettings].
 */
class ApplicationGradleVersionSettingsConfigurable :
    AbstractGradleVersionApplicationSettingsConfigurable({ ApplicationGradleVersionSettings })

/**
 * [Configurable] for [ProjectPersistentGradleVersionSettings].
 */
class ProjectGradleVersionSettingsConfigurable(project: Project) :
    AbstractGradleVersionApplicationSettingsConfigurable({ ProjectPersistentGradleVersionSettings.getInstance(project) })

/**
 * Base class for [Configurable] configuring [GradleVersionSettings].
 */
sealed class AbstractGradleVersionApplicationSettingsConfigurable(
    private val settingsProvider: () -> GradleVersionSettings
) : Configurable {

    private val originSettings = settingsProvider()
    private val settings = originSettings.asMemoryCopy()
    private val ignoreVersionBox = JBCheckBox("Check Gradle version on project load", settings.ignoreOutdatedVersion)
    private val alwaysConvertGroovyBox =
        JBCheckBox("Always convert Gradle to Groovy in Gradle files", settings.alwaysConvertGroovy)

    override fun createComponent(): JComponent? = JPanel().apply panel@{
        layout = BoxLayout(this@panel, BoxLayout.Y_AXIS)

        ignoreVersionBox.addChangeListener {
            val box = it.source as JBCheckBox
            settings.ignoreOutdatedVersion = box.isSelected
        }

        alwaysConvertGroovyBox.addChangeListener {
            val box = it.source as JBCheckBox
            settings.ignoreOutdatedVersion = box.isSelected
        }
        add(alwaysConvertGroovyBox)
    }

    override fun reset() {
        ignoreVersionBox.isSelected = settingsProvider().ignoreOutdatedVersion
    }

    override fun isModified(): Boolean = settingsProvider().ignoreOutdatedVersion != settings.ignoreOutdatedVersion

    override fun apply() {
        settingsProvider().ignoreOutdatedVersion = settings.ignoreOutdatedVersion
    }

    override fun getDisplayName(): String = "Gradle Version Updater"
}
