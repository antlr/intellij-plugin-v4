package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.RULE_ELEMENT_TYPES;
import static org.antlr.intellij.plugin.parser.ANTLRv4Parser.RULE_optionValue;

public class StringLiteralElement extends LeafPsiElement {
	public StringLiteralElement(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public PsiReference getReference() {
		PsiElement parent = getParent();

		if ( parent!=null && parent.getNode().getElementType()==RULE_ELEMENT_TYPES.get(RULE_optionValue) ) {
			return new StringLiteralRef(this);
		}

		return super.getReference();
	}
}
