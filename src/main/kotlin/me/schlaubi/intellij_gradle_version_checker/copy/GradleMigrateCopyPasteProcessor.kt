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
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.findParentOfType
import me.schlaubi.intellij_gradle_version_checker.dependency_format.DependencyDeclaration
import me.schlaubi.intellij_gradle_version_checker.util.calleeFunction
import me.schlaubi.intellij_gradle_version_checker.util.dependencyFormat
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

internal class MyTransferableData(val text: String) : TextBlockTransferableData {

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

internal sealed class GradleMigrateCopyPasteProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    final override fun collectTransferableData(
        file: PsiFile,
        editor: Editor,
        startOffsets: IntArray,
        endOffsets: IntArray
    ): List<TextBlockTransferableData> = emptyList()

    final override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
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

    final override fun processTransferableData(
        project: Project,
        editor: Editor,
        bounds: RangeMarker,
        caretOffset: Int,
        indented: Ref<in Boolean>,
        values: MutableList<out TextBlockTransferableData>
    ) {
        if (DumbService.getInstance(project).isDumb) return
        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? KtFile ?: return
        if (!targetFile.isGradleFile()) return

        val data = values.single() as MyTransferableData
        val text = TextBlockTransferable.convertLineSeparators(editor, data.text, values)

        val extensions = GradleExtensionsSettings.getInstance(project)
            .getExtensionsFor(targetFile.module) ?: return

        processGradleData(text, bounds, targetFile, extensions, project, editor)
    }

    abstract fun processGradleData(
        text: String,
        bounds: RangeMarker,
        targetFile: KtFile,
        gradleExtensionsSettings: GradleExtensionsSettings.GradleExtensionsData,
        project: Project,
        editor: Editor
    )

    protected fun <T : Migratable> Iterable<T>.replace(
        project: Project,
        psiFactory: KtPsiFactory,
        file: PsiFile,
        document: Document,
        start: Int,
    ) = runWriteAction {
        fold(start) { startOffset, item ->
            val range = item.range
            val migrated = with(item) {
                project.migrateString(psiFactory)
            }
            val offset = migrated.length - (range.last - range.first)
            val realRange = range.withOffset(startOffset)
            val psiAtOffset = file.findElementAt(realRange.first)
            val call = psiAtOffset?.findParentOfType<KtCallExpression>()

            if (call?.isGradleDependenciesCall() == true) {
                document.replaceString(
                    realRange.first,
                    realRange.last + 1, // End is exclusive
                    migrated
                )
            }

            startOffset + offset - 1
        }
    }
}

private fun KtCallExpression.isGradleDependenciesCall(): Boolean = calleeFunction
    ?.fqName
    .toString() == "org.gradle.kotlin.dsl.dependencies"

internal interface Migratable {
    val range: IntRange
    fun Project.migrateString(factory: KtPsiFactory): String
}

internal data class CopiedDependencyDeclaration(
    val config: String,
    val group: String,
    val artifact: String,
    val version: String?,
    override val range: IntRange
) : Migratable {
    override fun Project.migrateString(factory: KtPsiFactory): String {
        val expressionDeclaration = DependencyDeclaration(factory, group, artifact, version)
        val arguments = with(dependencyFormat) { expressionDeclaration.generateArguments(factory) }

        return "$config${arguments.text}"
    }

    companion object {
        fun fromMatch(match: MatchResult): CopiedDependencyDeclaration {
            val (config, group, artifact) = match.destructured
            val version = match.groupValues[4].asNullable()

            return CopiedDependencyDeclaration(config, group, artifact, version, match.range)
        }
    }
}
