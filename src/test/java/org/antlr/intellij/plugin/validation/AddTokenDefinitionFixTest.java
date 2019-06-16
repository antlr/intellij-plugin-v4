package org.antlr.intellij.plugin.validation;

import org.junit.Assert;
import org.junit.Test;

public class AddTokenDefinitionFixTest {

    @Test
    public void shouldCreateTokenDefinitionText() {
        Assert.assertEquals("MYTOKEN : M Y T O K E N;", AddTokenDefinitionFix.buildTokenDefinitionText("MYTOKEN"));
    }

    @Test
    public void shouldCreateTokenDefinitionTextWhenHavingLowercase() {
        Assert.assertEquals("MYToken : M Y T O K E N;", AddTokenDefinitionFix.buildTokenDefinitionText("MYToken"));
    }
}