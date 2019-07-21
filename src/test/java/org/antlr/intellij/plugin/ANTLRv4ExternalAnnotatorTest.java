package org.antlr.intellij.plugin;

import com.google.common.collect.Iterables;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.HighlightSeverity;
import org.antlr.intellij.plugin.validation.AddTokenDefinitionFix;
import org.antlr.intellij.plugin.validation.GrammarIssue;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ErrorType;
import org.junit.Assert;
import org.junit.Test;

public class ANTLRv4ExternalAnnotatorTest {

    @Test
    public void shouldRegisterTokenDefinitionQuickFix() {
        // given:
        Annotation annotation = new Annotation(0,0, HighlightSeverity.WARNING, "msg", "tooltip");

        // when:
        ANTLRv4ExternalAnnotator.registerFixForAnnotation(annotation, new GrammarIssue(new ANTLRMessage(ErrorType.IMPLICIT_TOKEN_DEFINITION)));

        // then:
        Annotation.QuickFixInfo quickFix = Iterables.getOnlyElement(annotation.getQuickFixes());
        Assert.assertTrue(quickFix.quickFix instanceof AddTokenDefinitionFix);
    }
}