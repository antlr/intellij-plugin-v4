package org.antlr.intellij.plugin.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.TestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

public class Issue540Test extends BasePlatformTestCase {

    private final File LEXER_FILE = new File(getTestDataPath()+"TestLexer.g4");
    private final File PARSER_FILE = new File(getTestDataPath()+"TestParser.g4");
    private final File TOKENS_FILE = new File(getTestDataGenPath()+"TestLexer.tokens");

    @Test
    public void test_shouldOnlyCreateTokensWhenModified() {

        // Create files and controller
        VirtualFile lexerFile = createAndOpenFile(LEXER_FILE, "lexer grammar TestLexer;\nTOKEN1: 'TOKEN1';");
        VirtualFile parserFile = createAndOpenFile(PARSER_FILE, "parser grammar TestParser;\noptions {tokenVocab=TestLexer;}\nstartRule: TOKEN1;");
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(getProject());

        // No tokens should be created yet
        assertFalse(TOKENS_FILE.exists());

        // Add one token to file, switch to parser file and check that tokens were created
        switchToFile(lexerFile);
        addLineToCurrentFile("TOKEN2: 'TOKEN2';", lexerFile, controller);
        switchToFile(parserFile);
        assertTrue(TOKENS_FILE.exists());
        long lastModified1 = TOKENS_FILE.lastModified();

        try {
            Thread.sleep(100); // Wait for 100ms to ensure that the file update
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Switch to lexer file, add token, and check if tokens file was updated
        switchToFile(lexerFile);
        addLineToCurrentFile("TOKEN3: 'TOKEN3';", lexerFile, controller);
        switchToFile(parserFile);
        long lastModified2 = new File(TOKENS_FILE.getAbsolutePath()).lastModified();
        assertTrue(lastModified2 > lastModified1);

        // Switch back and forth again, and make sure tokens are not recreated
        switchToFile(lexerFile);
        switchToFile(parserFile);
        assertEquals(lastModified2, TOKENS_FILE.lastModified());

    }

    private VirtualFile createAndOpenFile(File file, String contents) {
        String absPath = file.getAbsolutePath();
        VfsTestUtil.overwriteTestData(absPath, contents);
        VirtualFile virtualFile = WriteAction.computeAndWait(() ->
                LocalFileSystem.getInstance().refreshAndFindFileByPath(absPath));
        myFixture.openFileInEditor(virtualFile);
        return virtualFile;
    }

    private String getTestDataGenPath() {
        return getTestDataPath() + "gen/";
    }

    private void addLineToCurrentFile(String line, VirtualFile file, ANTLRv4PluginController controller) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            Document doc = myFixture.getEditor().getDocument();
            doc.setText(doc.getText() + "\n" + line);
            PsiDocumentManager.getInstance(getProject()).commitDocument(doc);
            FileDocumentManager.getInstance().saveDocument(doc);
            if (controller != null) controller.grammarFileSavedEvent(file);
        });
    }

    private void switchToFile(VirtualFile vf) {
        myFixture.openFileInEditor(vf);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/";
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.forceDeleteOnExit(LEXER_FILE);
        FileUtils.forceDeleteOnExit(PARSER_FILE);
        FileUtils.forceDeleteOnExit(new File(getTestDataGenPath()));
        EditorFactory.getInstance().releaseEditor(myFixture.getEditor());
        TestUtils.tearDownIgnoringObjectNotDisposedException(super::tearDown);
    }

}