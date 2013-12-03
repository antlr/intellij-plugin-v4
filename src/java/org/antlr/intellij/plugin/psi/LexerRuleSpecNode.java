package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class LexerRuleSpecNode extends ANTLRPsiNamedElement {
	public LexerRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}
}
