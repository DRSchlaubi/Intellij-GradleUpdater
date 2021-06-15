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

import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsActions
import com.intellij.psi.PsiManager
import me.schlaubi.intellij_gradle_version_checker.inspection.wrapper.GradleWrapperVersionInspection
import me.schlaubi.intellij_gradle_version_checker.settings.ApplicationGradleVersionSettings
import me.schlaubi.intellij_gradle_version_checker.settings.ProjectPersistentGradleVersionSettings
import me.schlaubi.intellij_gradle_version_checker.util.extractVersionFromDistributionUrlProperty
import me.schlaubi.intellij_gradle_version_checker.util.replace

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
                latestGradleVersion.toString()
            ),
            NotificationType.WARNING
        )

        notification.addAction(IgnoreForThisProjectAction(notification))
        notification.addAction(IgnoreAction(notification))
        notification.addAction(UpdateGradleVersionAction(notification))

        notification.notify(project)
    }
}

/**
 * [AnAction] on outdated Gradle version notification to Update the Gradle version.
 */
class UpdateGradleVersionAction(notification: Notification) : ExpiringAction(
    GradleUpdaterBundle.getMessage(
        "notification.outdated_version.actions.update.title",
        latestGradleVersion.toString()
    ),
    GradleUpdaterBundle.getMessage(
        "notification.outdated_version.actions.update.description",
        latestGradleVersion.toString()
    ),
    notification,
) {
    override fun perform(e: AnActionEvent) {
        val project = e.project ?: return
        val gradlePropertiesPath = project
            .guessProjectDir()
            ?.findChild("gradle")
            ?.findChild("wrapper")
            ?.findChild("gradle-wrapper.properties") ?: return

        val gradlePropertiesFile = PsiManager.getInstance(requireNotNull(e.project))
            .findFile(gradlePropertiesPath) as? PropertiesFile ?: return

        val property = gradlePropertiesFile
            .findPropertyByKey(GradleWrapperVersionInspection.WRAPPER_VERSION_PROPERTY) ?: return

        val value = property.value ?: error("Well this is awkward")

        val gradleVersion = extractVersionFromDistributionUrlProperty(value)?.toString() ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            property.replace(gradleVersion, latestGradleVersion.toString())
        }
    }
}

sealed class ExpiringAction(
    text: @NlsActions.ActionText String?,
    description: @NlsActions.ActionDescription String?,
    private val notification: Notification
) : AnAction(text, description, null) {
    final override fun actionPerformed(e: AnActionEvent) {
        perform(e)
        notification.expire()
    }

    abstract fun perform(e: AnActionEvent)
}

/**
 * [AnAction] on outdated Gradle version notification to mute this notification for this project.
 */
class IgnoreForThisProjectAction(notification: Notification) : ExpiringAction(
    GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_project.title"),
    GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_project.description"), notification
) {
    override fun perform(e: AnActionEvent) {
        val project = e.project ?: return
        ProjectPersistentGradleVersionSettings.getInstance(project).ignoreOutdatedVersion = true
    }
}

/**
 * [AnAction] on outdated Gradle version notification to mute this notification completely.
 */
class IgnoreAction(notification: Notification) :
    ExpiringAction(
        GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_application.title"),
        GradleUpdaterBundle.getMessage("notification.outdated_version.actions.ignore_application.description"),
        notification,
    ) {
    override fun perform(e: AnActionEvent) {
        ApplicationGradleVersionSettings.ignoreOutdatedVersion = true
    }
}
