package org.antlr.intellij.plugin.editor;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.antlr.intellij.plugin.ANTLRv4FileType;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.TestUtils;
import org.junit.Test;

public class Issue569Test extends BasePlatformTestCase {

    @Test
    public void test_shouldReassignExtensionType() {

        // Reassign extension temporarily
        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                FileTypeManager.getInstance().associateExtension(FileTypes.PLAIN_TEXT, "g4"));

        // Test File
        VirtualFile file = myFixture.configureByFile("FooParser.g4").getVirtualFile();
        assertEquals(file.getFileType(), FileTypes.PLAIN_TEXT);

        // Create controller and initialize it
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(getProject());
        assertNotNull(controller);
        controller.initComponent();

        // Check if file type is reassigned
        assertEquals(file.getFileType(), ANTLRv4FileType.INSTANCE);

    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/references";
    }

    @Override
    protected void tearDown() throws Exception {
        TestUtils.tearDownIgnoringObjectNotDisposedException(() -> {
            EditorFactory.getInstance().releaseEditor(myFixture.getEditor());
            super.tearDown();
        });
    }

}