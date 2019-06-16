package org.antlr.intellij.plugin;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.validation.GrammarFileInspector;
import org.antlr.intellij.plugin.validation.GrammarIssue;
import org.antlr.intellij.plugin.validation.GrammarIssuesCollector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ANTRLv4Inspection extends LocalInspectionTool {

    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(PsiFile file) {
                List<GrammarIssue> grammarIssues = GrammarIssuesCollector.collectGrammarIssues(file);
                GrammarFileInspector grammarFileInspector = new GrammarFileInspector(holder, grammarIssues);
                for (PsiElement child : file.getChildren()) {
                    child.accept(grammarFileInspector);
                }
            }
        };
    }

}
