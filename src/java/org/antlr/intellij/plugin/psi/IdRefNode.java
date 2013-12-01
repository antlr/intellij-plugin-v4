package org.antlr.intellij.plugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class IdRefNode extends ASTWrapperPsiElement {
	public IdRefNode(@NotNull ASTNode node) {
		super(node);
	}
}
