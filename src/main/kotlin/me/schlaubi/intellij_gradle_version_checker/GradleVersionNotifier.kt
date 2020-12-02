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

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import me.schlaubi.intellij_gradle_version_checker.inspection.GradleWrapperVersionInspection
import me.schlaubi.intellij_gradle_version_checker.settings.ApplicationGradleVersionSettings
import me.schlaubi.intellij_gradle_version_checker.settings.ProjectPersistentGradleVersionSettings
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * Utility class to invoke Plugin notifications.
 */
object GradleVersionNotifier {


    /**
     * Sends a notification to [project] telling the user to update the project's Gradle version.
     */
    fun notifyOutdated(project: Project) {
        val group = NotificationGroupManager.getInstance()
            .getNotificationGroup("me.schlaubi.intellij_gradle_version_checker.notifications.OutdatedVersion")
        val notification = group.createNotification(
            GradleUpdaterBundle.getMessage("notification.outdated_version.title"),
            GradleUpdaterBundle.getMessage(
                "notification.outdated_version.description",
                latestGradleVersion.gradleVersion.toString()
            ),
            NotificationType.WARNING
        )
        notification.addAction(IgnoreForThisProjectAction())
        notification.addAction(IgnoreAction())
        notification.addAction(UpdateGradleVersionAction())

        notification.notify(project)
    }
}

/**
 * [AnAction] on outdated Gradle version notification to Update the Gradle version.
 */
class UpdateGradleVersionAction : AnAction(
    GradleUpdaterBundle.getMessage(
        "notification.outdated_version.actions.update.title",
        latestGradleVersion.gradleVersion.toString()
    ),
    GradleUpdaterBundle.getMessage(
        "notification.outdated_version.actions.update.description",
        latestGradleVersion.gradleVersion.toString()
    ),
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val gradlePropertiesFile = e.project!!.guessProjectDir()?.findChild("gradle")?.findChild("wrapper")
            ?.findChild("gradle-wrapper.properties") ?: return

        val gradleProperties = Properties().apply {
            load(ByteArrayInputStream(gradlePropertiesFile.contentsToByteArray()))
        }

        val value = gradleProperties[GradleWrapperVersionInspection.WRAPPER_VERSION_PROPERTY] as? String ?: return
        val gradleVersion = extractVersionFromDistributionUrlProperty(value) ?: return
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

/**
 * [AnAction] on outdated Gradle version notification to mute this notification for this project.
 */
class IgnoreForThisProjectAction : AnAction(
    GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_project.title"),
    GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_project.description"),
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ProjectPersistentGradleVersionSettings.getInstance(project).ignoreOutdatedVersion = true
    }
}

/**
 * [AnAction] on outdated Gradle version notification to mute this notification completely.
 */
class IgnoreAction :
    AnAction(
        GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_application.title"),
        GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_application.description"),
        null
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        ApplicationGradleVersionSettings.ignoreOutdatedVersion = true
    }
}
