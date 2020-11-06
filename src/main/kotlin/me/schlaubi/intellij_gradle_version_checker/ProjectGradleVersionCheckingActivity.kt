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

package me.schlaubi.intellij_gradle_version_checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.shelf.ShelvedChangesViewManager
import me.schlaubi.intellij_gradle_version_checker.settings.ApplicationGradleVersionSettings
import me.schlaubi.intellij_gradle_version_checker.settings.ProjectPersistentGradleVersionSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.gradle.util.GradleVersion as GGradleVersion

class ProjectGradleVersionCheckingActivity : ShelvedChangesViewManager.PostStartupActivity() {

    override fun runActivity(project: Project) {
        if (ApplicationGradleVersionSettings.ignoreOutdatedVersion || ProjectPersistentGradleVersionSettings.getInstance(
                project
            ).ignoreOutdatedVersion
        ) return
        val gradleSettings = GradleSettings.getInstance(project).getLinkedProjectSettings(project.basePath!!) ?: return
        if (gradleSettings.distributionType?.isWrapped == false) return
        val currentGradleVersion = gradleSettings.resolveGradleVersion().asGradleVersion()
        if (latestGradleVersion.gradleVersion > currentGradleVersion) {
            GradleVersionNotifier.notifyOutdated(project)
        }
    }
}

private fun GGradleVersion.asGradleVersion(): GradleVersion =
    baseVersion.version.parseGradleVersion() ?: error("Invalid formatted gradle version")