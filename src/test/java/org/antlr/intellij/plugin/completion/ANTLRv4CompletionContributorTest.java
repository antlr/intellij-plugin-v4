package org.antlr.intellij.plugin.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.antlr.intellij.plugin.ANTLRv4FileType;
import org.antlr.intellij.plugin.TestUtils;

import java.util.Arrays;
import java.util.List;

public class ANTLRv4CompletionContributorTest extends BasePlatformTestCase {
    public void test_import_completion() {
        myFixture.copyDirectoryToProject("/import_parser_rules", "");
        myFixture.configureByText(
                ANTLRv4FileType.INSTANCE,
                """
                        parser grammar ANTLRv4Parser;
                        import <caret>                
                        """);
        List<String> completions = Arrays.stream(myFixture.completeBasic()).map(LookupElement::getLookupString).toList();
        assertNotNull(completions);
        assertSize(3, completions);
        assertTrue(completions.contains("Grammar"));
        assertTrue(completions.contains("GrammarExpr"));
        assertTrue(completions.contains("CommonLexerRules"));

    }

    public void test_import_completion_is_case_insensitive_lower_case() {
        myFixture.copyDirectoryToProject("/import_parser_rules", "");
        myFixture.configureByText(
                ANTLRv4FileType.INSTANCE,
                """
                        parser grammar ANTLRv4Parser;
                        import g<caret>               
                        """);
        List<String> completions = Arrays.stream(myFixture.completeBasic()).map(LookupElement::getLookupString).toList();
        assertNotNull(completions);
        assertSize(2, completions);
        assertTrue(completions.contains("Grammar"));
        assertTrue(completions.contains("GrammarExpr"));
    }

    public void test_import_completion_is_case_insensitive_upper_case() {
        myFixture.copyDirectoryToProject("/import_parser_rules", "");
        myFixture.configureByText(
                ANTLRv4FileType.INSTANCE,
                """
                        parser grammar ANTLRv4Parser;
                        import G<caret>               
                        """);
        List<String> completions = Arrays.stream(myFixture.completeBasic()).map(LookupElement::getLookupString).toList();
        assertNotNull(completions);
        assertSize(2, completions);
        assertTrue(completions.contains("Grammar"));
        assertTrue(completions.contains("GrammarExpr"));
    }

    protected String getTestDataPath() {
        return "src/test/resources/completion";
    }

    @Override
    protected void tearDown() throws Exception {
        TestUtils.tearDownIgnoringObjectNotDisposedException(() -> super.tearDown());
    }
}
