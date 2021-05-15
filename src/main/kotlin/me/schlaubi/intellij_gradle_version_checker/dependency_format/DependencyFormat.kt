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

package me.schlaubi.intellij_gradle_version_checker.dependency_format

import me.schlaubi.intellij_gradle_version_checker.util.calleeFunction
import me.schlaubi.intellij_gradle_version_checker.util.isSimple
import me.schlaubi.intellij_gradle_version_checker.util.simpleValue
import me.schlaubi.intellij_gradle_version_checker.util.toPsiTemplate
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*

val KtCallExpression.dependencyFormat: DependencyFormat?
    get() = DependencyFormat.forCall(this)

/**
 * Representation of the different Dependency formats that are available.
 */
sealed class DependencyFormat {

    /**
     * Checks whether this [KtCallExpression] adds a dependency in the specified format.
     */
    abstract fun KtCallExpression.isFromThisType(): Boolean

    /**
     * Checks whether the call in this format is considered to be auto-convertible
     * meaning if there is an consitent way to extract group, name and version out of this.
     */
    abstract fun KtCallExpression.isConvertible(): Boolean

    /**
     * Extracts the components of a dependency declaration
     * @see DependencyDeclaration
     * @throws IllegalArgumentException if [isConvertible] returns falls for this [KtCallExpression]
     */
    fun KtCallExpression.extractComponents(): DependencyDeclaration? {
        require(isConvertible()) { "Component needs to be convertible" }

        return extractComponentsInternally()
    }

    /**
     * Extracts all of the components of this call into a [DependencyDeclaration].
     * this implies that [isConvertible] already returned true
     */
    protected abstract fun KtCallExpression.extractComponentsInternally(): DependencyDeclaration?

    /**
     * Generates a [KtValueArgumentList] needed to declare [DependencyDeclaration] in this format.
     */
    abstract fun DependencyDeclaration.generateArguments(factory: KtPsiFactory): KtValueArgumentList

    object NotationDependencyFormat : DependencyFormat() {
        override fun KtCallExpression.isFromThisType(): Boolean {
            val function = calleeFunction ?: return false
            val parameters = function.valueParameters

            if (parameters.size != 1) return false

            val possibleNotation = parameters.first()
            if (!possibleNotation.isAny()
                // Passing in a function as kotlin() is not considered to be notation format
                || valueArguments.first().firstChild !is KtStringTemplateExpression
            ) return false

            if (parameters.size == 1) return true
            return parameters[1].isConfigureAction()
        }

        override fun KtCallExpression.isConvertible(): Boolean {
            val template = valueArguments.first().firstChild as? KtStringTemplateExpression ?: return false
            // No interpolation = we can just split at ":"
            if (!template.hasInterpolation()) return template.simpleValue.count { it == ':' } >= 1

            // Jetbrains and their cool dataflow analysis could probably analyze what all the string templates mean
            // and guess how to convert it but I am not JetBrains and I am lazy so you either do "group:name:$version"
            // or I will ignore you ok?
            if (template.entries.size > 2) return false
            // As stated above you either have all three : in the first entry and the 2nd is only the version or you're goofed
            // If you have no version and use string interpolation for name and group this also won't be supported
            return template.entries.first().text.count { it == ':' } == 2
        }

        override fun KtCallExpression.extractComponentsInternally(): DependencyDeclaration {
            val factory = KtPsiFactory(project)
            val template = valueArguments.first().firstChild as? KtStringTemplateExpression
                ?: error("This is not a notation declaration: $text")


            val components = template.simpleValue.split(':')
            val (group, name) = components
            if (template.isSimple()) {
                val version = components.getOrNull(2)

                return DependencyDeclaration(
                    group.toPsiTemplate(factory),
                    name.toPsiTemplate(factory),
                    version?.toPsiTemplate(factory)
                )
            } else {
                val version = template.entries.getOrNull(1)?.children!!.first() as KtExpression

                return DependencyDeclaration(
                    group.toPsiTemplate(factory),
                    name.toPsiTemplate(factory),
                    version
                )
            }
        }

        override fun DependencyDeclaration.generateArguments(factory: KtPsiFactory): KtValueArgumentList {
            fun StringBuilder.appendExpression(expression: KtExpression) {
                if ((expression as? KtStringTemplateExpression)?.isSimple() == true) {
                    append(expression.simpleValue)
                } else {
                    append('$')
                    if (expression.text.matches("[a-zA-Z_][\\w_]*".toRegex())) {
                        append(expression.text)
                    } else {
                        append('{')
                        append(expression.text)
                        append('}')
                    }
                }
            }

            val baseLine = buildString {
                appendExpression(group)
                append(':')
                appendExpression(name)
                if (version != null) {
                    append(':')
                    appendExpression(version)
                }
            }

            val string = factory.createStringTemplate(baseLine)
            return factory.buildValueArgumentList {
                appendFixedText("(")
                appendExpression(string)
                appendFixedText(")")
            }
        }
    }

