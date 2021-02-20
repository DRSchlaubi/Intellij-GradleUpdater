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

package me.schlaubi.intellij_gradle_version_checker.copy

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import me.schlaubi.intellij_gradle_version_checker.KtsPasteFromGroovyDialog
import okhttp3.internal.toImmutableList
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

// https://regex101.com/r/kZ258U/3
private val DEPENDENCY_DECLARATION_REGEX =
    """(\w+)\s+(?:group:\s+)?["'](.+?)(?::|(?:(?:['"]),\s*(?:artifact:\s*)?(?:['"])))(.+?)(?:(?::|(?:(?:['"]),\s*(?:version:\s*)?(?:['"])))(.*))?(?:['"])""".toRegex()

// https://regex101.com/r/lEKNuH/1/
private val PLUGIN_DECLARATION_REGEX =
    "id\\s*['\"]([\\w.]*)['\"](?:\\s* version\\s*['\"]([\\w.-A-Za-z]*)['\"])?".toRegex()

private class MyTransferableData(val text: String) : TextBlockTransferableData {

    override fun getFlavor() = DATA_FLAVOR
    override fun getOffsetCount() = 0

    override fun getOffsets(offsets: IntArray?, index: Int) = index
    override fun setOffsets(offsets: IntArray?, index: Int) = index

    companion object {
        val DATA_FLAVOR: DataFlavor =
            DataFlavor(ConvertGroovyCopyPasteProcessor::class.java, "class: ConvertGroovyCopyPasteProcessor")
    }
}

private fun PsiFile.isGradleFile() = (this as? KtFile)?.resolveImportReference(
    FqName("org.gradle.api.artifacts.dsl.DependencyHandler")
)?.isNotEmpty() == true

class ConvertGroovyCopyPasteProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    override fun collectTransferableData(
        file: PsiFile,
        editor: Editor,
        startOffsets: IntArray,
        endOffsets: IntArray
    ): List<TextBlockTransferableData> = emptyList()

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
        try {
            if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val text = content.getTransferData(DataFlavor.stringFlavor) as String
                return listOf(MyTransferableData(text))
            }
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e
        }
        return emptyList()
    }

    override fun processTransferableData(
        project: Project,
        editor: Editor,
        bounds: RangeMarker,
        caretOffset: Int,
        indented: Ref<Boolean>,
        values: List<TextBlockTransferableData>
    ) {
        if (DumbService.getInstance(project).isDumb) return
        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? KtFile ?: return
        if (!targetFile.isGradleFile()) return

        val data = values.single() as MyTransferableData
        val text = TextBlockTransferable.convertLineSeparators(editor, data.text, values)

        val dependencies = DEPENDENCY_DECLARATION_REGEX.findAll(text).toList().mapToDependencies()
        val plugins = PLUGIN_DECLARATION_REGEX.findAll(text).toList().mapToPlugins()
        val items = dependencies + plugins
        var end = bounds.endOffset
        // We're making this lazy so if we find dependencies and plugins we replace them first
        // and the recalculate '/" as the other migrations already replace them
        // if not we just replace them
        val singleQuoteStrings by lazy {
            targetFile.elementsInRange(TextRange.from(bounds.startOffset, end))
                .flatMap { PsiTreeUtil.findChildrenOfType(it, KtConstantExpression::class.java) }
                .asSequence()
                .filter { it.elementType == KtNodeTypes.CHARACTER_CONSTANT }
                .filter { it.textLength != 3 } // 3 chars = 1 char + 2 ' => valid char, 2 chars however is just '' which is not a valid char
                .toList()
        }
        if (items.isEmpty() && singleQuoteStrings.isEmpty()) return

        if (!confirmConversion(project)) return

        end = items.replace(editor.document, bounds.startOffset)

        if (items.isNotEmpty()) {
            targetFile.commitAndUnblockDocument() // commit original changes to not re migrate "/'
        }

        runWriteAction {
            singleQuoteStrings.forEach {
                    it.replace(
                        KtPsiFactory(project).createStringTemplate(
                            it.text.substring(
                                1,
                                it.text.length - 1
                            )
                        )
                    )
                }
        }
    }
}

private val PsiElement.everyChildren: List<PsiElement>
    get() {
        val list = mutableListOf<PsiElement>()


        return list.toImmutableList()
    }

