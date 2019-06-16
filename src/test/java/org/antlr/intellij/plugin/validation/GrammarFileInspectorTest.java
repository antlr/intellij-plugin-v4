package org.antlr.intellij.plugin.validation;

import com.intellij.codeInspection.LocalQuickFix;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ErrorType;
import org.junit.Test;

import java.util.Optional;

import static org.antlr.v4.tool.ErrorType.IMPLICIT_TOKEN_DEFINITION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GrammarFileInspectorTest {

    private static final int OFFENDING_TOKEN_INDEX = 123;

    @Test
    public void shouldDetectOffendingTokenAmongIssues() {
        // given:
        Token offendingToken = mockToken(OFFENDING_TOKEN_INDEX);
        LexerRuleRefNode lexerRuleRefNode = mockLexerRuleRefNode(OFFENDING_TOKEN_INDEX);
        GrammarIssue grammarIssue = new GrammarIssue(new ANTLRMessage(IMPLICIT_TOKEN_DEFINITION, offendingToken));
        grammarIssue.getOffendingTokens().add(offendingToken);

        // when:
        assertTrue(GrammarFileInspector.isAmongOffendingTokens(lexerRuleRefNode, grammarIssue));
    }

    @Test
    public void shouldReturnFalseWhenOffendingTokenDoesNotMatchOffsetWithLexerRuleRefNode() {
        // given:
        Token offendingToken = mockToken(OFFENDING_TOKEN_INDEX + 1);
        LexerRuleRefNode lexerRuleRefNode = mockLexerRuleRefNode(OFFENDING_TOKEN_INDEX);
        GrammarIssue grammarIssue = new GrammarIssue(new ANTLRMessage(IMPLICIT_TOKEN_DEFINITION, offendingToken));

        // when:
        assertFalse(GrammarFileInspector.isAmongOffendingTokens(lexerRuleRefNode, grammarIssue));
    }

    private LexerRuleRefNode mockLexerRuleRefNode(int offendingTokenIndex) {
        LexerRuleRefNode lexerRuleRefNode = mock(LexerRuleRefNode.class);
        when(lexerRuleRefNode.getTextOffset()).thenReturn(offendingTokenIndex);
        when(lexerRuleRefNode.isValid()).thenReturn(true);
        return lexerRuleRefNode;
    }

    private CommonToken mockToken(int tokenIndex) {
        CommonToken offendingToken = mock(CommonToken.class);
        when(offendingToken.getStartIndex()).thenReturn(tokenIndex);
        return offendingToken;
    }

    @Test
    public void shouldReturnEmptyOptionalForUnsupportedErrorType() {
        // when:
        LexerRuleRefNode lexerRuleRefNode = mock(LexerRuleRefNode.class);
        when(lexerRuleRefNode.isValid()).thenReturn(true);
        Optional<LocalQuickFix> quickFix = GrammarFileInspector.getFix(mock(LexerRuleRefNode.class), ErrorType.INTERNAL_ERROR);

        // then:
        assertFalse(quickFix.isPresent());
    }
}