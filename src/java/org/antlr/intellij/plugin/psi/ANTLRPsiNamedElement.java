package org.antlr.intellij.plugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ANTLRPsiNamedElement extends ASTWrapperPsiElement implements PsiNamedElement {
	protected String name = null; // an override to input text ID

	public ANTLRPsiNamedElement(@NotNull final ASTNode node) {
		super(node);
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
}