private fun <T : Migratable> List<T>.replace(
    document: Document,
    start: Int,
) = runWriteAction {
    fold(start) { startOffset, item ->
        val range = item.range
        val migrated = item.migrateString()
        val offset = migrated.length - (range.last - range.first)
        val realRange = range.withOffset(startOffset)

        document.replaceString(
            realRange.first,
            realRange.last + 1, // End is exclusive
            migrated
        )

        startOffset + offset - 1
    }
}

interface Migratable {
    val range: IntRange
    fun migrateString(): String
}

private data class DependencyDeclaration(
    val config: String,
    val group: String,
    val artifact: String,
    val version: String?,
    override val range: IntRange
) : Migratable {
    override fun migrateString(): String = """$config("$group:$artifact${version?.let { ":$version" } ?: ""}")"""

    companion object {
        fun fromMatch(match: MatchResult): DependencyDeclaration {
            val (config, group, artifact) = match.destructured
            val version = match.groupValues[4].asNullable()

            return DependencyDeclaration(config, group, artifact, version, match.range)
        }
    }
}

private data class PluginDeclaration(val id: String, val version: String?, override val range: IntRange) :
    Migratable {
    override fun migrateString(): String {
        return if (id in gradleOfficialPlugins) {
            if ("-" in id) {
                "`$id`"
            } else id
        } else {
            buildString {
                val kotlinId = id.substringAfter("org.jetbrains.kotlin.")
                if (kotlinId != id) {
                    // 'kotlin("<module>")'
                    append("kotlin").append('(').append('"').append(kotlinId).append('"').append(')')
                } else {
                    // 'id("<id>")
                    append("id").append('(').append('"').append(kotlinId).append('"').append(')')
                }
                if (version != null) {
                    // ' version "<version>"
                    append(' ').append("version").append(' ').append('"').append(version).append('"')
                }
            }
        }
    }

    companion object {
        fun fromMatch(matchResult: MatchResult): PluginDeclaration {
            val (id, version) = matchResult.destructured
            return PluginDeclaration(id, version.asNullable(), matchResult.range)
        }
    }
}

fun IntRange.withOffset(offset: Int) = (first + offset)..(last + offset)

private fun Iterable<MatchResult>.mapToDependencies() = map(DependencyDeclaration::fromMatch)
private fun Iterable<MatchResult>.mapToPlugins() = map(PluginDeclaration::fromMatch)

private fun confirmConversion(project: Project): Boolean {
    val dialog =
        KtsPasteFromGroovyDialog(project)
    dialog.show()
    return dialog.isOK
}

fun String.asNullable() = if (isBlank()) null else this

val gradleOfficialPlugins = listOf(
    "project-report",
    "project-reports",
    "help-tasks",
    "binary-base",
    "component-base",
    "language-base",
    "lifecycle-base",
    "build-dashboard",
    "reporting-base",
    "java-lang",
    "jvm-resources",
    "jvm-component",
    "application",
    "base",
    "distribution",
    "groovy-base",
    "groovy",
    "java-base",
    "java-library-distribution",
    "java-library",
    "java-platform",
    "java-test-fixtures",
    "java",
    "jvm-ecosystem",
    "version-catalog",
    "war",
    "junit-test-suite",
    "publishing",
    "antlr",
    "build-init",
    "wrapper",
    "checkstyle",
    "codenarc",
    "pmd",
    "ear",
    "eclipse-wtp",
    "eclipse",
    "idea",
    "visual-studio",
    "xcode",
    "play-ide",
    "ivy-publish",
    "jacoco",
    "coffeescript-base",
    "envjs",
    "javascript-base",
    "jshint",
    "rhino",
    "assembler-lang",
    "assembler",
    "c-lang",
    "c",
    "cpp-application",
    "cpp-lang",
    "cpp-library",
    "cpp",
    "objective-c-lang",
    "objective-c",
    "objective-cpp-lang",
    "objective-cpp",
    "swift-application",
    "swift-library",
    "swiftpm-export",
    "windows-resource-script",
    "windows-resources",
    "scala-lang",
    "maven-publish",
    "maven",
    "clang-compiler",
    "gcc-compiler",
    "microsoft-visual-cpp-compiler",
    "native-component-model",
    "native-component",
    "standard-tool-chains",
    "play-application",
    "play-coffeescript",
    "play-javascript",
    "play",
    "groovy-gradle-plugin",
    "java-gradle-plugin",
    "scala-base",
    "scala",
    "signing",
    "cpp-unit-test",
    "cunit-test-suite",
    "cunit",
    "google-test-test-suite",
    "google-test",
    "xctest"
)

