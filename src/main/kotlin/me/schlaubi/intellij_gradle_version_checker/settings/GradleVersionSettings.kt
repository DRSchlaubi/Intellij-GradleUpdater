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

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

sealed class AbstractPersistentGradleVersionSettings<T : GradleVersionSettings> : GradleVersionSettings, PersistentStateComponent<T> {

    override var ignoreOutdatedVersion = false

    @Suppress("UNCHECKED_CAST")
    override fun getState(): T = this as T

    override fun loadState(state: T) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun asMemoryCopy(): GradleVersionSettings = MemoryGradleVersionSettings(this)

}

@State(
    name = "me.schlaubi.intellij_gradle_version_checker.settings.ApplicationPersistentGradleVersionSettings",
    storages = [Storage("GradleUpdaterPlugin.xml")]
)
class ApplicationPersistentGradleVersionSettings : AbstractPersistentGradleVersionSettings<ApplicationPersistentGradleVersionSettings>() {
    companion object {
        fun getInstance(): GradleVersionSettings = ServiceManager.getService(ApplicationPersistentGradleVersionSettings::class.java)
    }
}

@State(
    name = "me.schlaubi.intellij_gradle_version_checker.settings.ProjectPersistentGradleVersionSettings",
    storages = [Storage("GradleUpdaterPlugin.xml")]
)
class ProjectPersistentGradleVersionSettings : AbstractPersistentGradleVersionSettings<ProjectPersistentGradleVersionSettings>() {
    companion object {
        fun getInstance(project: Project): GradleVersionSettings = ServiceManager.getService(project, ProjectPersistentGradleVersionSettings::class.java)
    }
}

data class MemoryGradleVersionSettings(override var ignoreOutdatedVersion: Boolean = false) : GradleVersionSettings {
    constructor(settings: GradleVersionSettings) : this(settings.ignoreOutdatedVersion)

    override fun asMemoryCopy(): GradleVersionSettings = copy()

}

object ApplicationGradleVersionSettings : GradleVersionSettings by ApplicationPersistentGradleVersionSettings.getInstance()

interface GradleVersionSettings {
    var ignoreOutdatedVersion: Boolean

    fun asMemoryCopy(): GradleVersionSettings
}
