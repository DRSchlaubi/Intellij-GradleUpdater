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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

// https://regex101.com/r/YS2HLg/1
private val KOTLIN_DEPENDENCY_DECLARATION =
    """(\w+)(?:\(|\s+)(?:group\s*=\s*)?["'](.+?)(?::|["'],\s*(?:artifact\s*=\s*)?["'])(.+?)(?:(?::|["'],\s*(?:version\s*=\s*)?["'])(.*))?["']\)?""".toRegex()

internal class ConvertDependencyFormatCopyPasteProcessor : GradleMigrateCopyPasteProcessor() {
    override fun processGradleData(
        text: String,
        bounds: RangeMarker,
        targetFile: KtFile,
        project: Project,
        editor: Editor
    ) {
        val psiFactory by lazy { KtPsiFactory(project) }

        val kotlinDependencyDeclarations = KOTLIN_DEPENDENCY_DECLARATION.findAll(text)
            .map { it.value to CopiedDependencyDeclaration.fromMatch(it) }
            .mapNotNull { (string, declaration) ->
                val newDeclaration = with(declaration) {
                    // Try migrating all to the project format and then compare which ones changed, because it's easier
                    project.migrateString(psiFactory)
                }

                if (string == newDeclaration) {
                    null // filter out declarations already conforming format
                } else {
                    MigratedMigratable(declaration.range, newDeclaration)
                }
            }

        kotlinDependencyDeclarations.asIterable().replace(project, psiFactory, editor.document, bounds.startOffset)
    }
}

private class MigratedMigratable(override val range: IntRange, private val migratedString: String) : Migratable {
    override fun Project.migrateString(factory: KtPsiFactory): String = migratedString
}
