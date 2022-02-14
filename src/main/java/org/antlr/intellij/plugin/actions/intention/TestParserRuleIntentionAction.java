package org.antlr.intellij.plugin.actions.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.icons.AllIcons.Actions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.actions.MyActionUtils;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nls.Capitalization;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TestParserRuleIntentionAction extends PsiElementBaseIntentionAction implements
        IntentionAction, Iconable {
    @NotNull
    @Override
    public String getText() {
        return "Test parser rule";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
        controller.getPreviewWindow().show(null);

        PsiFile file = PsiTreeUtil.getParentOfType(element, PsiFile.class);
        assert file != null;

        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        documentManager.saveDocument(editor.getDocument());

        ParserRuleRefNode node = MyActionUtils.getParserRuleRefNode(element);
        assert node != null;

        controller.setStartRuleNameEvent(file.getVirtualFile(), node.getText());
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor,
            @NotNull PsiElement element) {
        PsiFile file = PsiTreeUtil.getParentOfType(element, PsiFile.class);
        if (file == null) {
            return false;
        }

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null || !virtualFile.getName().endsWith(".g4")) {
            return false;
        }

        ParserRuleRefNode node = MyActionUtils.getParserRuleRefNode(element);
        return node != null;
    }

    @Nls(capitalization = Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return "ANTLRv4";
    }

    @Override
    public Icon getIcon(int flags) {
        return Actions.Execute;
    }
}
