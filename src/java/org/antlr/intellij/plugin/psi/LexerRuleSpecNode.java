package org.antlr.intellij.plugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class LexerRuleSpecNode extends ASTWrapperPsiElement {
	public LexerRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}
}
