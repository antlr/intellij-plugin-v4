package org.antlr.intellij.plugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class ParserRuleSpecNode extends ASTWrapperPsiElement {
	public ParserRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}
}

