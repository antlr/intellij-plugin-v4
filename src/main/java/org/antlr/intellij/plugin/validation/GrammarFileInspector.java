package org.antlr.intellij.plugin.validation;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.runtime.CommonToken;
import org.antlr.v4.tool.ErrorType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GrammarFileInspector extends PsiRecursiveElementVisitor {

    private final ProblemsHolder holder;

    private final List<GrammarIssue> grammarIssues;

    public GrammarFileInspector(ProblemsHolder holder, List<GrammarIssue> grammarIssues) {
        this.holder = Objects.requireNonNull(holder);
        this.grammarIssues = Objects.requireNonNull(grammarIssues);
    }

    @Override
    public void visitElement(PsiElement element) {
        if (element instanceof LexerRuleRefNode) {
            handleLeafElement((LexerRuleRefNode) element, holder);
        }
        super.visitElement(element);
    }

    private void handleLeafElement(LexerRuleRefNode element, ProblemsHolder holder) {
        if (!(element.getElementType() instanceof TokenIElementType)) {
            return;
        }

        TokenIElementType tokenIElementType = (TokenIElementType) element.getElementType();
        if (!"TOKEN_REF".equalsIgnoreCase(org.antlr.intellij.plugin.parser.ANTLRv4Lexer.VOCABULARY.getDisplayName(tokenIElementType.getANTLRTokenType()))) {
            return;
        }

        for (GrammarIssue grammarIssue : grammarIssues) {
            boolean isAmongOffendingTokens = isAmongOffendingTokens(element, grammarIssue);
            if (isAmongOffendingTokens) {
                Optional<LocalQuickFix> localQuickFix = getFix(element, grammarIssue.getMsg().getErrorType());

                if (localQuickFix.isPresent()) {
                    holder.registerProblem(element, grammarIssue.getAnnotation(), localQuickFix.get());
                } else {
                    holder.registerProblem(element, grammarIssue.getAnnotation());
                }
            }
        }

    }

    static boolean isAmongOffendingTokens(LexerRuleRefNode element, GrammarIssue grammarIssue) {
        return grammarIssue.getOffendingTokens().stream()
                .filter(t -> t instanceof CommonToken)
                .map(t -> (CommonToken) t)
                .map(CommonToken::getStartIndex)
                .anyMatch(idx -> idx == element.getTextOffset());
    }

    @NotNull
    static Optional<LocalQuickFix> getFix(LexerRuleRefNode element, ErrorType errorType) {
        if (errorType == ErrorType.IMPLICIT_TOKEN_DEFINITION){
            return Optional.of(new AddTokenDefinitionFix(element));
        }
        return Optional.empty();
    }

}
