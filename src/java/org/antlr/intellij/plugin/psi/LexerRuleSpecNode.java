package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class LexerRuleSpecNode extends ANTLRPsiNamedElement {
	public LexerRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}

	@Override
	public String getName() {
		LexerRuleRefNode rname = PsiTreeUtil.getChildOfType(this, LexerRuleRefNode.class);
		if ( rname!=null ) return rname.getText();
		return super.getName();
	}
}
