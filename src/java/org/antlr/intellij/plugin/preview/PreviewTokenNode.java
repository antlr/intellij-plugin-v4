package org.antlr.intellij.plugin.preview;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

public class PreviewTokenNode extends LeafPsiElement {
	public PreviewTokenNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public String getName() {
		return getText();
	}
}
