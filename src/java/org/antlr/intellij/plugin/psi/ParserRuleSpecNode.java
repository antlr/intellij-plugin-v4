package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class ParserRuleSpecNode extends ANTLRPsiNamedElement {
	public ParserRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}
}

