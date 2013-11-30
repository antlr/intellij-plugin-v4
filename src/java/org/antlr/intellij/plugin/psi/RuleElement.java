package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RuleElement extends ANTLRv4PSIElement implements PsiNamedElement {
	protected String name = null; // an override to input text ID

	public RuleElement(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public String getName() {
		if ( name!=null ) return name;
		ASTNode nameNode =
			findChildByType(TokenSet.create(ANTLRv4TokenTypes.RULE_REF, ANTLRv4TokenTypes.TOKEN_REF));
		String text = nameNode.getText();
		return text;
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
		this.name = name;
		return this;
	}

	@Override
	public Icon getIcon(int flags) {
		return Icons.PARSER_RULE;
	}
}
