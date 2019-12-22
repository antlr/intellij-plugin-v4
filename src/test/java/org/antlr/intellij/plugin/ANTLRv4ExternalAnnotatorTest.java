package org.antlr.intellij.plugin;

import com.google.common.collect.Iterables;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.validation.CreateRuleFix;
import org.antlr.intellij.plugin.validation.AddTokenDefinitionFix;
import org.antlr.intellij.plugin.validation.GrammarIssue;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ErrorType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ANTLRv4ExternalAnnotatorTest {

    @Test
    public void shouldRegisterTokenDefinitionQuickFix() {
        // given:
        Annotation annotation = new Annotation(0,0, HighlightSeverity.WARNING, "msg", "tooltip");

        // when:
        ANTLRv4ExternalAnnotator.registerFixForAnnotation(annotation, new GrammarIssue(new ANTLRMessage(ErrorType.IMPLICIT_TOKEN_DEFINITION)), null);

        // then:
        Annotation.QuickFixInfo quickFix = Iterables.getOnlyElement(annotation.getQuickFixes());
        Assert.assertTrue(quickFix.quickFix instanceof AddTokenDefinitionFix);
    }

    @Test
    public void shouldRegisterCreateRuleQuickFix() {
        // given:
        Annotation annotation = new Annotation(0,0, HighlightSeverity.WARNING, "msg", "tooltip");
        PsiFile file = Mockito.mock(PsiFile.class);
        Mockito.when(file.getText()).thenReturn("sample text");

        // when:
        ANTLRv4ExternalAnnotator.registerFixForAnnotation(annotation, new GrammarIssue(new ANTLRMessage(ErrorType.UNDEFINED_RULE_REF)), file);

        // then:
        Annotation.QuickFixInfo quickFix = Iterables.getOnlyElement(annotation.getQuickFixes());
        Assert.assertTrue(quickFix.quickFix instanceof CreateRuleFix);
    }
}