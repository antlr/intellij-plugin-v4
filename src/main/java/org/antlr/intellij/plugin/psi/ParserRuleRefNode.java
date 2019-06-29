package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;

public class ParserRuleRefNode extends GrammarElementRefNode {
	public ParserRuleRefNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public IElementType getRuleRefType() {
		return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF);
	}

	@Override
	public PsiReference getReference() {
		return new GrammarElementRef(this, getText());
	}
}
