package org.antlr.intellij.plugin.refactor;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.antlr.intellij.plugin.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class GrammarRenameTest extends BasePlatformTestCase {

    @Test
    public void test_validateInput() {
        GrammarRenameInputValidator validator = new GrammarRenameInputValidator();
        assertTrue(validator.isInputValid("foo", null, null));
        assertFalse(validator.isInputValid("f oo", null, null));
    }

    @Test
    public void test_renameFromLexerGrammar() {
        testFilePaths("RenameLexer.g4", "RenameLexer.tokens", "RenameParser.g4");
    }

    @Test
    public void test_renameGrammarFiles() {
        testFilePaths("RenameParser.g4", "RenameLexer.tokens", "RenameLexer.g4");
    }

    @Test
    public void test_renameFromFile() {
        myFixture.configureByFiles("RenameLexer.g4", "RenameLexer.tokens", "RenameParser.g4");

        // Rename the file
        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                myFixture.renameElement(myFixture.getFile(), "RenameLexer2"));

        assertNotNull(myFixture.findFileInTempDir("RenameLexer2.tokens"));
        myFixture.checkResultByFile("RenameLexer2.g4", "RenameLexer-after.g4", false);
        myFixture.checkResultByFile("RenameParser.g4", "RenameParser-after.g4", false);

    }

    private void testFilePaths(String @NotNull ... filePaths) {
        myFixture.configureByFiles(filePaths);
        myFixture.renameElementAtCaret("RenameLexer2");
        assertNotNull(myFixture.findFileInTempDir("RenameLexer2.tokens"));
        assertNotNull(myFixture.findFileInTempDir("RenameLexer2.g4"));
        myFixture.checkResultByFile("RenameParser.g4", "RenameParser-after.g4", false);
        myFixture.checkResultByFile("RenameLexer2.g4", "RenameLexer-after.g4", false);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/rename";
    }

    @Override
    protected void tearDown() throws Exception {
        TestUtils.tearDownIgnoringObjectNotDisposedException(() -> super.tearDown());
    }

}