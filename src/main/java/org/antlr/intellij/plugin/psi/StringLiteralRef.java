package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.antlr.intellij.plugin.resolve.TokenVocabResolver;
import org.jetbrains.annotations.Nullable;

public class StringLiteralRef extends PsiReferenceBase<StringLiteralElement> {

	public StringLiteralRef(StringLiteralElement node) {
		super(node, TextRange.from(1, node.getTextLength() - 2));
	}

	@Override
	public @Nullable PsiElement resolve() {
		return TokenVocabResolver.resolveTokenVocabFile(myElement);
	}
}
