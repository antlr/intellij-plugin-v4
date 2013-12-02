package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.plugin.Icons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ParserRuleRefNode extends ANTLRv4PSIElement implements PsiNamedElement {
	protected String name = null; // an override to input text ID if we rename via intellij

	public ParserRuleRefNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public String getName() {
		if ( name!=null ) return name;
		return getText();
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

	@Override
	public PsiReference getReference() {
		return new ParserRuleRef(this, getText());
	}
}