    object PositionalDependencyFormat : DependencyFormat() {
        override fun KtCallExpression.isFromThisType(): Boolean {
            if (!isMultiArgumentDeclaration()) return false

            return valueArguments.none { it.isNamed() }
        }

        override fun KtCallExpression.isConvertible(): Boolean = true

        override fun KtCallExpression.extractComponentsInternally(): DependencyDeclaration {
            val (group, name) = valueArguments
            val version = valueArguments.getOrNull(2)

            return DependencyDeclaration(
                group.getArgumentExpression()!!,
                name.getArgumentExpression()!!,
                version?.getArgumentExpression()
            )
        }

        override fun DependencyDeclaration.generateArguments(factory: KtPsiFactory): KtValueArgumentList {
            return factory.buildValueArgumentList {
                appendFixedText("(")
                appendExpression(group)
                appendFixedText(", ")
                appendExpression(name)
                if (version != null) {
                    appendFixedText(", ")
                    appendExpression(version)
                }
                appendFixedText(")")
            }
        }
    }

    object NamedDependencyFormat : DependencyFormat() {
        override fun KtCallExpression.isFromThisType(): Boolean {
            if (!isMultiArgumentDeclaration()) return false

            return valueArguments.all { it.isNamed() }
        }

        // the biggest concern of in-convertibility is using string templates and no longer being able to distinct
        // group, name and version from each other
        // since all of the above are separate arguments here we can neglect that
        override fun KtCallExpression.isConvertible(): Boolean = true

        override fun KtCallExpression.extractComponentsInternally(): DependencyDeclaration? = extractNamedDeclaration()
        override fun DependencyDeclaration.generateArguments(factory: KtPsiFactory): KtValueArgumentList {
            return factory.buildValueArgumentList {
                appendFixedText("(")

                appendName(Name.identifier("group"))
                appendFixedText(" = ")
                appendExpression(group)
                appendFixedText(", ")

                appendName(Name.identifier("name"))
                appendFixedText(" = ")
                appendExpression(name)

                if (version != null) {
                    appendFixedText(", ")

                    appendName(Name.identifier("version"))
                    appendFixedText(" = ")
                    appendExpression(version)
                }
                appendFixedText(")")
            }
        }

    }

    object SemiNamedDependencyFormat : DependencyFormat() {

        // the biggest concern of in-convertibility is using string templates and no longer being able to distinct
        // group, name and version from each other
        // since all of the above are separate arguments here we can neglect that
        override fun KtCallExpression.isConvertible(): Boolean = true

        override fun KtCallExpression.isFromThisType(): Boolean {
            if (!isMultiArgumentDeclaration()) return false

            val namedArguments = valueArguments.count { it.isNamed() }
            return namedArguments != 0 && namedArguments != valueArguments.size // at least one but not all
        }

        override fun KtCallExpression.extractComponentsInternally(): DependencyDeclaration? = extractNamedDeclaration()
        override fun DependencyDeclaration.generateArguments(factory: KtPsiFactory): KtValueArgumentList =
            throw UnsupportedOperationException("This format is read-only")
    }

    companion object {

        /**
         * List of all Dependency formats.
         */
        val all: List<DependencyFormat> =
            listOf(
                NotationDependencyFormat,
                PositionalDependencyFormat,
                NamedDependencyFormat,
                SemiNamedDependencyFormat
            )

        fun forCall(call: KtCallExpression): DependencyFormat? {
            val candidates = all.filter { with(it) { call.isFromThisType() } }
            if (candidates.isEmpty()) {
                println("Could not find type for dependency: ${call.text}")
                return null
            }
            if (candidates.size > 1) {
                println("Multiple candidates found for dependency: ${call.text}")
                println("Candidates: $candidates")
            }
            return candidates.first()
        }
    }
}

private fun KtCallExpression.extractNamedDeclaration(): DependencyDeclaration? {
    // Figure out which parameter is for which argument
    val arguments = valueArguments.associateBy {
        it.getArgumentName()?.asName?.asString() ?: return null
    }

    return DependencyDeclaration(
        arguments["group"]?.getArgumentExpression()!!,
        arguments["name"]?.getArgumentExpression()!!,
        arguments["version"]?.getArgumentExpression()
    )
}

private fun KtCallExpression.isMultiArgumentDeclaration(): Boolean {
    val function = calleeFunction ?: return false
    val parameters = function.valueParameters

    // artifact + group is mandatory version is optional
    if (valueArguments.size !in 2..3) return false

    return parameters
        .take(valueArguments.size)
        .all { it.isString() } // all parameters need to be strings
}

private fun KtParameter.isString() =
    type()?.fqName?.asString() == "kotlin.String"

private fun KtParameter.isAny() =
    type()?.fqName?.asString() == "kotlin.Any"

private fun KtParameter.isConfigureAction(): Boolean {
    return type()?.fqName?.asString() == "org.gradle.api.Action" // is Action
            && typeParameters.firstOrNull()?.type()?.fqName
        ?.asString() == "org.gradle.api.artifacts.ExternalModuleDependency" // is Action<ExternalModuleDependency>
}
