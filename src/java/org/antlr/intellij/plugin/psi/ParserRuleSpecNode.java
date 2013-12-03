package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class ParserRuleSpecNode extends ANTLRPsiNamedElement {
	public ParserRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}

	@Override
	public String getName() {
		ParserRuleRefNode rname = PsiTreeUtil.getChildOfType(this, ParserRuleRefNode.class);
		if ( rname!=null ) return rname.getText();
		return super.getName();
	}
}

