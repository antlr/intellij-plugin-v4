package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.plugin.ANTLRv4TokenType;
import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class LexerRuleRefNode extends RuleRefNode {
	public LexerRuleRefNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public ANTLRv4TokenType getRuleRefType() {
		return ANTLRv4TokenTypes.TOKEN_REF;
	}

	@Override
	public Icon getIcon(int flags) {
		return Icons.LEXER_RULE;
	}

	@Override
	public PsiReference getReference() {
		return new RuleRef(this, getText());
	}
}
