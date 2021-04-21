package me.schlaubi.intellij_gradle_version_checker

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
