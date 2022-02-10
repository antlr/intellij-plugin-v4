package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ANTLRActionGroup extends DefaultActionGroup {

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = MyActionUtils.getGrammarFileFromEvent(e);
        boolean isGrammarFile = file != null;
        e.getPresentation().setEnabled(isGrammarFile);
        e.getPresentation().setVisible(isGrammarFile);
    }
}
