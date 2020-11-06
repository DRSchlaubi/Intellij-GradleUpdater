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

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import me.schlaubi.intellij_gradle_version_checker.inspection.GradleWrapperVersionInspection
import me.schlaubi.intellij_gradle_version_checker.settings.ApplicationGradleVersionSettings
import me.schlaubi.intellij_gradle_version_checker.settings.ProjectPersistentGradleVersionSettings
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.*


object GradleVersionNotifier {

    private val group = NotificationGroup("Gradle Update Notification", NotificationDisplayType.BALLOON)

    fun notifyOutdated(project: Project) {
        val notification = group.createNotification(
            GradleUpdaterBundle.getMessage("notification.outdated_version.title"),
            GradleUpdaterBundle.getMessage("notification.outdated_version.description", latestGradleVersion.gradleVersion.toString()),
            NotificationType.WARNING
        )
        notification.addAction(IgnoreForThisProjectAction())
        notification.addAction(IgnoreAction())
        notification.addAction(UpdateGradleVersionAction())

        notification.notify(project)
    }
}

class UpdateGradleVersionAction : AnAction(GradleUpdaterBundle.getMessage("notification.outdated_version.actions.update", latestGradleVersion.gradleVersion.toString())) {
    override fun actionPerformed(e: AnActionEvent) {
        val gradlePropertiesFile = e.project!!.guessProjectDir()?.findChild("gradle")?.findChild("wrapper")
            ?.findChild("gradle-wrapper.properties") ?: return

        val gradleProperties = Properties().apply {
            load(ByteArrayInputStream(gradlePropertiesFile.contentsToByteArray()))
        }

        val value = gradleProperties[GradleWrapperVersionInspection.WRAPPER_VERSION_PROPERTY] as? String ?: return
        val fileName = value.substringAfterLast('/') // /gradle-6.3-bin.zip
        val version = fileName.substringAfter("gradle-").substringBefore(".zip")
        val (versionName, _ /* type */) = version.split("-")
        val gradleVersion = versionName.parseGradleVersion() ?: return
        val newGradleVersion = value.replace(gradleVersion.toString(), latestGradleVersion.gradleVersion.toString())
        gradleProperties[GradleWrapperVersionInspection.WRAPPER_VERSION_PROPERTY] = newGradleVersion

        val writer = StringWriter()
        val output = writer.use { gradleProperties.store(it, ""); it.toString() }

        FileChannel.open(gradlePropertiesFile.toNioPath(), StandardOpenOption.WRITE).use {
            it.write(ByteBuffer.wrap(output.toByteArray()))
        }

        gradlePropertiesFile.refresh(true, true)
    }
}

class IgnoreForThisProjectAction : AnAction(GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_project")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ProjectPersistentGradleVersionSettings.getInstance(project).ignoreOutdatedVersion = true
    }
}

class IgnoreAction : AnAction(GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_application")) {
    override fun actionPerformed(e: AnActionEvent) {
        ApplicationGradleVersionSettings.ignoreOutdatedVersion = true
    }
}
