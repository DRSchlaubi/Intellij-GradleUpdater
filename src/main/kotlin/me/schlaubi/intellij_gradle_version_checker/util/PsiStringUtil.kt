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

import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Whether this is a plain string or a interpolation template.
 */
fun KtStringTemplateExpression.isSimple() = entries.size == 1

/**
 * Returns the value of a string without interpolation
 */
val KtStringTemplateExpression.simpleValue: String
    get() = entries.firstOrNull()?.value ?: ""

/**
 * The value of a string template entry
 */
val KtStringTemplateEntry.value: String
    get() = text.removeSurrounding("\"")

/**
 * Converts a [String] into a [KtStringTemplateExpression].
 */
fun String.toPsiTemplate(factory: KtPsiFactory): KtStringTemplateExpression =
    factory.createStringTemplate(this)

fun KtStringTemplateExpression.equalsString(string: String) = isSimple() && simpleValue == string

fun KtStringTemplateExpression.startsWith(prefix: String) = isSimple() && simpleValue.startsWith(prefix)
