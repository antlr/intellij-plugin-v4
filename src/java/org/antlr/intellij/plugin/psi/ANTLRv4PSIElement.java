package org.antlr.intellij.plugin.psi;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

/** Root of all PSI nodes for ANTLRv4 language except for file root. */
public class ANTLRv4PSIElement extends LeafPsiElement {
	public ANTLRv4PSIElement(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getElementType().toString() + ")";
	}
}
