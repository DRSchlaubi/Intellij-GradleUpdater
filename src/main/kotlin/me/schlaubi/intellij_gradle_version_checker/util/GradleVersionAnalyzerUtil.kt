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

package me.schlaubi.intellij_gradle_version_checker.util

import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import me.schlaubi.intellij_gradle_version_checker.GradleVersion
import me.schlaubi.intellij_gradle_version_checker.inspection.wrapper.GradleWrapperVersionInspection
import me.schlaubi.intellij_gradle_version_checker.parseGradleVersion

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

/**
 * Extracts the Gradle version from the [distributionUrl property](property) in gradle-wrapper.properties.
 */
fun extractVersionFromDistributionUrlProperty(property: String): GradleVersion? {
    val fileName = property.substringAfterLast('/') // /gradle-6.3-bin.zip
    val version = fileName.substringAfter("gradle-").substringBefore(".zip")
    val (versionName /*, type */) = version.split("-")

    return versionName.parseGradleVersion()
}

internal fun Project.findGradleVersion(): Pair<GradleVersion, IProperty>? {
    val gradlePropertiesPath =
        guessProjectDir()
            ?.findChild("gradle")
            ?.findChild("wrapper")
            ?.findChild("gradle-wrapper.properties") ?: return null

    val gradlePropertiesFile = PsiManager.getInstance(requireNotNull(this))
        .findFile(gradlePropertiesPath) as? PropertiesFile ?: return null

    val property = gradlePropertiesFile
        .findPropertyByKey(GradleWrapperVersionInspection.WRAPPER_VERSION_PROPERTY) ?: return null

    val value = property.value ?: error("Well this is awkward")

    val version = extractVersionFromDistributionUrlProperty(value) ?: return null
    return version to property
}
