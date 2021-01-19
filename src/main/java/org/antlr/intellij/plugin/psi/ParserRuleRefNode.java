package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;

public class ParserRuleRefNode extends GrammarElementRefNode {
	public ParserRuleRefNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public PsiReference getReference() {
		return new GrammarElementRef(this, getText());
	}
}
