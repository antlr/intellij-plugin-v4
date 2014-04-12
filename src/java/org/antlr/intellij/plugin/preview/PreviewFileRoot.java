package org.antlr.intellij.plugin.preview;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.plugin.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PreviewFileRoot extends PsiFileBase {
    public PreviewFileRoot(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, PreviewLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return PreviewFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Sample input file";
    }

    @Override
    public Icon getIcon(int flags) {
		return Icons.FILE;
    }

	@NotNull
	@Override
	public PsiElement[] getChildren() {
		return super.getChildren();
	}
}
