package org.antlr.intellij.plugin.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.psi.DelegateGrammarNode;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class ANTLRv4ImportCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
        PsiElement position = completionParameters.getPosition();
        DelegateGrammarNode delegateGrammarNode = PsiTreeUtil.getParentOfType(position, DelegateGrammarNode.class);
        if (delegateGrammarNode == null) return;

        PsiFile containingFile = completionParameters.getOriginalFile();
        PsiDirectory containingDirectory = containingFile.getContainingDirectory();
        if (containingDirectory == null) return; // scratch file case

        CompletionResultSet resultSet = completionResultSet.caseInsensitive();
        Arrays
                .stream(containingDirectory.getChildren())
                .map(psiElement -> {
                    if (psiElement instanceof ANTLRv4FileRoot) {
                        return (ANTLRv4FileRoot) psiElement;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(PsiFileImpl::getName)
                .filter(name -> !name.equals(containingFile.getName()))
                .map(name -> name.replace(".g4", ""))
                .forEach(name -> resultSet.addElement(LookupElementBuilder.create(name)));
    }
}
