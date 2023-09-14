package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;

public class LexerRuleRefNode extends GrammarElementRefNode {
	public LexerRuleRefNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public PsiReference getReference() {
		if (isDeclaration()) {
			return null;
		}
		return new GrammarElementRef(this, getText());
	}

	private boolean isDeclaration() {
		PsiElement parent = getParent();
        return parent instanceof LexerRuleSpecNode
				|| parent instanceof TokenSpecNode
				|| parent instanceof ChannelSpecNode;
	}
}
