package org.antlr.intellij.plugin.refactor;

import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenameInputValidator;
import com.intellij.util.ProcessingContext;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.jetbrains.annotations.NotNull;
import static com.intellij.patterns.PlatformPatterns.psiElement;

public class GrammarRenameInputValidator implements RenameInputValidator {

    @Override
    public @NotNull ElementPattern<? extends PsiElement> getPattern() {
        return psiElement(ANTLRv4FileRoot.class);
    }

    @Override
    public boolean isInputValid(@NotNull String newName, PsiElement element, ProcessingContext context) {
        return !newName.contains(" ");
    }
}
