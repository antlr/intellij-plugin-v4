package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;

public class LexerRuleSpecNode extends RuleSpecNode {
	public static final Logger LOG = Logger.getInstance("org.antlr.intellij.plugin.psi.LexerRuleSpecNode");
	public LexerRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}

	@Override
	public IElementType getRuleRefType() {
		return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF);
	}

	@Override
	public GrammarElementRefNode getNameIdentifier() {
		GrammarElementRefNode tr = PsiTreeUtil.getChildOfType(this, LexerRuleRefNode.class);
		if ( tr==null ) {
			LOG.error("can't find LexerRuleRefNode child of "+this.getText(), (Throwable)null);
		}
		return tr;
	}

	public boolean isFragment() {
		return getNode().findChildByType(ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.FRAGMENT)) != null;
	}

	public static class Factory implements PsiElementFactory {
		public static Factory INSTANCE = new Factory();

		@Override
		public PsiElement createElement(ASTNode node) {
			return new LexerRuleSpecNode(node);
		}
	}
}
