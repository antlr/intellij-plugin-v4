package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.jetbrains.annotations.NotNull;

public class LexerRuleSpecNode extends RuleSpecNode {
	public LexerRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}

	@Override
	public IElementType getRuleRefType() {
		return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF);
	}

	@Override
	public GrammarElementRefNode getId() {
		return PsiTreeUtil.getChildOfType(this, LexerRuleRefNode.class);
	}
}
