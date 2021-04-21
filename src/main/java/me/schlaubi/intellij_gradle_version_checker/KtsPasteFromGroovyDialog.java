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

package me.schlaubi.intellij_gradle_version_checker;

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import me.schlaubi.intellij_gradle_version_checker.settings.ProjectPersistentGradleVersionSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("UnusedDeclaration")
public class KtsPasteFromGroovyDialog extends DialogWrapper {
    private JPanel panel;
    private JCheckBox donTShowThisCheckBox;
    private JLabel questionLabel;
    private JButton buttonOK;
    private final Project project;

    public KtsPasteFromGroovyDialog(@NotNull Project project) {
        super(project, true);
        this.project = project;
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle(GradleUpdaterBundle.getMessage("copypaste.onpaste.seems.like.groovy"));
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    @Override
    public Container getContentPane() {
        return panel;
    }

    @NotNull
    @Override
    protected Action @NotNull [] createActions() {
        setOKButtonText(CommonBundle.getYesButtonText());
        setCancelButtonText(CommonBundle.getNoButtonText());
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        if (donTShowThisCheckBox.isSelected()) {
            ProjectPersistentGradleVersionSettings.getInstance(project).setAlwaysConvertGroovy(true);
        }

        super.doOKAction();
    }
}
