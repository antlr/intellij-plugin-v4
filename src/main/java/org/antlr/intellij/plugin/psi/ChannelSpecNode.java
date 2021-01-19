package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;

/**
 * A channel defined in the {@code channels} section.
 */
public class ChannelSpecNode extends RuleSpecNode {

	public ChannelSpecNode(@NotNull ASTNode node) {
		super(node);
	}

	@Override
	public GrammarElementRefNode getNameIdentifier() {
		return PsiTreeUtil.getChildOfType(this, LexerRuleRefNode.class);
	}

	@Override
	public IElementType getRuleRefType() {
		return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF);
	}
}
