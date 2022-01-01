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
import com.intellij.lang.properties.psi.PropertiesResourceBundleUtil
import com.intellij.lang.properties.psi.PropertyKeyValueFormat
import com.intellij.lang.properties.psi.impl.PropertyImpl

/**
 * Replaces the string [old] with [new] in this property.
 */
fun IProperty.replace(old: String, new: String, format: PropertyKeyValueFormat = PropertyKeyValueFormat.MEMORY) {
    val currentValue = value ?: error("Property does not have a value yet")

    val replacement =
        PropertiesResourceBundleUtil.convertValueToFileFormat(new, (this as PropertyImpl).keyValueDelimiter, format)

    setValue(currentValue.replace(old, replacement), PropertyKeyValueFormat.FILE)
}
