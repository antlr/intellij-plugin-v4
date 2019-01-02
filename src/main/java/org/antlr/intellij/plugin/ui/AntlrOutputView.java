package org.antlr.intellij.plugin.ui;

import com.intellij.compiler.impl.CompilerErrorTreeView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.MessageCategory;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AntlrOutputView {

	private final CompilerErrorTreeView treeView;

	public AntlrOutputView(Project project) {
		treeView = new CompilerErrorTreeView(project, null);
	}

	public JComponent getComponent() {
		return treeView;
	}

	public void clearMessages(VirtualFile file) {
		treeView.getErrorViewStructure().removeGroup(file.getPresentableUrl());
	}

	public void addInfo(String message) {
		treeView.addMessage(MessageCategory.INFORMATION, new String[]{message}, null, -1, -1, null);
	}

	public void addError(String message, @Nullable VirtualFile grammarFile, int line, int column) {
		treeView.addMessage(MessageCategory.ERROR, new String[]{message}, grammarFile, line, column, null);
	}

	public void addWarning(String message, @Nullable VirtualFile grammarFile, int line, int column) {
		treeView.addMessage(MessageCategory.WARNING, new String[]{message}, grammarFile, line, column, null);
	}
}
