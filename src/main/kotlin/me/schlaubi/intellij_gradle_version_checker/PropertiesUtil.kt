package me.schlaubi.intellij_gradle_version_checker

import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil

fun IProperty.replace(old: String, new: String) {
    val currentValue = value ?: error("Property does not have a value yet")
    // For some reason Property.setValue() escapes https\:// to https\\:// which causes gradle build to fail
    // So we replace the value manually to prevent escaping
    val oldNode = (this as PropertyImpl).valueNode as PropertyValueImpl
    val newNode = PropertyValueImpl(
        oldNode.elementType,
        currentValue.replace(old, new)
    ).apply {
        CodeEditUtil.setNodeGenerated(this, true)
    }

    node.replaceChild(oldNode, newNode)
}
