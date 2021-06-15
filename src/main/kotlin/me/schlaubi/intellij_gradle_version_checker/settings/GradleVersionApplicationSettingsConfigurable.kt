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

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import me.schlaubi.intellij_gradle_version_checker.dependency_format.DependencyFormat
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
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
    AbstractGradleVersionApplicationSettingsConfigurable(
        { ProjectPersistentGradleVersionSettings.getInstance(project) },
        project
    )

/**
 * Base class for [Configurable] configuring [GradleVersionSettings].
 */
sealed class AbstractGradleVersionApplicationSettingsConfigurable(
    private val settingsProvider: () -> GradleVersionSettings,
    private val project: Project? = null
) : Configurable {

    private val originSettings = settingsProvider()
    private val settings = originSettings.asMemoryCopy()
    private val ignoreVersionBox = JBCheckBox("Check Gradle version on project load", settings.ignoreOutdatedVersion)
    private val alwaysConvertGroovyBox =
        JBCheckBox("Always convert Gradle to Groovy in Gradle files", settings.alwaysConvertGroovy)
    private val formatSelector = ComboBox(
        DependencyFormat.all
            .asSequence()
            .filter { it !is DependencyFormat.SemiNamedDependencyFormat }
            .map { it.humanName }
            .toList()
            .toTypedArray()
    ).apply {
        preferredSize = Dimension(200, 25)
    }
    private var previewBox: EditorTextField? = null

    override fun createComponent(): JComponent? = JPanel(FlowLayout(FlowLayout.LEFT)).apply panel@{
        layout = BoxLayout(this@panel, BoxLayout.Y_AXIS)
        ignoreVersionBox.addChangeListener {
            val box = it.source as JBCheckBox
            settings.ignoreOutdatedVersion = box.isSelected
        }

        alwaysConvertGroovyBox.addChangeListener {
            val box = it.source as JBCheckBox
            settings.ignoreOutdatedVersion = box.isSelected
        }

        formatSelector.addActionListener {
            @Suppress("UNCHECKED_CAST")
            val selector = it.source as ComboBox<String>

            settings.dependencyFormat = selector.item
        }

        formatSelector.selectedItem = settingsProvider().dependencyFormat

        add(ignoreVersionBox)
        add(alwaysConvertGroovyBox)
        val formatSelection = JPanel()
        formatSelection.add(JLabel("Dependency format"))
        formatSelection.add(formatSelector)
        add(formatSelection)
    }

    override fun reset() {
        ignoreVersionBox.isSelected = settingsProvider().ignoreOutdatedVersion
        alwaysConvertGroovyBox.isSelected = settingsProvider().alwaysConvertGroovy
        formatSelector.selectedItem = settingsProvider().dependencyFormat
    }

    override fun isModified(): Boolean {
        return settingsProvider().ignoreOutdatedVersion != settings.ignoreOutdatedVersion
                || settingsProvider().alwaysConvertGroovy != settings.alwaysConvertGroovy
                || settingsProvider().dependencyFormat != settings.dependencyFormat
    }

    override fun apply() {
        settingsProvider().ignoreOutdatedVersion = settings.ignoreOutdatedVersion
        settingsProvider().alwaysConvertGroovy = alwaysConvertGroovyBox.isSelected
        settingsProvider().dependencyFormat = formatSelector.selectedItem as String
    }

    override fun getDisplayName(): String = "Gradle Version Updater"
}


private fun String.toDocument(): Document =
    EditorFactory.getInstance().createDocument(StringUtil.convertLineSeparators(this))