package org.antlr.intellij.plugin.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import org.antlr.intellij.plugin.psi.DelegateGrammarNode;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;

public class ANTLRv4CompletionContributor extends CompletionContributor {
    // a user can type in an upper case letter
    private final static PsiElementPattern.Capture<LexerRuleRefNode> LEXER_RULE_GRAMMAR_IMPORT_PATTERN =
            PlatformPatterns
                    .psiElement(LexerRuleRefNode.class)
                    .withSuperParent(2, DelegateGrammarNode.class);

    // a user can type in a lower case letter
    private final static PsiElementPattern.Capture<ParserRuleRefNode> PARSE_RULE_GRAMMAR_IMPORT_PATTERN =
            PlatformPatterns
                    .psiElement(ParserRuleRefNode.class)
                    .withSuperParent(2, DelegateGrammarNode.class);

    public ANTLRv4CompletionContributor() {
        extend(CompletionType.BASIC, LEXER_RULE_GRAMMAR_IMPORT_PATTERN, new ANTLRv4ImportCompletionProvider());
        extend(CompletionType.BASIC, PARSE_RULE_GRAMMAR_IMPORT_PATTERN, new ANTLRv4ImportCompletionProvider());
    }
}
