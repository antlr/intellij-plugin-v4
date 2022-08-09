package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Refs to tokens, rules
 */
public abstract class GrammarElementRefNode extends LeafPsiElement implements PsiNamedElement {
	protected String name = null; // an override to input text ID if we rename via intellij

	public GrammarElementRefNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public String getName() {
		if ( name!=null ) return name;
		return getText();
	}

	@Override
	public PsiElement setName(@NotNull String newName) {
		name = newName;
		replace(MyPsiUtils.createLeafFromText(getProject(),
				getContext(),
				newName,
				ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(org.antlr.intellij.plugin.parser.ANTLRv4Lexer.TOKEN_REF)));
		return this;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getElementType().toString() + ")";
	}
}
