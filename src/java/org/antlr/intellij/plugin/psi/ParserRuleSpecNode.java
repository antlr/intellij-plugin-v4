package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4TokenType;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.jetbrains.annotations.NotNull;

public class ParserRuleSpecNode extends RuleSpecNode {
	public ParserRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}

	@Override
	public ANTLRv4TokenType getRuleRefType() {
		return ANTLRv4TokenTypes.RULE_REF;
	}

	@Override
	public GrammarElementRefNode getId() {
		GrammarElementRefNode rr = PsiTreeUtil.getChildOfType(this, ParserRuleRefNode.class);
		if ( rr==null ) {
			System.err.println("null");
		}
		return rr;
	}
}

