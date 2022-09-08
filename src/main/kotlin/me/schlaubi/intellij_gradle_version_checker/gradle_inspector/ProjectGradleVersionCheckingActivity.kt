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

package me.schlaubi.intellij_gradle_version_checker.gradle_inspector

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vcs.changes.shelf.ShelvedChangesViewManager
import me.schlaubi.intellij_gradle_version_checker.*
import me.schlaubi.intellij_gradle_version_checker.settings.ProjectPersistentGradleVersionSettings
import me.schlaubi.intellij_gradle_version_checker.util.findGradleVersion
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.gradle.util.GradleVersion as GGradleVersion

/**
 * [ShelvedChangesViewManager.PostStartupActivity] checking the Gradle version of the currently opened project
 * if not disabled in settings and notifies the users to update it's Gradle version.
 */
class ProjectGradleVersionCheckingActivity : StartupActivity.Background {

    override fun runActivity(project: Project) {
        if (ProjectPersistentGradleVersionSettings.getInstance(
                project
            ).ignoreOutdatedVersion
        ) return
        val gradleSettings = GradleSettings.getInstance(project).getLinkedProjectSettings(project.basePath!!) ?: return

        if (gradleSettings.distributionType?.isWrapped == false) return
        ApplicationManager.getApplication().runReadAction {
            val currentGradleVersion = project.moreEducatedVersionGuess(gradleSettings)
            try {
                if (latestGradleVersion > currentGradleVersion) {
                    GradleVersionNotifier.notifyOutdated(project)
                }
            } catch (ignored: UninitializedPropertyAccessException) {
                // if you install without restarting GradleVersionResolvingActivity doesn't run for whatever reason
            }
        }
    }
}

private fun GGradleVersion.asGradleVersion(): GradleVersion =
    baseVersion.version.parseGradleVersion() ?: error("Invalid formatted gradle version")

private fun Project.moreEducatedVersionGuess(settings: GradleProjectSettings): GradleVersion {
    val version = findGradleVersion()?.first
    if (version != null) {
        return version
    }
    return settings.resolveGradleVersion().asGradleVersion()
}
