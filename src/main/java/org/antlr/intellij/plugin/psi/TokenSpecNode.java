package org.antlr.intellij.plugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.jetbrains.annotations.NotNull;

/**
 * A token defined in the {@code tokens} section.
 */
public class TokenSpecNode extends RuleSpecNode {

	public TokenSpecNode(@NotNull ASTNode node) {
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

	public static class Factory implements PsiElementFactory {
		public static Factory INSTANCE = new Factory();

		@Override
		public PsiElement createElement(ASTNode node) {
			ASTNode idList = node.getTreeParent();
			ASTNode parent = null;

			if (idList != null) {
				parent = idList.getTreeParent();
			}
			if (parent != null) {
				if (parent.getElementType() == ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_tokensSpec)) {
					return new TokenSpecNode(node);
				}
				else if (parent.getElementType() == ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_channelsSpec)) {
					return new ChannelSpecNode(node);
				}
			}

			return new ASTWrapperPsiElement(node);
		}
	}
}
