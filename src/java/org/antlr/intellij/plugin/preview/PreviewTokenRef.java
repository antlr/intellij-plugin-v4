package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreviewTokenRef extends PsiReferenceBase<PreviewTokenNode> {
	public PreviewTokenRef(PreviewTokenNode element, TextRange range) {
		super(element, range);
	}

	@Nullable
	@Override
	public PsiElement resolve() {
		return getElement(); // return same node
	}

	@NotNull
	@Override
	public Object[] getVariants() {
//		return new PsiElement[] {getElement()};
//		return new String[] {"hi","mom"};
		return new Object[0];
	}
}
