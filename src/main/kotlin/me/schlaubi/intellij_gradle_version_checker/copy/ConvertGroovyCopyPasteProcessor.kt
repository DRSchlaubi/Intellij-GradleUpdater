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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import me.schlaubi.intellij_gradle_version_checker.KtsPasteFromGroovyDialog
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

// https://regex101.com/r/1OMz6a/1
private val DECLARATION_REGEX =
    """(\w+)\s+(?:group:\s*)?["']([^"':]+)(?::|["'],\s*(?:artifact:\s*)?['"])([^"':]+)(?::|['"],\s*(?:version:\s*)?['"])?([^"':]+)?(?:['"])""".toRegex()

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

        val dependencies = DECLARATION_REGEX.findAll(text).toList().mapToDependencies()
        if (dependencies.isEmpty()) return

        if (!confirmConversion(project)) return

        runWriteAction {
            dependencies.fold(bounds.startOffset) { startOffset, dependency ->
                val range = dependency.range
                val migratedDependency = dependency.toString()
                val offset = migratedDependency.length - (range.last - range.first)
                val realRange = range.withOffset(startOffset)

                editor.document.replaceString(
                    realRange.first,
                    realRange.last + 1, // End is exclusive
                    migratedDependency
                )

                startOffset + offset - 1
            }
        }
    }
}

private data class DependencyDeclaration(
    val config: String,
    val group: String,
    val artifact: String,
    val version: String?,
    val range: IntRange
) {
    override fun toString(): String = """$config("$group:$artifact${version?.let { ":$version" } ?: ""}")"""

    companion object {
        fun fromMatch(match: MatchResult): DependencyDeclaration {
            val (config, group, artifact) = match.destructured
            val version = match.groupValues[4].asNullable()

            return DependencyDeclaration(config, group, artifact, version, match.range)
        }
    }
}

fun IntRange.withOffset(offset: Int) = (first + offset)..(last + offset)

private fun Iterable<MatchResult>.mapToDependencies() = map(DependencyDeclaration::fromMatch)

private fun confirmConversion(project: Project): Boolean {
    val dialog =
        KtsPasteFromGroovyDialog(project)
    dialog.show()
    return dialog.isOK
}

fun String.asNullable() = if (isBlank()) null else this
