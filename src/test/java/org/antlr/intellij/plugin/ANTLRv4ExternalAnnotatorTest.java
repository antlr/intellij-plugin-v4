package org.antlr.intellij.plugin;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.antlr.intellij.plugin.validation.AddTokenDefinitionFix;
import org.antlr.intellij.plugin.validation.CreateRuleFix;
import org.junit.Assert;

import java.util.List;
import java.util.Objects;

public class ANTLRv4ExternalAnnotatorTest extends BasePlatformTestCase {

    @Override
    protected void tearDown() throws Exception {
        TestUtils.tearDownIgnoringObjectNotDisposedException(super::tearDown);
    }

    public void testShouldRegisterTokenDefinitionQuickFix() {
        // given:
        myFixture.configureByText("test.g4", "grammar test; rule: TOKEN;");

        // when:
        List<HighlightInfo> result = myFixture.doHighlighting();

        // then:
        var quickFix = result.stream()
            .map(el -> el.findRegisteredQuickFix((action, range) -> action))
            .filter(Objects::nonNull)
            .findFirst().orElseThrow();

        Assert.assertTrue(quickFix.getAction() instanceof AddTokenDefinitionFix);
    }

    public void testShouldRegisterCreateRuleQuickFix() {
        // given:
        myFixture.configureByText("test.g4", "grammar test; rule: undefined_rule;");

        // when:
        List<HighlightInfo> result = myFixture.doHighlighting();

        // then:
        var quickFix = result.stream()
            .map(el -> el.findRegisteredQuickFix((action, range) -> action))
            .filter(Objects::nonNull)
            .findFirst().orElseThrow();

        Assert.assertTrue(quickFix.getAction() instanceof CreateRuleFix);
    }
}