package org.antlr.intellij.plugin;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ANTLRv4FileRoot extends PsiFileBase {
    public ANTLRv4FileRoot(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, ANTLRv4Language.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return ANTLRv4FileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "ANTLR v4 grammar file";
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

    public GrammarSpecNode getGrammarSpec(){
        return PsiTreeUtil.getChildOfType(this, GrammarSpecNode.class);
    }
}
