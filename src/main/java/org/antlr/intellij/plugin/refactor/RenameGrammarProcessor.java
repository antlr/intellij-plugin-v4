package org.antlr.intellij.plugin.refactor;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.psi.GrammarElementRefNode;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.resolve.TokenVocabResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;

public class RenameGrammarProcessor extends RenamePsiElementProcessor {

    @Override
    public boolean canProcessElement(@NotNull PsiElement psiElement) {
        return (psiElement instanceof GrammarElementRefNode) ||
               (psiElement instanceof ANTLRv4FileRoot);
    }

    @Override
    public boolean isToSearchInComments(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public boolean isToSearchForTextOccurrences(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public void renameElement(@NotNull PsiElement element, @NotNull String newName, UsageInfo @NotNull [] usages, @Nullable RefactoringElementListener listener) throws IncorrectOperationException {

        if (element instanceof ANTLRv4FileRoot) {
            ANTLRv4FileRoot root = (ANTLRv4FileRoot) element;
            String oldName = root.getName();
            root.setName(newName);
            ((PsiNamedElement) root.getChildren()[0].getChildren()[1].getFirstChild()).setName(newName);

            // Rename the token-vocab file if found
            VirtualFile tokenVocabFile = TokenVocabResolver.findRelativeTokenFile(oldName, root);
            if (tokenVocabFile != null) {
                try {
                    tokenVocabFile.rename(this, newName + ".tokens");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        } else if (element instanceof GrammarElementRefNode &&
                MyPsiUtils.isGrammarName(element) &&
                !MyPsiUtils.isGrammarNameVocabOption(element)) {

            GrammarElementRefNode node = (GrammarElementRefNode) element;
            PsiFile file = node.getContainingFile();
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            file.setName(dotIndex >= 0 ? newName + "." + fileName.substring(dotIndex + 1) : newName);
            node.replaceWithText(newName);

        } else {
            super.renameElement(element, newName, usages, listener);
        }

        for (UsageInfo usage : usages) {
            if (MyPsiUtils.isGrammarNameVocabOption(usage.getElement())) {
                super.renameElement(element, newName, usages, listener);
            }
        }

    }

}
