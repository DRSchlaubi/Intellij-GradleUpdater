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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient
import me.schlaubi.intellij_gradle_version_checker.dependency_format.DependencyFormat

/**
 * Base class for persistent [GradleVersionSettings].
 *
 * @param T the type of the implementation
 * @see GradleVersionSettings
 * @see PersistentStateComponent
 */
sealed class AbstractPersistentGradleVersionSettings<T : GradleVersionSettings> :
    GradleVersionSettings,
    PersistentStateComponent<T> {

    override var ignoreOutdatedVersion = false

    override var alwaysConvertGroovy: Boolean = false

    override var dependencyFormat: String = DependencyFormat.NotationDependencyFormat::class.simpleName ?: "<error>"

    @Suppress("UNCHECKED_CAST")
    override fun getState(): T = this as T

    override fun loadState(state: T) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun asMemoryCopy(): GradleVersionSettings = MemoryGradleVersionSettings(this)
}

/**
 * Application wide implementation of [AbstractPersistentGradleVersionSettings].
 */
@State(
    name = "me.schlaubi.intellij_gradle_version_checker.settings.ApplicationPersistentGradleVersionSettings",
    storages = [Storage("GradleUpdaterPlugin.xml")]
)
class ApplicationPersistentGradleVersionSettings :
    AbstractPersistentGradleVersionSettings<ApplicationPersistentGradleVersionSettings>() {
    companion object {
        fun getInstance(): GradleVersionSettings =
            ApplicationManager.getApplication().getService(ApplicationPersistentGradleVersionSettings::class.java)

    }
}

/**
 * Project wide implementation of [AbstractPersistentGradleVersionSettings].
 */
@State(
    name = "me.schlaubi.intellij_gradle_version_checker.settings.ProjectPersistentGradleVersionSettings",
    storages = [Storage("GradleUpdaterPlugin.xml")]
)
class ProjectPersistentGradleVersionSettings :
    AbstractPersistentGradleVersionSettings<ProjectPersistentGradleVersionSettings>() {
    @Transient
    private val parent = ApplicationGradleVersionSettings

    override var ignoreOutdatedVersion: Boolean = parent.ignoreOutdatedVersion
    override var alwaysConvertGroovy: Boolean = parent.alwaysConvertGroovy
    override var dependencyFormat: String = parent.dependencyFormat

    companion object {
        @JvmStatic
        fun getInstance(project: Project): GradleVersionSettings =
            project.getService(ProjectPersistentGradleVersionSettings::class.java)
    }
}

/**
 * Non persistent implementation of [GradleVersionSettings].
 */
data class MemoryGradleVersionSettings(
    override var ignoreOutdatedVersion: Boolean = false,
    override var alwaysConvertGroovy: Boolean = false,
    override var dependencyFormat: String = DependencyFormat.NotationDependencyFormat::class.simpleName ?: "<error>"
) : GradleVersionSettings {
    constructor(settings: GradleVersionSettings) : this(settings.ignoreOutdatedVersion)

    override fun asMemoryCopy(): GradleVersionSettings = copy()
}

/**
 * Object delegating to [ApplicationPersistentGradleVersionSettings.getInstance].
 */
object ApplicationGradleVersionSettings :
    GradleVersionSettings by ApplicationPersistentGradleVersionSettings.getInstance()

/**
 * Interface for plugin settings.
 */
interface GradleVersionSettings {
    /**
     * Whether version notification should be enabled or not.
     */
    var ignoreOutdatedVersion: Boolean

    /**
     * Whether to ask if Groovy should be converted to Kotlin.
     */
    var alwaysConvertGroovy: Boolean

    /**
     * The name of the dependency format which is supposed to match all dependencies.
     */
    var dependencyFormat: String

    /**
     * Creates a non-persistent copy of this instance.
     *
     * @see MemoryGradleVersionSettings
     */
    fun asMemoryCopy(): GradleVersionSettings
}
