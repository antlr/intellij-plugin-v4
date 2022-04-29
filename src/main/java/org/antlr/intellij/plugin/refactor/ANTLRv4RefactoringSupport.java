package org.antlr.intellij.plugin.refactor;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.antlr.intellij.plugin.psi.RuleSpecNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ANTLRv4RefactoringSupport extends RefactoringSupportProvider{
	
	public boolean isAvailable(@NotNull PsiElement context){
		return context.getLanguage().isKindOf(ANTLRv4Language.INSTANCE);
	}
	
	// variable in-place rename only applies to elements limited to one file
	public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement element, @Nullable PsiElement context){
		return element instanceof RuleSpecNode;
	}
}