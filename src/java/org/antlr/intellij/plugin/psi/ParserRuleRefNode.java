package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.ANTLRv4TokenType;
import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;

import javax.swing.*;

public class ParserRuleRefNode extends GrammarElementRefNode {
	public ParserRuleRefNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public ANTLRv4TokenType getRuleRefType() {
		return ANTLRv4TokenTypes.RULE_REF;
	}

	@Override
	public Icon getIcon(int flags) {
		return Icons.PARSER_RULE;
	}

	@Override
	public PsiReference getReference() {
		return new GrammarElementRef(this, getText());
	}
}
