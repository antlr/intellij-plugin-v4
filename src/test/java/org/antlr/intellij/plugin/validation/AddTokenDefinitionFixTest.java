package org.antlr.intellij.plugin.validation;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class AddTokenDefinitionFixTest {

    private static final int TOKEN_EXPR_START_OFFSET = 10;
    private static final int ELEMENT_LENGTH = 20;

    @Test
    public void shouldCreateTokenDefinitionText() {
        Assert.assertEquals("M Y T O K E N", AddTokenDefinitionFix.buildTokenDefinitionExpressionText("MYTOKEN"));
    }

    @Test
    public void shouldCreateTokenDefinitionTextWhenHavingLowercase() {
        Assert.assertEquals("M Y T O K E N", AddTokenDefinitionFix.buildTokenDefinitionExpressionText("MYToken"));
    }

    @Test
    public void shouldCreateTokenDefinitionTextWhenHavingNonLetterLiterals() {
        Assert.assertEquals("M Y '_' T O K E N", AddTokenDefinitionFix.buildTokenDefinitionExpressionText("MY_TOKEN"));
    }

    @Test
    public void shouldGetRangeForReplacement() {
        // given:
        PsiElement element = Mockito.mock(PsiElement.class);
        Mockito.when(element.getTextLength()).thenReturn(ELEMENT_LENGTH);

        // when:
        TextRange range = AddTokenDefinitionFix.getRange(element, TOKEN_EXPR_START_OFFSET);

        // then:
        Assert.assertEquals(TOKEN_EXPR_START_OFFSET, range.getStartOffset());
        Assert.assertEquals(ELEMENT_LENGTH - 1, range.getEndOffset());
    }
}