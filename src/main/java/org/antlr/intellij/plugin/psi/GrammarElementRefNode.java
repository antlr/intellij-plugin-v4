package org.antlr.intellij.plugin.psi;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * Refs to tokens, rules
 */
public abstract class GrammarElementRefNode extends LeafPsiElement {
	public GrammarElementRefNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public String getName() {
		return getText();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getElementType() + ")";
	}
}
