package org.antlr.intellij.plugin.actions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.util.TextRange;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.v4.tool.ErrorType;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnnotationIntentActionsFactoryTest {
    @Test
    public void shouldReturnEmptyOptionalForUnsupportedErrorType() {
        // when:
        LexerRuleRefNode lexerRuleRefNode = mock(LexerRuleRefNode.class);
        when(lexerRuleRefNode.isValid()).thenReturn(true);
        Optional<IntentionAction> quickFix = AnnotationIntentActionsFactory.getFix(new TextRange(0, 1), ErrorType.INTERNAL_ERROR, null);

        // then:
        assertFalse(quickFix.isPresent());
    }
}