package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class PreviewTokenNode extends LeafPsiElement {
	public PreviewTokenNode(IElementType type, CharSequence text) {
		super(type, text);
	}

	@Override
	public String getName() {
		return getText();
	}

	@Override
	public PsiReference getReference() {
		return new PreviewTokenRef(this, new TextRange(0, getText().length()));
	}

	@NotNull
	@Override
	public PsiReference[] getReferences() {
		return super.getReferences();
	}
}